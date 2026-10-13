package com.library.fines.controller;

import com.library.common.dto.ApiResponse;
import com.library.fines.service.FineService;
import com.library.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/fines")
@RequiredArgsConstructor
public class FineController {

    private final FineService fineService;

    @GetMapping("/my-fines")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyFines(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Map<String, Object> summary = fineService.getMemberFineSummary(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(summary, "Fines retrieved"));
    }
}
