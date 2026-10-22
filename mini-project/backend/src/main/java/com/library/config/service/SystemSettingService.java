package com.library.config.service;

import com.library.audit.service.AuditLogService;
import com.library.common.exception.ConflictException;
import com.library.common.exception.ResourceNotFoundException;
import com.library.config.dto.CreateStaffRequest;
import com.library.config.dto.SystemConfigDTO;
import com.library.config.model.SystemSetting;
import com.library.config.repository.SystemSettingRepository;
import com.library.idgenerator.SnowflakeIdGenerator;
import com.library.members.model.Member;
import com.library.members.model.Role;
import com.library.members.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository settingRepository;
    private final MemberRepository memberRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public SystemConfigDTO getConfig() {
        int loanDays = getInt("circulation.loan.days", 14);
        int borrowLimit = getInt("circulation.borrow.limit", 5);
        double fineRate = getDouble("circulation.fine.rate.daily", 1.50);
        int holdHours = getInt("circulation.reservation.hold.hours", 48);
        boolean maintenance = getBoolean("system.maintenance.mode", false);
        int renewalCount = getInt("circulation.max.renewals", 2);

        return SystemConfigDTO.builder()
                .loanPeriodDays(loanDays)
                .defaultBorrowLimit(borrowLimit)
                .dailyFineRate(fineRate)
                .holdExpiryHours(holdHours)
                .maintenanceMode(maintenance)
                .maxRenewalCount(renewalCount)
                .build();
    }

    public SystemConfigDTO updateConfig(SystemConfigDTO dto, String staffEmail) {
        saveSetting("circulation.loan.days", String.valueOf(dto.getLoanPeriodDays()), "Default loan period in days", staffEmail);
        saveSetting("circulation.borrow.limit", String.valueOf(dto.getDefaultBorrowLimit()), "Default member borrowing limit", staffEmail);
        saveSetting("circulation.fine.rate.daily", String.valueOf(dto.getDailyFineRate()), "Daily overdue fine rate (INR)", staffEmail);
        saveSetting("circulation.reservation.hold.hours", String.valueOf(dto.getHoldExpiryHours()), "Hold pickup shelf expiration in hours", staffEmail);
        saveSetting("system.maintenance.mode", String.valueOf(dto.isMaintenanceMode()), "System maintenance mode lock", staffEmail);
        saveSetting("circulation.max.renewals", String.valueOf(dto.getMaxRenewalCount()), "Maximum allowed loan renewals", staffEmail);

        auditLogService.record(staffEmail, "ADMIN", "CONFIG_UPDATE", "CONFIG", "SYSTEM",
                String.format("Updated system policies: LoanDays=%d, FineRate=INR %.2f, HoldHours=%d, Maintenance=%s",
                        dto.getLoanPeriodDays(), dto.getDailyFineRate(), dto.getHoldExpiryHours(), dto.isMaintenanceMode()));

        return getConfig();
    }

    public List<Member> listStaffAccounts() {
        List<Member> librarians = memberRepository.findByRole(Role.LIBRARIAN);
        List<Member> admins = memberRepository.findByRole(Role.ADMIN);
        librarians.addAll(admins);
        return librarians;
    }

    public Member createStaffAccount(CreateStaffRequest request, String adminEmail) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already in use: " + request.getEmail());
        }

        String rawId = idGenerator.nextIdString();
        String memberCode = "STAFF-" + rawId.substring(Math.max(0, rawId.length() - 5));

        Member staff = Member.builder()
                .id(rawId)
                .memberId(memberCode)
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : Role.LIBRARIAN)
                .status("ACTIVE")
                .department(request.getDepartment() != null ? request.getDepartment() : "Library Administration")
                .phone(request.getPhone())
                .maxBorrowLimit(50)
                .createdAt(Instant.now())
                .build();

        Member saved = memberRepository.save(staff);
        auditLogService.record(adminEmail, "ADMIN", "STAFF_CREATE", "STAFF", saved.getEmail(),
                String.format("Created %s account for %s (%s)", saved.getRole(), saved.getName(), saved.getEmail()));
        return saved;
    }

    public Member updateStaffRole(String staffId, Role newRole, String adminEmail) {
        Member staff = memberRepository.findById(staffId)
                .or(() -> memberRepository.findByEmail(staffId))
                .orElseThrow(() -> new ResourceNotFoundException("Staff account not found: " + staffId));

        if (staff.getEmail().equalsIgnoreCase(adminEmail) && newRole != Role.ADMIN) {
            throw new ConflictException("Administrators cannot demote their own account!");
        }

        Role oldRole = staff.getRole();
        staff.setRole(newRole);
        Member saved = memberRepository.save(staff);

        auditLogService.record(adminEmail, "ADMIN", "ROLE_CHANGE", "STAFF", saved.getEmail(),
                String.format("Changed role for %s: %s -> %s", saved.getEmail(), oldRole, newRole));
        return saved;
    }

    public Member toggleStaffStatus(String staffId, String adminEmail) {
        Member staff = memberRepository.findById(staffId)
                .or(() -> memberRepository.findByEmail(staffId))
                .orElseThrow(() -> new ResourceNotFoundException("Staff account not found: " + staffId));

        if (staff.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new ConflictException("Administrators cannot deactivate their own account!");
        }

        String newStatus = "ACTIVE".equalsIgnoreCase(staff.getStatus()) ? "INACTIVE" : "ACTIVE";
        staff.setStatus(newStatus);
        Member saved = memberRepository.save(staff);

        auditLogService.record(adminEmail, "ADMIN", "STAFF_STATUS_TOGGLE", "STAFF", saved.getEmail(),
                String.format("Staff status changed to %s for %s", newStatus, saved.getEmail()));
        return saved;
    }

    private void saveSetting(String key, String value, String description, String user) {
        SystemSetting setting = settingRepository.findByKey(key)
                .orElse(SystemSetting.builder().key(key).build());
        setting.setValue(value);
        setting.setDescription(description);
        setting.setUpdatedBy(user);
        setting.setUpdatedAt(Instant.now());
        settingRepository.save(setting);
    }

    private int getInt(String key, int defaultValue) {
        return settingRepository.findByKey(key)
                .map(s -> {
                    try { return Integer.parseInt(s.getValue()); } catch (Exception e) { return defaultValue; }
                }).orElse(defaultValue);
    }

    private double getDouble(String key, double defaultValue) {
        return settingRepository.findByKey(key)
                .map(s -> {
                    try { return Double.parseDouble(s.getValue()); } catch (Exception e) { return defaultValue; }
                }).orElse(defaultValue);
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        return settingRepository.findByKey(key)
                .map(s -> Boolean.parseBoolean(s.getValue()))
                .orElse(defaultValue);
    }
}
