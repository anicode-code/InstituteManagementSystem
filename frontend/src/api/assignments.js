import api from './axios';

export const getAllAssignments = () => api.get('/api/assignments');
export const getAssignmentById = (id) => api.get(`/api/assignments/${id}`);
export const getAssignmentsByFaculty = (facultyId) =>
  api.get(`/api/assignments/faculty/${facultyId}`);
export const getAssignmentsBySubject = (subjectId) =>
  api.get(`/api/assignments/subject/${subjectId}`);

// Backend parses all as strings, including dueDate as "YYYY-MM-DD"
export const createAssignment = (data) =>
  api.post('/api/assignments', {
    title: String(data.title),
    description: String(data.description ?? ''),
    dueDate: String(data.dueDate),
    subjectId: String(data.subjectId),
    facultyId: String(data.facultyId),
  });

export const deleteAssignment = (id) => api.delete(`/api/assignments/${id}`);
