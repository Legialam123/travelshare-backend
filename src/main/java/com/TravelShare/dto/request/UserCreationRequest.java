package com.TravelShare.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {
        @Size(min = 6, message = "USERNAME_INVALID")
        String username;
        @Size(min = 6, message = "INVALID_PASSWORD")
        String password;
        String email;
        String fullName;
        @Size( min = 10, max = 10, message = "INVALID_PHONE_NUMBER")
        String phoneNumber;
        LocalDate dob;
        LocalDateTime createdAt;
        String role;
}
