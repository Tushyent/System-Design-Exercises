package com.library.auth.controller;

import com.library.auth.dto.AuthResponse;
import com.library.auth.dto.LoginRequest;
import com.library.auth.dto.RegisterRequest;
import com.library.common.dto.ApiResponse;
import com.library.common.exception.ConflictException;
import com.library.common.exception.RateLimitExceededException;
import com.library.idgenerator.SnowflakeIdGenerator;
import com.library.members.model.Member;
import com.library.members.model.Role;
import com.library.members.repository.MemberRepository;
import com.library.ratelimit.RateLimiterDecision;
import com.library.ratelimit.TokenBucketRateLimiter;
import com.library.security.JwtTokenProvider;
import com.library.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final TokenBucketRateLimiter rateLimiter;

    /** Strict brute-force policy (see ratelimit.auth.* in application.properties). */
    @Value("${ratelimit.auth.capacity:10}")
    private int authBurstCapacity;

    @Value("${ratelimit.auth.refill-per-second:2}")
    private int authRefillPerSecond;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = httpRequest.getRemoteAddr();
        RateLimiterDecision decision = rateLimiter.tryAcquire("auth:" + clientIp, authBurstCapacity, authRefillPerSecond);
        if (!decision.isAllowed()) {
            throw new RateLimitExceededException(
                    "Rate limit exceeded for authentication requests", decision.getRetryAfterSeconds());
        }

        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already registered: " + request.getEmail());
        }

        String snowflakeId = snowflakeIdGenerator.nextIdString();
        String memberId = "MEM-" + snowflakeId.substring(Math.max(0, snowflakeId.length() - 6));

        Role assignedRole = request.getRole() != null ? request.getRole() : Role.MEMBER;

        Member member = Member.builder()
                .id(snowflakeId)
                .memberId(memberId)
                .name(request.getName())
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .status("ACTIVE")
                .maxBorrowLimit(assignedRole == Role.MEMBER ? 5 : 20)
                .build();

        memberRepository.save(member);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String jwt = tokenProvider.generateToken(authentication);

        AuthResponse authResponse = AuthResponse.builder()
                .token(jwt)
                .id(member.getId())
                .memberId(member.getMemberId())
                .name(member.getName())
                .email(member.getEmail())
                .role(member.getRole().name())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(authResponse, "Member registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = httpRequest.getRemoteAddr();
        RateLimiterDecision decision = rateLimiter.tryAcquire("auth:" + clientIp, authBurstCapacity, authRefillPerSecond);
        if (!decision.isAllowed()) {
            throw new RateLimitExceededException(
                    "Too many login attempts. Please try again later.", decision.getRetryAfterSeconds());
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        AuthResponse authResponse = AuthResponse.builder()
                .token(jwt)
                .id(userPrincipal.getId())
                .memberId(userPrincipal.getMemberId())
                .name(userPrincipal.getUsername())
                .email(userPrincipal.getEmail())
                .role(userPrincipal.getRole())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(authResponse, "Logged in successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Member member = memberRepository.findById(principal.getId()).orElse(null);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        AuthResponse authResponse = AuthResponse.builder()
                .id(member.getId())
                .memberId(member.getMemberId())
                .name(member.getName())
                .email(member.getEmail())
                .role(member.getRole().name())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(authResponse, "Profile retrieved"));
    }
}
