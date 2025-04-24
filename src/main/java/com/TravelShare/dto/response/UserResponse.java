package com.TravelShare.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    String id;
    String username;
    String password;
    String email;
    String fullName;
    String phoneNumber;
    LocalDate dob;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String avatarUrl;
    String role;
}
