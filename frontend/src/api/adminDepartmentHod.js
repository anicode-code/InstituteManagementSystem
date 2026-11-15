import api from './axios';

export const getAdminDepartmentHodDirectory = (departmentId) =>
  api.get(`/api/admin/departments/${departmentId}/hod`);

export const getAdminDepartmentHodProfile = (departmentId, hodId) =>
  api.get(`/api/admin/departments/${departmentId}/hod/${hodId}`);
