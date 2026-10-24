import api from './axios';

export const getAllSubjects = () => api.get('/api/subjects');
export const getSubjectById = (id) => api.get(`/api/subjects/${id}`);
export const getSubjectsByFaculty = (facultyId) =>
  api.get(`/api/subjects/faculty/${facultyId}`);
export const getSubjectsByStudent = (studentId) =>
  api.get(`/api/subjects/student/${studentId}`);
export const getSubjectsByDepartment = (deptId) =>
  api.get(`/api/subjects/department/${deptId}`);
export const createSubject = (data) => api.post('/api/subjects', data);
// Update only sends name, code, credits
export const updateSubject = (id, data) => api.put(`/api/subjects/${id}`, data);
export const deleteSubject = (id) => api.delete(`/api/subjects/${id}`);
