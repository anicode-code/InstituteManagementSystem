import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import UsersPage from './pages/UsersPage';
import DepartmentsPage from './pages/DepartmentsPage';
import SubjectsPage from './pages/SubjectsPage';
import AssignmentsPage from './pages/AssignmentsPage';
import AttendancePage from './pages/AttendancePage';
import ProfilePage from './pages/ProfilePage';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Toaster
          position="top-right"
          toastOptions={{
            style: {
              background: 'var(--bg-card)',
              color: 'var(--text-primary)',
              border: '1px solid var(--border-light)',
              borderRadius: '10px',
              fontSize: '0.875rem',
            },
            success: { iconTheme: { primary: 'var(--success)', secondary: '#fff' } },
            error:   { iconTheme: { primary: 'var(--danger)',  secondary: '#fff' } },
          }}
        />
        <Routes>
          {/* Public */}
          <Route path="/login" element={<LoginPage />} />

          {/* Redirect root to dashboard */}
          <Route path="/" element={<Navigate to="/dashboard" replace />} />

          {/* Protected — all authenticated */}
          <Route path="/dashboard" element={
            <ProtectedRoute><DashboardPage /></ProtectedRoute>
          } />
          <Route path="/subjects" element={
            <ProtectedRoute><SubjectsPage /></ProtectedRoute>
          } />
          <Route path="/assignments" element={
            <ProtectedRoute><AssignmentsPage /></ProtectedRoute>
          } />
          <Route path="/attendance" element={
            <ProtectedRoute><AttendancePage /></ProtectedRoute>
          } />
          <Route path="/profile" element={
            <ProtectedRoute><ProfilePage /></ProtectedRoute>
          } />

          {/* Protected — ADMIN and HOD only */}
          <Route path="/users" element={
            <ProtectedRoute allowedRoles={['ADMIN', 'HOD']}>
              <UsersPage />
            </ProtectedRoute>
          } />
          <Route path="/departments" element={
            <ProtectedRoute allowedRoles={['ADMIN', 'HOD']}>
              <DepartmentsPage />
            </ProtectedRoute>
          } />

          {/* Catch-all */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
