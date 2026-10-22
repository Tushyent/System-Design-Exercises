package com.library.members.dto;

import com.library.members.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberRequest {
    private String name;
    private String email;
    private String department;
    private String phone;
    private Integer maxBorrowLimit;
    private Role role;
    private String status;
}
