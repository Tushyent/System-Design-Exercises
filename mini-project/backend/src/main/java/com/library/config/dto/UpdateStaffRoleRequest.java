package com.library.config.dto;

import com.library.members.model.Role;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStaffRoleRequest {
    @NotNull(message = "Role is required (LIBRARIAN, ADMIN, MEMBER)")
    private Role role;
}
