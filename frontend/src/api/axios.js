import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' },
});

// Attach JWT token on every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('ims_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle 401 globally
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const isAuthUrl = error.config?.url?.includes('/api/auth/');
      const isPublicPage = window.location.pathname === '/login' || window.location.pathname === '/register';
      if (!isAuthUrl && !isPublicPage) {
        localStorage.removeItem('ims_token');
        localStorage.removeItem('ims_user');
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
