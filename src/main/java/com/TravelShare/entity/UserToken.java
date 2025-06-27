package com.TravelShare.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class UserToken {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
    String token;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    TokenType type;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    @Column(nullable = false)
    private boolean used;

    public enum TokenType {
        RESET_PASSWORD,
        VERIFY_EMAIL
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.used = false;
    }
}
