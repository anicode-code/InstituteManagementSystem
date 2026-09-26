import api from './axios';

export const getAllAttendance = () => api.get('/api/attendance');
export const getAttendanceByStudent = (studentId) =>
  api.get(`/api/attendance/student/${studentId}`);
export const getAttendanceBySubject = (subjectId) =>
  api.get(`/api/attendance/subject/${subjectId}`);
export const getAttendancePercentage = (studentId, subjectId) =>
  api.get('/api/attendance/percentage', { params: { studentId, subjectId } });

// Backend parses all as strings via Map<String, String>
export const createAttendance = (data) =>
  api.post('/api/attendance', {
    studentId: String(data.studentId),
    subjectId: String(data.subjectId),
    date: String(data.date),         // "YYYY-MM-DD"
    status: String(data.status),     // "PRESENT" | "ABSENT"
  });

// Status is a query param
export const updateAttendance = (id, status) =>
  api.put(`/api/attendance/${id}`, null, { params: { status } });
