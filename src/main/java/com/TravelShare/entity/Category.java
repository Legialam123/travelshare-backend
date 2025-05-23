package com.TravelShare.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name="category")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true)
    String name;

    @Column(nullable = false)
    String description;

    @Builder.Default
    Boolean isSystemCategory = false;

    @Column(nullable = true)
    String iconCode;  // Mã icon (Material Icons hoặc Unicode)

    @Column(nullable = true)
    String color;     // Mã màu HEX (ví dụ: #FF5733)

    @ManyToOne
    @JoinColumn(name = "group_id")
    Group group;

    @ManyToOne
    @JoinColumn(name = "created_by_id")
    User createdBy;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    @Builder.Default
    Set<Expense> expenses = new HashSet<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    @Builder.Default
    Set<Group> groups = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type; // EXPENSE, GROUP hoặc SYSTEM

    public enum CategoryType {
        EXPENSE, GROUP, BOTH
    }
}
