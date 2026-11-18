import api from './axios';

export const getAllAssignments = () => api.get('/api/assignments');
export const getAssignmentById = (id) => api.get(`/api/assignments/${id}`);
export const getAssignmentsByFaculty = (facultyId) =>
  api.get(`/api/assignments/faculty/${facultyId}`);
export const getAssignmentsBySubject = (subjectId) =>
  api.get(`/api/assignments/subject/${subjectId}`);

const assignmentFormData = (data) => {
  const form = new FormData();
  form.append('title', String(data.title));
  form.append('description', String(data.description ?? ''));
  form.append('dueDate', String(data.dueDate));
  form.append('maxMarks', String(data.maxMarks ? Number(data.maxMarks) : 100));
  form.append('subjectId', String(data.subjectId));
  if (data.facultyId != null) form.append('facultyId', String(data.facultyId));
  (data.files || []).forEach((file) => form.append('files', file));
  return form;
};

export const createAssignment = (data) =>
  api.post('/api/assignments', assignmentFormData(data));

export const updateAssignment = (id, data) =>
  api.put(`/api/assignments/${id}`, assignmentFormData(data));

export const deleteAssignment = (id) => api.delete(`/api/assignments/${id}`);

// Submissions & Evaluation
export const submitAssignment = (id, submissionText, files = []) => {
  const form = new FormData();
  form.append('submissionText', String(submissionText ?? ''));
  files.forEach((file) => form.append('files', file));
  return api.post(`/api/assignments/${id}/submit`, form);
};

export const getMySubmission = (id) =>
  api.get(`/api/assignments/${id}/my-submission`);

export const getSubmissionsForAssignment = (id) =>
  api.get(`/api/assignments/${id}/submissions`);

export const evaluateSubmission = (submissionId, data) =>
  api.post(`/api/assignments/submissions/${submissionId}/evaluate`, data);

export const downloadAssignmentAttachment = (assignmentId, attachmentId) =>
  api.get(`/api/assignments/${assignmentId}/attachments/${attachmentId}/download`, {
    responseType: 'blob',
  });

export const downloadSubmissionAttachment = (submissionId, attachmentId) =>
  api.get(`/api/assignments/submissions/${submissionId}/attachments/${attachmentId}/download`, {
    responseType: 'blob',
  });

export const getAssignmentAttachmentMetadata = (assignmentId) =>
  api.get(`/api/assignments/${assignmentId}/attachments`);

export const getSubmissionAttachmentMetadata = (submissionId) =>
  api.get(`/api/assignments/submissions/${submissionId}/attachments`);
