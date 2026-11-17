package com.institute.ims.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "roll_number", nullable = false, unique = true)
    private String rollNumber;

    @Column(name = "registration_number", unique = true, length = 50)
    private String registrationNumber;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private String email;

    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String gender;

    @Column(name = "residential_address", length = 1000)
    private String residentialAddress;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Column(name = "employment_status")
    private String employmentStatus;

    private String designation;

    @Column(name = "administrative_role")
    private String administrativeRole;

    private String qualification;

    private String specialization;

    @Column(name = "years_of_experience")
    private Double yearsOfExperience;

    @Column(name = "employment_type")
    private String employmentType;

    @JsonIgnore
    @Column(name = "profile_picture", columnDefinition = "bytea")
    private byte[] profilePicture;

    @JsonIgnore
    @Column(name = "profile_picture_content_type", length = 32)
    private String profilePictureContentType;

    @Column(name = "profile_picture_version")
    private Long profilePictureVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status = VerificationStatus.PENDING;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Primary department (backward-compatible, used by students)
    @ManyToOne
    @JoinColumn(name = "department_id")
    @JsonIgnoreProperties({"hod"})
    private Department department;

    // Multiple departments (used by faculty)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_departments",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "department_id")
    )
    @JsonIgnoreProperties({"hod"})
    private Set<Department> departments = new HashSet<>();

    // Student academic fields
    @Column(name = "academic_year")
    private Integer year;

    @Column(name = "semester")
    private Integer semester;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "student_semester_sgpas", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "semester_number")
    @Column(name = "sgpa", nullable = false)
    private Map<Integer, Double> semesterSgpas = new LinkedHashMap<>();

    // Optional subjects selected by a student
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "student_selected_subjects",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    @JsonIgnoreProperties({"faculty", "department"})
    private Set<Subject> selectedSubjects = new HashSet<>();

    public User() {
    }

    public User(String name, String rollNumber, String password,
                String email, Role role, Department department) {
        this.name = name;
        this.rollNumber = rollNumber;
        this.password = password;
        this.email = email;
        this.role = role;
        this.department = department;
        this.status = VerificationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public User(String name, String rollNumber, String password,
                String email, Role role, Department department, VerificationStatus status) {
        this.name = name;
        this.rollNumber = rollNumber;
        this.password = password;
        this.email = email;
        this.role = role;
        this.department = department;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public String getResidentialAddress() {
        return residentialAddress;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public String getDesignation() {
        return designation;
    }

    public String getAdministrativeRole() {
        return administrativeRole;
    }

    public String getQualification() {
        return qualification;
    }

    public String getSpecialization() {
        return specialization;
    }

    public Double getYearsOfExperience() {
        return yearsOfExperience;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    @JsonIgnore
    public byte[] getProfilePicture() {
        return profilePicture;
    }

    @JsonIgnore
    public String getProfilePictureContentType() {
        return profilePictureContentType;
    }

    @JsonIgnore
    public Long getProfilePictureVersion() {
        return profilePictureVersion;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Transient
    public String getProfileImageUrl() {
        if (profilePicture == null || id == null) {
            return null;
        }
        long version = profilePictureVersion == null ? 0 : profilePictureVersion;
        return "/api/users/" + id + "/profile-picture?v=" + version;
    }

    public Role getRole() {
        return role;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Department getDepartment() {
        return department;
    }

    public Set<Department> getDepartments() {
        return departments;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getSemester() {
        return semester;
    }

    public Map<Integer, Double> getSemesterSgpas() {
        return semesterSgpas;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Map<Integer, Double> getYearlyGpas() {
        Map<Integer, Double> yearlyGpas = new LinkedHashMap<>();
        for (int academicYear = 1; academicYear <= 4; academicYear++) {
            Double firstSemesterSgpa = semesterSgpas.get(academicYear * 2 - 1);
            Double secondSemesterSgpa = semesterSgpas.get(academicYear * 2);
            if (firstSemesterSgpa != null && secondSemesterSgpa != null) {
                double average = BigDecimal.valueOf(firstSemesterSgpa)
                        .add(BigDecimal.valueOf(secondSemesterSgpa))
                        .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                        .doubleValue();
                yearlyGpas.put(academicYear, average);
            }
        }
        return yearlyGpas;
    }

    public Set<Subject> getSelectedSubjects() {
        return selectedSubjects;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setResidentialAddress(String residentialAddress) {
        this.residentialAddress = residentialAddress;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public void setAdministrativeRole(String administrativeRole) {
        this.administrativeRole = administrativeRole;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public void setYearsOfExperience(Double yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public void setProfilePicture(byte[] profilePicture) {
        this.profilePicture = profilePicture;
    }

    public void setProfilePictureContentType(String profilePictureContentType) {
        this.profilePictureContentType = profilePictureContentType;
    }

    public void setProfilePictureVersion(Long profilePictureVersion) {
        this.profilePictureVersion = profilePictureVersion;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setStatus(VerificationStatus status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public void setDepartments(Set<Department> departments) {
        this.departments = departments;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public void setSemesterSgpas(Map<Integer, Double> semesterSgpas) {
        this.semesterSgpas.clear();
        if (semesterSgpas != null) {
            this.semesterSgpas.putAll(semesterSgpas);
        }
    }

    public void setSelectedSubjects(Set<Subject> selectedSubjects) {
        this.selectedSubjects = selectedSubjects;
    }
}
