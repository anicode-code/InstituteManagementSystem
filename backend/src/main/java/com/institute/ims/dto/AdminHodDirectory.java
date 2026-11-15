package com.institute.ims.dto;

public record AdminHodDirectory(
        AdminHodDirectoryItem currentHod,
        Summary summary) {

    public record Summary(
            int totalFacultyMembers,
            int totalStudents,
            int totalSubjects) {
    }
}
