import api from './axios';

export const getStudyMaterials = () => api.get('/api/study-materials');

export const uploadStudyMaterials = ({ subjectId, title, description, files }) => {
  const form = new FormData();
  form.append('subjectId', String(subjectId));
  if (title?.trim()) form.append('title', title.trim());
  if (description?.trim()) form.append('description', description.trim());
  files.forEach((file) => form.append('files', file));
  return api.post('/api/study-materials', form);
};

export const downloadStudyMaterial = (id) =>
  api.get(`/api/study-materials/${id}/download`, { responseType: 'blob' });

export const deleteStudyMaterial = (id) =>
  api.delete(`/api/study-materials/${id}`);
