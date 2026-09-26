import api from './axios';

export const getAllUsers = (role) =>
  api.get('/api/users', { params: role ? { role } : {} });

export const getUserById = (id) => api.get(`/api/users/${id}`);

export const getCurrentUser = () => api.get('/api/users/me');

export const createUser = (data) => api.post('/api/users', data);

export const updateUser = (id, data) => api.put(`/api/users/${id}`, data);

export const deleteUser = (id) => api.delete(`/api/users/${id}`);
