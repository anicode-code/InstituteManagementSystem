import api from './axios';

export const getAllUsers = (role) =>
  api.get('/api/users', { params: role ? { role } : {} });

export const getHodStudentRoster = (year, sortBy = 'rollNumber', direction = 'asc') =>
  api.get('/api/hod/students', { params: { year, sortBy, direction } });

export const getHodStudentDetails = (studentId) =>
  api.get(`/api/hod/students/${studentId}`);

export const getUserById = (id) => api.get(`/api/users/${id}`);

export const getCurrentUser = () => api.get('/api/users/me');

export const updateUser = (id, data) => api.put(`/api/users/${id}`, data);

export const deleteUser = (id) => api.delete(`/api/users/${id}`);

export const getPendingUsers = () => api.get('/api/users/pending');

export const verifyUser = (id) => api.put(`/api/users/${id}/verify`);

export const verifyBulk = (userIds) => api.post('/api/users/verify-bulk', { userIds });

export const rejectUser = (id) => api.delete(`/api/users/${id}/reject`);

export const rejectUsers = (userIds) => api.post('/api/users/reject-bulk', { userIds });
