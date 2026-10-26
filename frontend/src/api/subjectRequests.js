import api from './axios';

export const getEligibleRequestSubjects = () =>
  api.get('/api/subject-requests/eligible-subjects');

export const getMySubjectRequests = () =>
  api.get('/api/subject-requests/mine');

export const createSubjectRequest = (subjectId) =>
  api.post('/api/subject-requests', { subjectId: Number(subjectId) });

export const getHodSubjectRequests = (params = {}) =>
  api.get('/api/subject-requests/for-hod', { params });

export const decideSubjectRequest = (requestId, decision) =>
  api.put(`/api/subject-requests/${requestId}/decision`, { decision });

export const getHodDepartmentsForSubjects = () =>
  api.get('/api/subject-requests/hod-departments');

export const getHodManagedSubjects = () =>
  api.get('/api/subject-requests/hod-subjects');

export const getAssignedFaculty = (subjectId) =>
  api.get(`/api/subject-assignments/subjects/${subjectId}/faculty`);

export const getEligibleSubjectFaculty = (subjectId) =>
  api.get(`/api/subject-assignments/subjects/${subjectId}/eligible-faculty`);

export const assignFacultyToSubject = (subjectId, facultyId) =>
  api.post(`/api/subject-assignments/subjects/${subjectId}/faculty`, {
    facultyId: Number(facultyId),
  });

export const removeFacultyFromSubject = (subjectId, facultyId) =>
  api.delete(`/api/subject-assignments/subjects/${subjectId}/faculty/${facultyId}`);
