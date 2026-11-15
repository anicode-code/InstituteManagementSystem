package com.institute.ims.dto;

import com.institute.ims.model.AttendanceStatus;
import com.institute.ims.model.VerificationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminHodProfile(
        PersonalInformation personalInformation,
        ProfessionalInformation professionalInformation,
        DepartmentOverview departmentOverview,
        List<FacultyMember> facultyMembers,
        StudentOverview studentOverview,
        List<SubjectDetails> subjects,
        AttendanceOverview attendanceOverview,
        AssignmentOverview assignmentOverview,
        AcademicPerformanceOverview academicPerformanceOverview,
        AdministrativeInformation administrativeInformation) {

    public record PersonalInformation(
            String fullName,
            String profileImageUrl,
            String hodId,
            String email,
            String phone,
            LocalDate dateOfBirth,
            String gender,
            String residentialAddress,
            LocalDate joiningDate,
            LocalDate appointmentDate,
            LocalDateTime accountCreatedAt,
            VerificationStatus accountStatus,
            String employmentStatus) {
    }

    public record ProfessionalInformation(
            String departmentName,
            String departmentCode,
            String designation,
            String highestQualification,
            String specialization,
            Double professionalExperience,
            String employmentType,
            List<String> previousDesignations) {
    }

    public record DepartmentOverview(
            int totalStudents,
            int totalFacultyMembers,
            int totalSubjects,
            int totalActiveAssignments,
            Double departmentAttendanceAverage,
            Double overallAcademicPerformance) {
    }

    public record FacultyMember(
            Long userId,
            String name,
            String facultyId,
            String designation,
            List<String> assignedSubjects,
            List<Integer> assignedYears,
            List<Integer> assignedSemesters,
            VerificationStatus employmentStatus) {
    }

    public record Distribution(int value, int count) {
    }

    public record StudentOverview(
            int totalStudents,
            List<Distribution> yearDistribution,
            List<Distribution> semesterDistribution,
            Double overallAttendance,
            Double academicPerformance,
            List<StudentAttention> studentsRequiringAttention) {
    }

    public record SubjectDetails(
            Long subjectId,
            String name,
            String code,
            Integer year,
            Integer semester,
            int enrolledStudents,
            Double attendancePercentage,
            int classesConducted,
            List<String> assignedFaculty) {
    }

    public record AttendanceOverview(
            Double departmentAttendancePercentage,
            List<SubjectAttendance> subjectStatistics,
            List<FacultyAttendance> facultyStatistics,
            List<StudentAttention> studentsWithLowAttendance,
            List<AttendanceTrend> trends) {
    }

    public record SubjectAttendance(
            Long subjectId,
            String subjectName,
            String subjectCode,
            int attendanceRecords,
            Double attendancePercentage) {
    }

    public record FacultyAttendance(
            Long facultyId,
            String facultyName,
            String employeeId,
            int attendanceRecords,
            Double attendancePercentage) {
    }

    public record AttendanceTrend(
            String month,
            int attendanceRecords,
            Double attendancePercentage) {
    }

    public record StudentAttention(
            Long studentId,
            String name,
            String rollNumber,
            Double attendancePercentage,
            Double averagePerformance,
            List<String> reasons) {
    }

    public record AssignmentOverview(
            int totalAssignments,
            int totalActiveAssignments,
            int overdueAssignments,
            int pendingSubmissions,
            int assignmentsAwaitingGrading,
            List<FacultyAssignmentCount> facultyDistribution,
            List<AssignmentDetails> assignments) {
    }

    public record FacultyAssignmentCount(
            Long facultyId,
            String facultyName,
            String employeeId,
            int assignmentCount) {
    }

    public record AssignmentDetails(
            Long assignmentId,
            String title,
            String subjectName,
            String subjectCode,
            String facultyName,
            LocalDate creationDate,
            LocalDate dueDate,
            int totalSubmissions,
            int pendingSubmissions,
            String gradingStatus,
            boolean active) {
    }

    public record AcademicPerformanceOverview(
            Double departmentAveragePerformance,
            List<PerformanceGroup> yearPerformance,
            List<PerformanceGroup> semesterPerformance,
            List<SubjectPerformance> subjectPerformance,
            String gradeDistribution,
            List<StudentAttention> studentsRequiringAttention) {
    }

    public record PerformanceGroup(int value, Double averageMarks, int assessedRecords) {
    }

    public record SubjectPerformance(
            Long subjectId,
            String subjectName,
            String subjectCode,
            Double averageMarks,
            int assessedRecords) {
    }

    public record AdministrativeInformation(
            List<String> appointmentHistory,
            List<String> responsibilities,
            List<String> documents,
            List<String> certifications) {
    }
}
