import { useState } from 'react';
import { useNavigate, Navigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { GraduationCap, Eye, EyeOff, UserPlus } from 'lucide-react';
import { getAllDepartments } from '../api/departments';
import { useEffect } from 'react';
import toast from 'react-hot-toast';

const ROLES = [
  { value: 'STUDENT',  label: 'Student' },
  { value: 'FACULTY',  label: 'Faculty' },
  { value: 'HOD',      label: 'HOD' },
];

export default function RegisterPage() {
  const { user, register } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    name: '',
    rollNumber: '',
    password: '',
    email: '',
    role: 'STUDENT',
    departmentId: '',
  });
  const [departments, setDepartments] = useState([]);
  const [showPw, setShowPw] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (user) return <Navigate to="/dashboard" replace />;

  const [departmentsLoading, setDepartmentsLoading] = useState(true);

  useEffect(() => {
    getAllDepartments()
      .then((res) => {
        if (Array.isArray(res.data)) {
          setDepartments(res.data);
        }
      })
      .catch((err) => {
        console.error('Failed to load departments:', err);
      })
      .finally(() => {
        setDepartmentsLoading(false);
      });
  }, []);

  const handleChange = (e) => {
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!form.name.trim()) { setError('Full name is required.'); return; }
    if (!form.rollNumber.trim()) { setError('Roll number is required.'); return; }
    if (!form.password) { setError('Password is required.'); return; }
    if (form.password.length < 6) { setError('Password must be at least 6 characters.'); return; }
    if (!form.email.trim()) { setError('Email is required.'); return; }
    if (!form.role) { setError('Role is required.'); return; }

    setLoading(true);
    try {
      const payload = {
        name: form.name.trim(),
        rollNumber: form.rollNumber.trim(),
        password: form.password,
        email: form.email.trim(),
        role: form.role,
        departmentId: form.departmentId ? Number(form.departmentId) : undefined,
      };
      await register(payload);
      toast.success('Registration submitted! Please wait for admin verification before logging in.');
      navigate('/login');
    } catch (err) {
      const data = err?.response?.data;
      let msg;
      if (typeof data === 'string') {
        msg = data;
      } else if (data?.message) {
        msg = data.message;
      } else {
        msg = 'Registration failed. Please try again.';
      }
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-card" style={{ maxWidth: 480 }}>
        <div className="login-logo">
          <div className="login-logo-icon">
            <GraduationCap />
          </div>
          <div>
            <div className="login-logo-text">IMS</div>
            <div className="login-logo-sub">Institute Management System</div>
          </div>
        </div>

        <h2 className="login-title">Create Account</h2>
        <p className="login-subtitle">Register to request access. An admin will verify your account.</p>

        {error && (
          <div className="alert alert-error" style={{ marginBottom: '16px' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label" htmlFor="name">Full Name *</label>
            <input
              id="name"
              className="form-input"
              type="text"
              name="name"
              autoComplete="name"
              placeholder="Your full name"
              value={form.name}
              onChange={handleChange}
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="rollNumber">Roll Number *</label>
            <input
              id="rollNumber"
              className="form-input"
              type="text"
              name="rollNumber"
              autoComplete="off"
              placeholder="e.g. 2026CS001"
              value={form.rollNumber}
              onChange={handleChange}
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="email">Email *</label>
            <input
              id="email"
              className="form-input"
              type="email"
              name="email"
              autoComplete="email"
              placeholder="your@email.com"
              value={form.email}
              onChange={handleChange}
              disabled={loading}
            />
          </div>

          <div className="form-group" style={{ position: 'relative' }}>
            <label className="form-label" htmlFor="reg-password">Password *</label>
            <input
              id="reg-password"
              className="form-input"
              type={showPw ? 'text' : 'password'}
              name="password"
              autoComplete="new-password"
              placeholder="Minimum 6 characters"
              value={form.password}
              onChange={handleChange}
              disabled={loading}
              style={{ paddingRight: '40px' }}
            />
            <button
              type="button"
              onClick={() => setShowPw((p) => !p)}
              style={{
                position: 'absolute', right: 12, bottom: 10,
                background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer',
              }}
            >
              {showPw ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          </div>

          <div className="grid-2">
            <div className="form-group">
              <label className="form-label" htmlFor="role">Role *</label>
              <select
                id="role"
                className="form-select"
                name="role"
                value={form.role}
                onChange={handleChange}
                disabled={loading}
              >
                {ROLES.map((r) => (
                  <option key={r.value} value={r.value}>{r.label}</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label" htmlFor="departmentId">Department</label>
              <select
                id="departmentId"
                className="form-select"
                name="departmentId"
                value={form.departmentId}
                onChange={handleChange}
                disabled={loading || departmentsLoading}
              >
                <option value="">
                  {departmentsLoading ? 'Loading departments...' : 'Select Department (Optional)'}
                </option>
                {departments.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name} {d.code ? `(${d.code})` : ''}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="alert" style={{
            background: 'var(--info-light, rgba(59,130,246,0.1))',
            color: 'var(--info, #3b82f6)',
            borderRadius: 'var(--radius-sm)',
            padding: '10px 14px',
            fontSize: '0.8rem',
            marginBottom: '16px',
            border: '1px solid var(--info, #3b82f6)',
          }}>
            ℹ️ After registration, an admin must verify your account before you can log in.
          </div>

          <button
            type="submit"
            className="btn btn-primary w-full"
            style={{ justifyContent: 'center', padding: '12px' }}
            disabled={loading}
          >
            {loading ? (
              <span className="spinner" style={{ width: 18, height: 18, borderWidth: 2 }} />
            ) : (
              <>
                <UserPlus size={18} />
                Register
              </>
            )}
          </button>
        </form>

        <div style={{ marginTop: 16, textAlign: 'center', fontSize: '0.875rem', color: 'var(--text-muted)' }}>
          Already have an account?{' '}
          <Link to="/login" style={{ color: 'var(--accent)', fontWeight: 600, textDecoration: 'none' }}>
            Sign in
          </Link>
        </div>
      </div>
    </div>
  );
}
