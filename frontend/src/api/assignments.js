import api from './axios';

export const getAllAssignments = () => api.get('/api/assignments');
export const getAssignmentById = (id) => api.get(`/api/assignments/${id}`);
export const getAssignmentsByFaculty = (facultyId) =>
  api.get(`/api/assignments/faculty/${facultyId}`);
export const getAssignmentsBySubject = (subjectId) =>
  api.get(`/api/assignments/subject/${subjectId}`);

export const createAssignment = (data) =>
  api.post('/api/assignments', {
    title: String(data.title),
    description: String(data.description ?? ''),
    dueDate: String(data.dueDate),
    maxMarks: data.maxMarks ? Number(data.maxMarks) : 100,
    subjectId: String(data.subjectId),
    facultyId: String(data.facultyId),
  });

export const deleteAssignment = (id) => api.delete(`/api/assignments/${id}`);

// Submissions & Evaluation
export const submitAssignment = (id, submissionText) =>
  api.post(`/api/assignments/${id}/submit`, { submissionText });

export const getMySubmission = (id) =>
  api.get(`/api/assignments/${id}/my-submission`);

export const getSubmissionsForAssignment = (id) =>
  api.get(`/api/assignments/${id}/submissions`);

export const evaluateSubmission = (submissionId, data) =>
  api.post(`/api/assignments/submissions/${submissionId}/evaluate`, data);
