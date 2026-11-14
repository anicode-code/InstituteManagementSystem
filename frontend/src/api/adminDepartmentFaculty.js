import api from './axios';

export const getAdminDepartmentFaculty = (departmentId, params) =>
  api.get(`/api/admin/departments/${departmentId}/faculty`, { params });

export const getAdminDepartmentFacultyProfile = (departmentId, facultyId) =>
  api.get(`/api/admin/departments/${departmentId}/faculty/${facultyId}`);
