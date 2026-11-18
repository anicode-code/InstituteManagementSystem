import { BrowserRouter, Routes, Route, Navigate, useParams } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import UsersPage from './pages/UsersPage';
import DepartmentsPage from './pages/DepartmentsPage';
import SubjectsPage from './pages/SubjectsPage';
import AssignmentsPage from './pages/AssignmentsPage';
import StudyMaterialsPage from './pages/StudyMaterialsPage';
import AttendancePage from './pages/AttendancePage';
import ProfilePage from './pages/ProfilePage';
import HodStudentsPage from './pages/HodStudentsPage';
import FacultySubjectRequestsPage from './pages/FacultySubjectRequestsPage';
import HodSubjectAssignmentsPage from './pages/HodSubjectAssignmentsPage';
import AdminDepartmentStudentsPage from './pages/AdminDepartmentStudentsPage';
import AdminDepartmentFacultyPage from './pages/AdminDepartmentFacultyPage';
import AdminDepartmentHodPage from './pages/AdminDepartmentHodPage';

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
          <Route path="/register" element={<RegisterPage />} />

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
          <Route path="/study-materials" element={
            <ProtectedRoute allowedRoles={['HOD', 'FACULTY', 'STUDENT']}>
              <StudyMaterialsPage />
            </ProtectedRoute>
          } />
          <Route path="/attendance" element={
            <ProtectedRoute><AttendancePage /></ProtectedRoute>
          } />
          <Route path="/subject-requests" element={
            <ProtectedRoute allowedRoles={['FACULTY']}><FacultySubjectRequestsPage /></ProtectedRoute>
          } />
          <Route path="/subject-assignments" element={
            <ProtectedRoute allowedRoles={['HOD']}><HodSubjectAssignmentsPage /></ProtectedRoute>
          } />
          <Route path="/profile" element={
            <ProtectedRoute><ProfilePage /></ProtectedRoute>
          } />
          <Route path="/students" element={
            <ProtectedRoute allowedRoles={['HOD']}>
              <HodStudentsPage />
            </ProtectedRoute>
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
          <Route path="/departments/:departmentId" element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <DepartmentHomeRoute />
            </ProtectedRoute>
          } />
          <Route path="/departments/:departmentId/subjects" element={
            <ProtectedRoute allowedRoles={['ADMIN']}><DepartmentSubjectsRoute /></ProtectedRoute>
          } />
          <Route path="/departments/:departmentId/assignments" element={
            <ProtectedRoute allowedRoles={['ADMIN']}><DepartmentAssignmentsRoute /></ProtectedRoute>
          } />
          <Route path="/departments/:departmentId/attendance" element={
            <ProtectedRoute allowedRoles={['ADMIN']}><DepartmentAttendanceRoute /></ProtectedRoute>
          } />
          <Route path="/departments/:departmentId/students" element={
            <ProtectedRoute allowedRoles={['ADMIN']}><DepartmentStudentsRoute /></ProtectedRoute>
          } />
          <Route path="/departments/:departmentId/students/:studentId" element={
            <ProtectedRoute allowedRoles={['ADMIN']}><DepartmentStudentProfileRoute /></ProtectedRoute>
          } />
          <Route path="/departments/:departmentId/faculty" element={
            <ProtectedRoute allowedRoles={['ADMIN']}><DepartmentFacultyRoute /></ProtectedRoute>
          } />
          <Route path="/departments/:departmentId/faculty/:facultyId" element={
            <ProtectedRoute allowedRoles={['ADMIN']}><DepartmentFacultyProfileRoute /></ProtectedRoute>
          } />
          <Route path="/departments/:departmentId/hod" element={
            <ProtectedRoute allowedRoles={['ADMIN']}><DepartmentHodRoute /></ProtectedRoute>
          } />
          <Route path="/departments/:departmentId/hod/:hodId" element={
            <ProtectedRoute allowedRoles={['ADMIN']}><DepartmentHodProfileRoute /></ProtectedRoute>
          } />

          {/* Catch-all */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

function DepartmentSubjectsRoute() {
  const { departmentId } = useParams();
  return <SubjectsPage departmentId={departmentId} />;
}

function DepartmentHomeRoute() {
  const { departmentId } = useParams();
  return <Navigate to={`/departments/${departmentId}/subjects`} replace />;
}

function DepartmentAssignmentsRoute() {
  const { departmentId } = useParams();
  return <AssignmentsPage departmentId={departmentId} />;
}

function DepartmentAttendanceRoute() {
  const { departmentId } = useParams();
  return <AttendancePage departmentId={departmentId} />;
}

function DepartmentStudentsRoute() {
  const { departmentId } = useParams();
  return <AdminDepartmentStudentsPage departmentId={departmentId} />;
}

function DepartmentStudentProfileRoute() {
  const { departmentId, studentId } = useParams();
  return <AdminDepartmentStudentsPage departmentId={departmentId} studentId={studentId} />;
}

function DepartmentFacultyRoute() {
  const { departmentId } = useParams();
  return <AdminDepartmentFacultyPage departmentId={departmentId} />;
}

function DepartmentFacultyProfileRoute() {
  const { departmentId, facultyId } = useParams();
  return <AdminDepartmentFacultyPage departmentId={departmentId} facultyId={facultyId} />;
}

function DepartmentHodRoute() {
  const { departmentId } = useParams();
  return <AdminDepartmentHodPage departmentId={departmentId} />;
}

function DepartmentHodProfileRoute() {
  const { departmentId, hodId } = useParams();
  return <AdminDepartmentHodPage departmentId={departmentId} hodId={hodId} />;
}
