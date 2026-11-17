package com.institute.ims.dto;

import com.institute.ims.model.AttendanceStatus;
import com.institute.ims.model.VerificationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminStudentProfile(
        PersonalInformation personalInformation,
        AcademicInformation academicInformation,
        AcademicSummary academicSummary,
        List<SubjectPerformance> subjectPerformance,
        List<AttendanceEntry> attendanceDetails,
        List<AssignmentEntry> assignmentHistory,
        List<ExaminationResult> examinationResults,
        List<SemesterGpa> semesterGpas,
        List<YearlyGpa> yearlyGpas) {

    public record PersonalInformation(
            String name,
            String photoUrl,
            String email,
            String phone,
            LocalDate dateOfBirth,
            String address) {
    }

    public record AcademicInformation(
            String rollNumber,
            String registrationNumber,
            String departmentName,
            String departmentCode,
            Integer admissionYear,
            Integer year,
            Integer semester,
            VerificationStatus accountStatus,
            LocalDateTime registeredAt) {
    }

    public record SemesterGpa(Integer semester, Double sgpa) {
    }

    public record YearlyGpa(Integer year, Double ygpa) {
    }

    public record AcademicSummary(
            Double overallAttendance,
            long attendedClasses,
            long missedClasses,
            long totalClasses,
            Double academicPerformance,
            long submittedAssignments,
            long totalAssignments,
            long pendingAssignments) {
    }

    public record SubjectPerformance(
            Long subjectId,
            String subject,
            String code,
            long attendedClasses,
            long missedClasses,
            long totalClasses,
            Double attendancePercentage,
            Double averageMarks,
            String grade) {
    }

    public record AttendanceEntry(
            Long id,
            String subject,
            String code,
            LocalDate date,
            AttendanceStatus status,
            Integer performanceMarks) {
    }

    public record AssignmentEntry(
            Long id,
            String assignment,
            String subject,
            String code,
            LocalDate dueDate,
            String submissionStatus,
            LocalDateTime submittedAt,
            Integer marks,
            Integer maxMarks) {
    }

    public record ExaminationResult(
            Integer semester,
            Double internalMarks,
            String result,
            String grade,
            Double sgpa,
            Double cgpa) {
    }
}
