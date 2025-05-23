package com.TravelShare.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {
    String oldPassword;
    @Size(min = 6, message = "INVALID_PASSWORD")
    String newPassword;
    String email;
    String fullName;
    @Size(min = 10, message = "INVALID_PHONE_NUMBER")
    String phoneNumber;
    LocalDate dob;
    LocalDateTime updatedAt;
    String role;
}
