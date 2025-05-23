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
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String fileName;

    String originalFileName;

    @Column(nullable = false)
    String contentType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    MediaType mediaType;

    @Column(nullable = false)
    Long fileSize;

    @Column(nullable = false)
    String filePath;

    @Column(nullable = false)
    String fileUrl;

    String description;

    @Column(nullable = false)
    LocalDateTime uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    User uploadedBy;

    // Optional relationships based on where the media is used


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_split_id")
    ExpenseSplit expenseSplit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id")
    Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id")
    Settlement settlement;

    public enum MediaType {
        IMAGE, VIDEO, AUDIO, DOCUMENT, OTHER
    }
}
