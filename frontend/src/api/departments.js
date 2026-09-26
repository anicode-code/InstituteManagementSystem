import api from './axios';

export const getAllDepartments = () => api.get('/api/departments');
export const getDepartmentById = (id) => api.get(`/api/departments/${id}`);
export const createDepartment = (data) => api.post('/api/departments', data);
export const updateDepartment = (id, data) => api.put(`/api/departments/${id}`, data);
export const deleteDepartment = (id) => api.delete(`/api/departments/${id}`);
