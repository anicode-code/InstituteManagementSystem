import { useState } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { GraduationCap, Eye, EyeOff, LogIn } from 'lucide-react';
import toast from 'react-hot-toast';

export default function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({ username: '', password: '' });
  const [showPw, setShowPw] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (user) return <Navigate to="/dashboard" replace />;

  const handleChange = (e) => {
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.username || !form.password) {
      setError('Username and password are required.');
      return;
    }
    setLoading(true);
    try {
      await login(form.username, form.password);
      toast.success('Welcome back!');
      navigate('/dashboard');
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        err?.response?.data ||
        'Invalid credentials. Please try again.';
      setError(typeof msg === 'string' ? msg : 'Login failed.');
    } finally {
      setLoading(false);
    }
  };

  const fillDemo = (username, password) => {
    setForm({ username, password });
    setError('');
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-logo">
          <div className="login-logo-icon">
            <GraduationCap />
          </div>
          <div>
            <div className="login-logo-text">IMS</div>
            <div className="login-logo-sub">Institute Management System</div>
          </div>
        </div>

        <h2 className="login-title">Welcome back</h2>
        <p className="login-subtitle">Sign in to your account to continue</p>

        {error && (
          <div className="alert alert-error" style={{ marginBottom: '20px' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label" htmlFor="username">Username</label>
            <input
              id="username"
              className="form-input"
              type="text"
              name="username"
              autoComplete="username"
              placeholder="Enter your username"
              value={form.username}
              onChange={handleChange}
              disabled={loading}
            />
          </div>

          <div className="form-group" style={{ position: 'relative' }}>
            <label className="form-label" htmlFor="password">Password</label>
            <input
              id="password"
              className="form-input"
              type={showPw ? 'text' : 'password'}
              name="password"
              autoComplete="current-password"
              placeholder="Enter your password"
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

          <button
            type="submit"
            className="btn btn-primary w-full"
            style={{ marginTop: '8px', justifyContent: 'center', padding: '12px' }}
            disabled={loading}
          >
            {loading ? (
              <span className="spinner" style={{ width: 18, height: 18, borderWidth: 2 }} />
            ) : (
              <>
                <LogIn size={18} />
                Sign In
              </>
            )}
          </button>
        </form>

        {/* Demo credentials */}
        <div style={{
          marginTop: 28,
          padding: '16px',
          background: 'var(--bg-secondary)',
          borderRadius: 'var(--radius-sm)',
          border: '1px solid var(--border)',
        }}>
          <p style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', marginBottom: 10, textTransform: 'uppercase', letterSpacing: '0.08em' }}>
            Demo Accounts
          </p>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 6 }}>
            {[
              { label: 'Admin',   u: 'admin',   p: 'admin123',   cls: 'badge-admin' },
              { label: 'HOD',     u: 'cse_hod',     p: 'cse_hod123',     cls: 'badge-hod' },
              { label: 'Faculty', u: 'faculty1', p: 'faculty123', cls: 'badge-faculty' },
              { label: 'Student', u: 'demo_student', p: 'demo_student123', cls: 'badge-student' },
            ].map(({ label, u, p, cls }) => (
              <button
                key={u}
                type="button"
                className="btn btn-ghost"
                style={{ justifyContent: 'flex-start', fontSize: '0.78rem', padding: '6px 8px' }}
                onClick={() => fillDemo(u, p)}
              >
                <span className={`topbar-badge ${cls}`} style={{ marginRight: 4 }}>{label}</span>
                {u}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
