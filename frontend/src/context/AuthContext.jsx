import { createContext, useContext, useState, useCallback } from 'react';
import { login as apiLogin, register as apiRegister } from '../api/auth';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem('ims_user');
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });

  const login = useCallback(async (username, password) => {
    const res = await apiLogin({ username, password });
    const { token, id, name, role } = res.data;
    const userData = { id, name, username: res.data.username, role };
    localStorage.setItem('ims_token', token);
    localStorage.setItem('ims_user', JSON.stringify(userData));
    setUser(userData);
    return userData;
  }, []);

  const register = useCallback(async (formData) => {
    const res = await apiRegister(formData);
    return res.data;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('ims_token');
    localStorage.removeItem('ims_user');
    setUser(null);
  }, []);

  const isAdmin = user?.role === 'ADMIN';
  const isHod = user?.role === 'HOD';
  const isFaculty = user?.role === 'FACULTY';
  const isStudent = user?.role === 'STUDENT';
  const isAdminOrHod = isAdmin || isHod;

  return (
    <AuthContext.Provider
      value={{ user, login, logout, register, isAdmin, isHod, isFaculty, isStudent, isAdminOrHod }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};
