package com.TravelShare.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class RefreshToken {
    @Id
    String id;  // JWT ID

    String username;
    Date expiryTime;
    boolean revoked;

    @Column(length = 1000)
    String fingerprint;  // Device fingerprint hash
}