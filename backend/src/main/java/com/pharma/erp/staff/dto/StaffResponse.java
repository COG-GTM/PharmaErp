package com.pharma.erp.staff.dto;

import com.pharma.erp.staff.entity.Staff;
import com.pharma.erp.staff.entity.StaffType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record StaffResponse(
        Integer id,
        String firstName,
        String middleName,
        String lastName,
        String fullName,
        LocalDate dateOfBirth,
        LocalDate joinDate,
        String experience,
        BigDecimal salary,
        String homeTown,
        String homeTownAddress,
        String jobAddress,
        String leavingAddress,
        String mobileNumber,
        String email,
        String lastCompany,
        String resumePath,
        String feedback,
        String suggestion,
        LoginTypeResponse loginType,
        String username,
        Integer storeId,
        StaffType staffType,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static StaffResponse from(Staff s) {
        String fullName = Stream.of(s.getFirstName(), s.getMiddleName(), s.getLastName())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
        return new StaffResponse(
                s.getId(), s.getFirstName(), s.getMiddleName(), s.getLastName(), fullName,
                s.getDateOfBirth(), s.getJoinDate(), s.getExperience(), s.getSalary(), s.getHomeTown(),
                s.getHomeTownAddress(), s.getJobAddress(), s.getLeavingAddress(), s.getMobileNumber(),
                s.getEmail(), s.getLastCompany(), s.getResumePath(), s.getFeedback(), s.getSuggestion(),
                s.getLoginType() == null ? null : LoginTypeResponse.from(s.getLoginType()),
                s.getUsername(), s.getStoreId(), s.getStaffType(), s.isActive(), s.getCreatedAt(),
                s.getUpdatedAt());
    }
}
