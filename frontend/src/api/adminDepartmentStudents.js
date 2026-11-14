import api from './axios';

export const getAdminDepartmentStudents = (departmentId, params) =>
  api.get(`/api/admin/departments/${departmentId}/students`, { params });

export const getAdminDepartmentStudentProfile = (departmentId, studentId) =>
  api.get(`/api/admin/departments/${departmentId}/students/${studentId}`);
