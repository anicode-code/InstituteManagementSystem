package com.institute.ims.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "faculty_profile_options",
        uniqueConstraints = @UniqueConstraint(columnNames = {"option_category", "option_value"}))
public class FacultyProfileOption {

    public enum Category {
        GENDER,
        ACADEMIC_DESIGNATION,
        ADMINISTRATIVE_ROLE,
        EMPLOYMENT_STATUS,
        EMPLOYMENT_TYPE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_category", nullable = false)
    private Category category;

    @Column(name = "option_value", nullable = false)
    private String value;

    public FacultyProfileOption() {
    }

    public FacultyProfileOption(Category category, String value) {
        this.category = category;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public String getValue() {
        return value;
    }
}
