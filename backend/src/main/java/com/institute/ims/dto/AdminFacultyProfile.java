package com.institute.ims.dto;

import com.institute.ims.model.AttendanceStatus;
import com.institute.ims.model.VerificationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminFacultyProfile(
        PersonalInformation personalInformation,
        ProfessionalInformation professionalInformation,
        TeachingOverview teachingOverview,
        List<AssignedSubject> assignedSubjects,
        AttendanceOverview attendanceOverview,
        List<AssignmentSummary> assignments,
        AcademicPerformanceOverview academicPerformanceOverview,
        AdditionalInformation additionalInformation) {

    public record PersonalInformation(
            String fullName,
            String profileImageUrl,
            String facultyId,
            String email,
            String phone,
            LocalDate dateOfBirth,
            String gender,
            String residentialAddress,
            LocalDate joiningDate,
            LocalDateTime accountCreatedAt,
            VerificationStatus accountStatus,
            String employmentStatus) {
    }

    public record ProfessionalInformation(
            String departmentName,
            String departmentCode,
            String designation,
            String administrativeRole,
            String qualification,
            String specialization,
            Double yearsOfExperience,
            String employmentType,
            List<Integer> assignedAcademicYears,
            List<Integer> assignedSemesters) {
    }

    public record TeachingOverview(
            int totalSubjectsAssigned,
            Long totalClassesConducted,
            int totalStudentsTeaching,
            List<Integer> assignedAcademicYears,
            List<Integer> assignedSemesters) {
    }

    public record AssignedSubject(
            Long subjectId,
            String subjectName,
            String subjectCode,
            Integer year,
            Integer semester,
            int enrolledStudents,
            Long classesConducted,
            Double studentAttendancePercentage,
            Double averagePerformance) {
    }

    public record AttendanceOverview(
            Long totalClassesConducted,
            int totalAttendanceRecords,
            Double studentAttendancePercentage,
            List<SubjectAttendance> subjectStatistics,
            List<AttendanceRecord> records) {
    }

    public record SubjectAttendance(
            Long subjectId,
            String subjectName,
            String subjectCode,
            Long classesConducted,
            int enrolledStudents,
            int attendanceRecords,
            Double attendancePercentage) {
    }

    public record AttendanceRecord(
            Long id,
            LocalDate date,
            Long studentId,
            String studentName,
            String studentRollNumber,
            Long subjectId,
            String subjectName,
            String subjectCode,
            AttendanceStatus status,
            Integer performanceMarks) {
    }

    public record AssignmentSummary(
            Long id,
            String title,
            String description,
            String subjectName,
            String subjectCode,
            LocalDate creationDate,
            LocalDate dueDate,
            int totalSubmissions,
            int pendingSubmissions,
            String gradingStatus,
            Integer maxMarks) {
    }

    public record AcademicPerformanceOverview(
            Double averageSubjectPerformance,
            List<SubjectPerformance> subjectPerformance,
            List<StudentRequiringAttention> studentsRequiringAttention,
            String gradeDistribution) {
    }

    public record SubjectPerformance(
            Long subjectId,
            String subjectName,
            String subjectCode,
            Double averageMarks,
            int assessedRecords) {
    }

    public record StudentRequiringAttention(
            Long studentId,
            String studentName,
            String studentRollNumber,
            Double attendancePercentage,
            Double averagePerformance,
            List<String> reasons,
            List<String> subjects) {
    }

    public record AdditionalInformation(List<String> documents) {
    }
}
