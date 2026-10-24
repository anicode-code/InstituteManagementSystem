package com.institute.ims.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hod_id")
    @JsonIgnoreProperties({"password", "departments", "selectedSubjects", "department"})
    private User hod;

    public Department() {
    }

    public Department(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public User getHod() {
        return hod;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setHod(User hod) {
        this.hod = hod;
    }
}
