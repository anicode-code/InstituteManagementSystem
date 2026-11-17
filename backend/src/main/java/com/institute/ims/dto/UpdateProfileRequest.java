package com.institute.ims.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;

import java.time.LocalDate;
import java.util.List;

public class UpdateProfileRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Invalid email")
    private String email;

    private Integer year;
    private Integer semester;
    private List<String> optionalSubjectCodes;
    private String phone;
    private LocalDate dateOfBirth;
    private boolean dateOfBirthProvided;
    private String gender;
    private String residentialAddress;
    private LocalDate joiningDate;
    private boolean joiningDateProvided;
    private String employmentStatus;
    private String designation;
    private String qualification;
    private String specialization;
    @DecimalMin(value = "0.0", message = "Years of experience cannot be negative")
    private Double yearsOfExperience;
    private boolean yearsOfExperienceProvided;
    private String employmentType;

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getSemester() {
        return semester;
    }

    public List<String> getOptionalSubjectCodes() {
        return optionalSubjectCodes;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public boolean isDateOfBirthProvided() {
        return dateOfBirthProvided;
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

    public boolean isJoiningDateProvided() {
        return joiningDateProvided;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public String getDesignation() {
        return designation;
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

    public boolean isYearsOfExperienceProvided() {
        return yearsOfExperienceProvided;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email != null && email.isBlank() ? null : email;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public void setOptionalSubjectCodes(List<String> optionalSubjectCodes) {
        this.optionalSubjectCodes = optionalSubjectCodes;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        this.dateOfBirthProvided = true;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setResidentialAddress(String residentialAddress) {
        this.residentialAddress = residentialAddress;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
        this.joiningDateProvided = true;
    }

    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public void setYearsOfExperience(Double yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
        this.yearsOfExperienceProvided = true;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }
}
