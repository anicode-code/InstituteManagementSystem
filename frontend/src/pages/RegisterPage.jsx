import { useState, useEffect } from 'react';
import { useNavigate, Navigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { GraduationCap, Eye, EyeOff, UserPlus, CheckSquare, Square, BookOpen } from 'lucide-react';
import { getAllDepartments } from '../api/departments';
import { getCurriculumByDeptAndSemester } from '../data/curriculumSubjects';
import toast from 'react-hot-toast';

// Only STUDENT and FACULTY can self-register
const ROLES = [
  { value: 'STUDENT', label: 'Student' },
  { value: 'FACULTY', label: 'Faculty' },
];

const YEARS = [
  { value: 1, label: '1st Year' },
  { value: 2, label: '2nd Year' },
  { value: 3, label: '3rd Year' },
  { value: 4, label: '4th Year' },
];

const SEMESTERS_BY_YEAR = {
  1: [{ value: 1, label: 'Semester 1' }, { value: 2, label: 'Semester 2' }],
  2: [{ value: 3, label: 'Semester 3' }, { value: 4, label: 'Semester 4' }],
  3: [{ value: 5, label: 'Semester 5' }, { value: 6, label: 'Semester 6' }],
  4: [{ value: 7, label: 'Semester 7' }, { value: 8, label: 'Semester 8' }],
};

export default function RegisterPage() {
  const { user, register } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    name: '',
    rollNumber: '',
    password: '',
    email: '',
    role: 'STUDENT',
    // Student: single department
    departmentId: '',
    // Faculty: multiple departments
    departmentIds: [],
    // Student academic
    year: '',
    semester: '',
    // Student electives
    optionalSubjectCodes: [],
  });

  const [departments, setDepartments] = useState([]);
  const [departmentsLoading, setDepartmentsLoading] = useState(true);
  const [showPw, setShowPw] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Derived curriculum for the selected department+semester
  const [curriculum, setCurriculum] = useState(null);

  if (user) return <Navigate to="/dashboard" replace />;

  useEffect(() => {
    getAllDepartments()
      .then((res) => {
        if (Array.isArray(res.data)) setDepartments(res.data);
      })
      .catch((err) => console.error('Failed to load departments:', err))
      .finally(() => setDepartmentsLoading(false));
  }, []);

  // Update curriculum whenever dept or semester changes (student only)
  useEffect(() => {
    if (form.role !== 'STUDENT' || !form.departmentId || !form.semester) {
      setCurriculum(null);
      return;
    }
    const selectedDept = departments.find((d) => String(d.id) === String(form.departmentId));
    if (!selectedDept) { setCurriculum(null); return; }
    const data = getCurriculumByDeptAndSemester(selectedDept.code, form.semester);
    setCurriculum(data);
    // Clear previous elective selections when dept/semester changes
    setForm((f) => ({ ...f, optionalSubjectCodes: [] }));
  }, [form.departmentId, form.semester, form.role, departments]);

  // When year changes, reset semester
  const handleYearChange = (e) => {
    setForm((f) => ({ ...f, year: e.target.value, semester: '' }));
    setError('');
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    // When role changes, clear dept selections and academic fields
    if (name === 'role') {
      setForm((f) => ({
        ...f,
        role: value,
        departmentId: '',
        departmentIds: [],
        year: '',
        semester: '',
        optionalSubjectCodes: [],
      }));
    } else {
      setForm((f) => ({ ...f, [name]: value }));
    }
    setError('');
  };

  // Faculty: toggle department in multi-select list
  const toggleFacultyDept = (deptId) => {
    setForm((f) => {
      const ids = f.departmentIds.includes(deptId)
        ? f.departmentIds.filter((id) => id !== deptId)
        : [...f.departmentIds, deptId];
      return { ...f, departmentIds: ids };
    });
    setError('');
  };

  // Student: toggle optional subject selection
  const toggleOptionalSubject = (code) => {
    setForm((f) => {
      const codes = f.optionalSubjectCodes.includes(code)
        ? f.optionalSubjectCodes.filter((c) => c !== code)
        : [...f.optionalSubjectCodes, code];
      return { ...f, optionalSubjectCodes: codes };
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!form.name.trim()) { setError('Full name is required.'); return; }
    if (!form.rollNumber.trim()) { setError('Roll number is required.'); return; }
    if (!form.password) { setError('Password is required.'); return; }
    if (form.password.length < 6) { setError('Password must be at least 6 characters.'); return; }
    if (!form.email.trim()) { setError('Email is required.'); return; }

    if (form.role === 'STUDENT') {
      if (!form.departmentId) { setError('Please select your department.'); return; }
      if (!form.year) { setError('Please select your academic year.'); return; }
      if (!form.semester) { setError('Please select your semester.'); return; }
    }

    if (form.role === 'FACULTY') {
      if (form.departmentIds.length === 0) { setError('Please select at least one department.'); return; }
    }

    setLoading(true);
    try {
      const payload = {
        name: form.name.trim(),
        rollNumber: form.rollNumber.trim(),
        password: form.password,
        email: form.email.trim(),
        role: form.role,
      };

      if (form.role === 'STUDENT') {
        payload.departmentId = Number(form.departmentId);
        payload.year = Number(form.year);
        payload.semester = Number(form.semester);
        if (form.optionalSubjectCodes.length > 0) {
          payload.optionalSubjectCodes = form.optionalSubjectCodes;
        }
      }

      if (form.role === 'FACULTY') {
        payload.departmentIds = form.departmentIds.map(Number);
      }

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
      <div className="login-card" style={{ maxWidth: 520 }}>
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
          {/* ── Basic Info ── */}
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
            <label className="form-label" htmlFor="rollNumber">Roll / Employee Number *</label>
            <input
              id="rollNumber"
              className="form-input"
              type="text"
              name="rollNumber"
              autoComplete="off"
              placeholder="e.g. 2026CS001 or FAC001"
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

          {/* ── Role ── */}
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

          {/* ── STUDENT: Single Department ── */}
          {form.role === 'STUDENT' && (
            <div className="form-group">
              <label className="form-label" htmlFor="departmentId">Department *</label>
              <select
                id="departmentId"
                className="form-select"
                name="departmentId"
                value={form.departmentId}
                onChange={handleChange}
                disabled={loading || departmentsLoading}
              >
                <option value="">
                  {departmentsLoading ? 'Loading departments...' : 'Select your department'}
                </option>
                {departments.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name} {d.code ? `(${d.code})` : ''}
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* ── STUDENT: Year & Semester ── */}
          {form.role === 'STUDENT' && (
            <div className="grid-2">
              <div className="form-group">
                <label className="form-label" htmlFor="year">Academic Year *</label>
                <select
                  id="year"
                  className="form-select"
                  name="year"
                  value={form.year}
                  onChange={handleYearChange}
                  disabled={loading}
                >
                  <option value="">Select year</option>
                  {YEARS.map((y) => (
                    <option key={y.value} value={y.value}>{y.label}</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="semester">Semester *</label>
                <select
                  id="semester"
                  className="form-select"
                  name="semester"
                  value={form.semester}
                  onChange={handleChange}
                  disabled={loading || !form.year}
                >
                  <option value="">Select semester</option>
                  {form.year && SEMESTERS_BY_YEAR[form.year]?.map((s) => (
                    <option key={s.value} value={s.value}>{s.label}</option>
                  ))}
                </select>
              </div>
            </div>
          )}

          {/* ── STUDENT: Curriculum display ── */}
          {form.role === 'STUDENT' && curriculum && (
            <div style={{ marginBottom: 20 }}>
              {/* Compulsory Subjects */}
              {curriculum.compulsory?.length > 0 && (
                <div style={{ marginBottom: 16 }}>
                  <div className="form-label" style={{ marginBottom: 8, display: 'flex', alignItems: 'center', gap: 6 }}>
                    <BookOpen size={14} />
                    Compulsory Subjects (automatically enrolled)
                  </div>
                  <div style={{
                    background: 'var(--bg-hover)',
                    borderRadius: 'var(--radius-sm)',
                    border: '1px solid var(--border)',
                    padding: '10px 12px',
                    display: 'flex',
                    flexWrap: 'wrap',
                    gap: 8,
                  }}>
                    {curriculum.compulsory.map((sub) => (
                      <span key={sub.code} style={{
                        background: 'var(--success-light)',
                        color: 'var(--success)',
                        padding: '4px 10px',
                        borderRadius: 20,
                        fontSize: '0.72rem',
                        fontWeight: 600,
                        display: 'inline-flex',
                        gap: 4,
                        alignItems: 'center',
                      }}>
                        <span style={{ opacity: 0.7 }}>{sub.code}</span>
                        {sub.name}
                        {sub.credits && <span style={{ opacity: 0.6 }}>({sub.credits}cr)</span>}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* Optional Subjects */}
              {curriculum.optional?.length > 0 && (
                <div>
                  <div className="form-label" style={{ marginBottom: 8 }}>
                    Optional Subjects — select your preferences
                  </div>
                  <div style={{
                    border: '1px solid var(--border)',
                    borderRadius: 'var(--radius-sm)',
                    overflow: 'hidden',
                  }}>
                    {curriculum.optional.map((sub, idx) => {
                      const selected = form.optionalSubjectCodes.includes(sub.code);
                      return (
                        <button
                          key={sub.code}
                          type="button"
                          onClick={() => toggleOptionalSubject(sub.code)}
                          disabled={loading}
                          style={{
                            width: '100%',
                            display: 'flex',
                            alignItems: 'center',
                            gap: 10,
                            padding: '10px 14px',
                            background: selected ? 'var(--accent-light)' : 'transparent',
                            border: 'none',
                            borderTop: idx > 0 ? '1px solid var(--border)' : 'none',
                            cursor: 'pointer',
                            textAlign: 'left',
                            color: selected ? 'var(--accent)' : 'var(--text-primary)',
                            transition: 'background 0.15s',
                          }}
                        >
                          {selected ? <CheckSquare size={16} /> : <Square size={16} style={{ opacity: 0.4 }} />}
                          <span style={{ flex: 1 }}>
                            <span style={{ fontWeight: 600, fontSize: '0.85rem' }}>{sub.name}</span>
                            <span style={{ marginLeft: 6, fontSize: '0.72rem', opacity: 0.6 }}>
                              {sub.code}{sub.credits ? ` · ${sub.credits} cr` : ''}
                            </span>
                          </span>
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* ── FACULTY: Multi-Department ── */}
          {form.role === 'FACULTY' && (
            <div className="form-group">
              <label className="form-label">Departments * (select all that apply)</label>
              {departmentsLoading ? (
                <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Loading departments...</div>
              ) : (
                <div style={{
                  border: '1px solid var(--border)',
                  borderRadius: 'var(--radius-sm)',
                  overflow: 'hidden',
                }}>
                  {departments.map((d, idx) => {
                    const selected = form.departmentIds.includes(d.id);
                    return (
                      <button
                        key={d.id}
                        type="button"
                        onClick={() => toggleFacultyDept(d.id)}
                        disabled={loading}
                        style={{
                          width: '100%',
                          display: 'flex',
                          alignItems: 'center',
                          gap: 10,
                          padding: '10px 14px',
                          background: selected ? 'var(--accent-light)' : 'transparent',
                          border: 'none',
                          borderTop: idx > 0 ? '1px solid var(--border)' : 'none',
                          cursor: 'pointer',
                          textAlign: 'left',
                          color: selected ? 'var(--accent)' : 'var(--text-primary)',
                          transition: 'background 0.15s',
                        }}
                      >
                        {selected ? <CheckSquare size={16} /> : <Square size={16} style={{ opacity: 0.4 }} />}
                        <span>
                          <span style={{ fontWeight: 600 }}>{d.name}</span>
                          <span style={{ marginLeft: 6, fontSize: '0.72rem', opacity: 0.6 }}>({d.code})</span>
                        </span>
                      </button>
                    );
                  })}
                </div>
              )}
              {form.departmentIds.length > 0 && (
                <div style={{ marginTop: 6, fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  {form.departmentIds.length} department{form.departmentIds.length > 1 ? 's' : ''} selected
                </div>
              )}
            </div>
          )}

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
