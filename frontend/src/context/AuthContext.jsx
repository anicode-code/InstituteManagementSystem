import { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { login as apiLogin, register as apiRegister } from '../api/auth';
import { getCurrentUser } from '../api/users';

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

  // Re-sync full user info (department, roles, academic details) if token is present
  useEffect(() => {
    const token = localStorage.getItem('ims_token');
    if (!token) return;

    getCurrentUser()
      .then((res) => {
        if (res.data) {
          const u = res.data;
          const freshUser = {
            id: u.id,
            name: u.name,
            rollNumber: u.rollNumber,
            email: u.email,
            role: u.role,
            status: u.status,
            department: u.department,
            departments: u.departments,
            year: u.year,
            semester: u.semester,
          };
          setUser(freshUser);
          localStorage.setItem('ims_user', JSON.stringify(freshUser));
        }
      })
      .catch(() => {});
  }, []);

  const login = useCallback(async (rollNumber, password) => {
    const res = await apiLogin({ rollNumber, password });
    const { token, id, name, role, status, department } = res.data;
    const userData = { id, name, rollNumber: res.data.rollNumber, role, status, department };
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
