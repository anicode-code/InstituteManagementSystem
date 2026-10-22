import api from './axios';

export const getAllAttendance = () => api.get('/api/attendance');
export const getAttendanceByStudent = (studentId) =>
  api.get(`/api/attendance/student/${studentId}`);
export const getAttendanceBySubject = (subjectId) =>
  api.get(`/api/attendance/subject/${subjectId}`);
export const getAttendancePercentage = (studentId, subjectId) =>
  api.get('/api/attendance/percentage', { params: { studentId, subjectId } });

export const createAttendance = (data) =>
  api.post('/api/attendance', {
    studentId: String(data.studentId),
    subjectId: String(data.subjectId),
    date: String(data.date), // "YYYY-MM-DD"
    status: String(data.status), // "PRESENT" | "ABSENT"
    performanceMarks: data.performanceMarks !== undefined && data.performanceMarks !== null && data.performanceMarks !== ''
      ? Number(data.performanceMarks)
      : null,
  });

export const updateAttendance = (id, status, performanceMarks) =>
  api.put(`/api/attendance/${id}`, {
    status,
    performanceMarks: performanceMarks !== undefined && performanceMarks !== null && performanceMarks !== ''
      ? Number(performanceMarks)
      : null,
  });

export const recordDayBatch = (subjectId, date, entries) =>
  api.post('/api/attendance/batch-day', {
    subjectId,
    date,
    entries,
  });

export const deleteDayAttendance = (subjectId, date) =>
  api.delete('/api/attendance/day', { params: { subjectId, date } });
