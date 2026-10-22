package com.library.config.controller;

import com.library.common.dto.ApiResponse;
import com.library.config.dto.CreateStaffRequest;
import com.library.config.dto.SystemConfigDTO;
import com.library.config.dto.UpdateStaffRoleRequest;
import com.library.config.service.SystemSettingService;
import com.library.members.model.Member;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminConfigController {

    private final SystemSettingService settingService;

    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<SystemConfigDTO>> getConfig() {
        SystemConfigDTO config = settingService.getConfig();
        return ResponseEntity.ok(ApiResponse.ok(config, "System configuration retrieved"));
    }

    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SystemConfigDTO>> updateConfig(
            @RequestBody SystemConfigDTO dto,
            Authentication authentication) {
        String adminEmail = authentication != null ? authentication.getName() : "admin";
        SystemConfigDTO updated = settingService.updateConfig(dto, adminEmail);
        return ResponseEntity.ok(ApiResponse.ok(updated, "System configuration updated successfully"));
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Member>>> listStaffAccounts() {
        List<Member> staff = settingService.listStaffAccounts();
        return ResponseEntity.ok(ApiResponse.ok(staff, "Staff accounts retrieved"));
    }

    @PostMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Member>> createStaff(
            @Valid @RequestBody CreateStaffRequest request,
            Authentication authentication) {
        String adminEmail = authentication != null ? authentication.getName() : "admin";
        Member created = settingService.createStaffAccount(request, adminEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Staff account created successfully"));
    }

    @PutMapping("/staff/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Member>> updateStaffRole(
            @PathVariable String id,
            @Valid @RequestBody UpdateStaffRoleRequest request,
            Authentication authentication) {
        String adminEmail = authentication != null ? authentication.getName() : "admin";
        Member updated = settingService.updateStaffRole(id, request.getRole(), adminEmail);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Staff role updated to " + updated.getRole()));
    }

    @PostMapping("/staff/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Member>> toggleStaffStatus(
            @PathVariable String id,
            Authentication authentication) {
        String adminEmail = authentication != null ? authentication.getName() : "admin";
        Member updated = settingService.toggleStaffStatus(id, adminEmail);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Staff status toggled to " + updated.getStatus()));
    }
}
