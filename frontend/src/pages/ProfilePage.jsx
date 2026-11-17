import { useEffect, useState } from 'react';
import Layout from '../components/Layout';
import { useAuth } from '../context/AuthContext';
import { getCurrentUser, updateMyProfile } from '../api/users';
import { getCurriculumByDeptAndSemester } from '../data/curriculumSubjects';
import { User, Mail, Hash, Building2, Shield, Pencil, X, Save, BookOpen, CheckSquare, Square } from 'lucide-react';
import toast from 'react-hot-toast';

const ROLE_COLORS = {
  ADMIN:   { bg: 'var(--accent-light)',   color: 'var(--accent)' },
  HOD:     { bg: 'var(--info-light)',     color: 'var(--info)' },
  FACULTY: { bg: 'var(--warning-light)',  color: 'var(--warning)' },
  STUDENT: { bg: 'var(--success-light)',  color: 'var(--success)' },
};

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

const profileForm = (user) => ({
  name: user?.name || '',
  email: user?.email || '',
  phone: user?.phone || '',
  dateOfBirth: user?.dateOfBirth || '',
  gender: user?.gender || '',
  residentialAddress: user?.residentialAddress || '',
  joiningDate: user?.joiningDate || '',
  employmentStatus: user?.employmentStatus || '',
  designation: user?.designation || '',
  qualification: user?.qualification || '',
  specialization: user?.specialization || '',
  yearsOfExperience: user?.yearsOfExperience == null ? '' : String(user.yearsOfExperience),
  employmentType: user?.employmentType || '',
  year: user?.year ? String(user.year) : '',
  semester: user?.semester ? String(user.semester) : '',
  optionalSubjectCodes: user?.selectedSubjects
    ?.filter((subject) => subject.type === 'OPTIONAL')
    .map((subject) => subject.code) || [],
});

function InfoRow({ icon, label, value }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 12,
      padding: '14px 0', borderBottom: '1px solid var(--border)',
    }}>
      <div style={{ color: 'var(--text-muted)', flexShrink: 0 }}>{icon}</div>
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.08em', marginBottom: 2 }}>{label}</div>
        <div style={{ fontWeight: 500, color: value ? 'var(--text-primary)' : 'var(--text-muted)' }}>{value || 'Not provided'}</div>
      </div>
    </div>
  );
}

export default function ProfilePage() {
  const { user: authUser, syncUser } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState(profileForm(null));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const res = await getCurrentUser();
        setProfile(res.data);
        setForm(profileForm(res.data));
      } catch {
        setError('Could not load your profile. Please try again.');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const handleYearChange = (event) => {
    setForm((current) => ({
      ...current,
      year: event.target.value,
      semester: '',
      optionalSubjectCodes: [],
    }));
  };

  const handleSemesterChange = (event) => {
    setForm((current) => ({
      ...current,
      semester: event.target.value,
      optionalSubjectCodes: [],
    }));
  };

  const toggleOptionalSubject = (code) => {
    setForm((current) => ({
      ...current,
      optionalSubjectCodes: current.optionalSubjectCodes.includes(code)
        ? current.optionalSubjectCodes.filter((selectedCode) => selectedCode !== code)
        : [...current.optionalSubjectCodes, code],
    }));
  };

  const handleCancel = () => {
    setForm(profileForm(profile));
    setEditing(false);
    setError('');
  };

  const handleSave = async () => {
    setError('');
    if (!form.name.trim()) { setError('Name is required.'); return; }

    const request = { name: form.name.trim(), email: form.email.trim() };
    if (profile?.role === 'STUDENT') {
      const hasYear = Boolean(form.year);
      const hasSemester = Boolean(form.semester);
      if (hasYear !== hasSemester) {
        setError('Select both academic year and semester.');
        return;
      }
      if (hasYear && Number(form.year) !== Math.ceil(Number(form.semester) / 2)) {
        setError('Select a semester that matches the academic year.');
        return;
      }
      if (hasYear) {
        request.year = Number(form.year);
        request.semester = Number(form.semester);
        request.optionalSubjectCodes = form.optionalSubjectCodes;
      }
    }
    if (profile?.role === 'FACULTY') {
      request.phone = form.phone.trim();
      request.dateOfBirth = form.dateOfBirth || null;
      request.gender = form.gender.trim();
      request.residentialAddress = form.residentialAddress.trim();
      request.joiningDate = form.joiningDate || null;
      request.employmentStatus = form.employmentStatus.trim();
      request.designation = form.designation.trim();
      request.qualification = form.qualification.trim();
      request.specialization = form.specialization.trim();
      request.yearsOfExperience = form.yearsOfExperience === ''
        ? null
        : Number(form.yearsOfExperience);
      request.employmentType = form.employmentType.trim();
    }

    setSaving(true);
    try {
      const res = await updateMyProfile(request);
      setProfile(res.data);
      setForm(profileForm(res.data));
      syncUser({
        ...authUser,
        name: res.data.name,
        email: res.data.email,
        role: res.data.role,
        status: res.data.status,
        department: res.data.department,
        departments: res.data.departments,
        year: res.data.year,
        semester: res.data.semester,
      });
      toast.success('Profile updated');
      setEditing(false);
    } catch (err) {
      setError(err?.response?.data?.message || 'Update failed');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <Layout pageTitle="My Profile">
        <div className="loader"><div className="spinner" /></div>
      </Layout>
    );
  }

  const p = profile || authUser;
  const initials = p?.name?.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2) || '?';
  const roleStyle = ROLE_COLORS[p?.role] || {};
  const curriculum = p?.role === 'STUDENT'
    ? getCurriculumByDeptAndSemester(p?.department?.code, form.semester)
    : null;

  return (
    <Layout pageTitle="My Profile">
      <div style={{ maxWidth: 640, margin: '0 auto' }}>
        {/* Profile Card */}
        <div className="card" style={{ marginBottom: 20 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 20, flexWrap: 'wrap' }}>
            <div className="avatar lg" style={{ background: roleStyle.color || 'var(--accent)', boxShadow: `0 0 24px ${roleStyle.color || 'var(--accent)'}33` }}>
              {initials}
            </div>
            <div style={{ flex: 1 }}>
              <h2 style={{ fontSize: '1.5rem', fontWeight: 800, marginBottom: 4 }}>{p?.name}</h2>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
                <span style={{
                  background: roleStyle.bg, color: roleStyle.color,
                  padding: '4px 12px', borderRadius: 20,
                  fontSize: '0.75rem', fontWeight: 700,
                }}>
                  {p?.role}
                </span>
                <span style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
                  Roll No: {p?.rollNumber}
                </span>
              </div>
            </div>
            <button
              className={`btn ${editing ? 'btn-secondary' : 'btn-primary'}`}
              onClick={() => { editing ? handleCancel() : setEditing(true); setError(''); }}
              type="button"
            >
              {editing ? <><X size={16} /> Cancel</> : <><Pencil size={16} /> Edit</>}
            </button>
          </div>
        </div>

        {/* Edit Form */}
        {editing && (
          <div className="card" style={{ marginBottom: 20 }}>
            <h3 style={{ fontWeight: 700, marginBottom: 16 }}>Edit Profile</h3>
            {error && <div className="alert alert-error">{error}</div>}
            <div className="form-group">
              <label className="form-label">Full Name *</label>
              <input
                className="form-input"
                value={form.name}
                onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                placeholder="Your full name"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Email</label>
              <input
                className="form-input"
                type="email"
                value={form.email}
                onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
                placeholder="your@email.com"
              />
            </div>
            {p?.role === 'FACULTY' && (
              <>
                <h4 style={{ fontWeight: 700, margin: '20px 0 12px' }}>Personal Information</h4>
                <div className="grid-2">
                  <div className="form-group">
                    <label className="form-label" htmlFor="profile-phone">Phone</label>
                    <input id="profile-phone" className="form-input" value={form.phone} onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))} />
                  </div>
                  <div className="form-group">
                    <label className="form-label" htmlFor="profile-date-of-birth">Date of birth</label>
                    <input id="profile-date-of-birth" className="form-input" type="date" value={form.dateOfBirth} onChange={(e) => setForm((f) => ({ ...f, dateOfBirth: e.target.value }))} />
                  </div>
                  <div className="form-group">
                    <label className="form-label" htmlFor="profile-gender">Gender</label>
                    <input id="profile-gender" className="form-input" value={form.gender} onChange={(e) => setForm((f) => ({ ...f, gender: e.target.value }))} />
                  </div>
                  <div className="form-group">
                    <label className="form-label" htmlFor="profile-joining-date">Joining date</label>
                    <input id="profile-joining-date" className="form-input" type="date" value={form.joiningDate} onChange={(e) => setForm((f) => ({ ...f, joiningDate: e.target.value }))} />
                  </div>
                  <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                    <label className="form-label" htmlFor="profile-residential-address">Residential address</label>
                    <textarea id="profile-residential-address" className="form-input" rows={2} value={form.residentialAddress} onChange={(e) => setForm((f) => ({ ...f, residentialAddress: e.target.value }))} />
                  </div>
                  <div className="form-group">
                    <label className="form-label" htmlFor="profile-employment-status">Employment status</label>
                    <input id="profile-employment-status" className="form-input" value={form.employmentStatus} onChange={(e) => setForm((f) => ({ ...f, employmentStatus: e.target.value }))} />
                  </div>
                </div>
                <h4 style={{ fontWeight: 700, margin: '8px 0 12px' }}>Professional Information</h4>
                <div className="grid-2">
                  <div className="form-group">
                    <label className="form-label" htmlFor="profile-designation">Designation</label>
                    <input id="profile-designation" className="form-input" value={form.designation} onChange={(e) => setForm((f) => ({ ...f, designation: e.target.value }))} />
                  </div>
                  <div className="form-group">
                    <label className="form-label" htmlFor="profile-qualification">Qualification</label>
                    <input id="profile-qualification" className="form-input" value={form.qualification} onChange={(e) => setForm((f) => ({ ...f, qualification: e.target.value }))} />
                  </div>
                  <div className="form-group">
                    <label className="form-label" htmlFor="profile-specialization">Specialization</label>
                    <input id="profile-specialization" className="form-input" value={form.specialization} onChange={(e) => setForm((f) => ({ ...f, specialization: e.target.value }))} />
                  </div>
                  <div className="form-group">
                    <label className="form-label" htmlFor="profile-years-of-experience">Years of experience</label>
                    <input id="profile-years-of-experience" className="form-input" type="number" min="0" step="0.1" value={form.yearsOfExperience} onChange={(e) => setForm((f) => ({ ...f, yearsOfExperience: e.target.value }))} />
                  </div>
                  <div className="form-group">
                    <label className="form-label" htmlFor="profile-employment-type">Employment type</label>
                    <input id="profile-employment-type" className="form-input" value={form.employmentType} onChange={(e) => setForm((f) => ({ ...f, employmentType: e.target.value }))} />
                  </div>
                </div>
              </>
            )}
            {p?.role === 'STUDENT' && (
              <>
                <div className="grid-2">
                  <div className="form-group">
                    <label className="form-label" htmlFor="profile-year">Academic Year</label>
                    <select
                      id="profile-year"
                      className="form-select"
                      value={form.year}
                      onChange={handleYearChange}
                      disabled={saving}
                    >
                      <option value="">Select year</option>
                      {YEARS.map((year) => (
                        <option key={year.value} value={year.value}>{year.label}</option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group">
                    <label className="form-label" htmlFor="profile-semester">Semester</label>
                    <select
                      id="profile-semester"
                      className="form-select"
                      value={form.semester}
                      onChange={handleSemesterChange}
                      disabled={saving || !form.year}
                    >
                      <option value="">Select semester</option>
                      {form.year && SEMESTERS_BY_YEAR[form.year]?.map((semester) => (
                        <option key={semester.value} value={semester.value}>{semester.label}</option>
                      ))}
                    </select>
                  </div>
                </div>
                {curriculum && (
                  <div style={{ marginBottom: 16 }}>
                    {curriculum.compulsory?.length > 0 && (
                      <div style={{ marginBottom: 14 }}>
                        <div className="form-label" style={{ marginBottom: 8, display: 'flex', alignItems: 'center', gap: 6 }}>
                          <BookOpen size={14} />
                          Compulsory Subjects (automatically enrolled)
                        </div>
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                          {curriculum.compulsory.map((subject) => (
                            <span key={subject.code} style={{
                              background: 'var(--success-light)',
                              color: 'var(--success)',
                              padding: '4px 10px',
                              borderRadius: 20,
                              fontSize: '0.72rem',
                              fontWeight: 600,
                            }}>
                              {subject.code} · {subject.name}
                            </span>
                          ))}
                        </div>
                      </div>
                    )}
                    {curriculum.optional?.length > 0 && (
                      <div>
                        <div className="form-label" style={{ marginBottom: 8 }}>
                          Optional Subjects — select your preferences
                        </div>
                        <div style={{ border: '1px solid var(--border)', borderRadius: 'var(--radius-sm)', overflow: 'hidden' }}>
                          {curriculum.optional.map((subject, index) => {
                            const selected = form.optionalSubjectCodes.includes(subject.code);
                            return (
                              <button
                                key={subject.code}
                                type="button"
                                onClick={() => toggleOptionalSubject(subject.code)}
                                disabled={saving}
                                style={{
                                  width: '100%',
                                  display: 'flex',
                                  alignItems: 'center',
                                  gap: 10,
                                  padding: '10px 14px',
                                  background: selected ? 'var(--accent-light)' : 'transparent',
                                  border: 'none',
                                  borderTop: index > 0 ? '1px solid var(--border)' : 'none',
                                  cursor: 'pointer',
                                  textAlign: 'left',
                                  color: selected ? 'var(--accent)' : 'var(--text-primary)',
                                }}
                              >
                                {selected ? <CheckSquare size={16} /> : <Square size={16} style={{ opacity: 0.4 }} />}
                                <span style={{ flex: 1 }}>
                                  <span style={{ fontWeight: 600, fontSize: '0.85rem' }}>{subject.name}</span>
                                  <span style={{ marginLeft: 6, fontSize: '0.72rem', opacity: 0.6 }}>
                                    {subject.code}{subject.credits ? ` · ${subject.credits} cr` : ''}
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
              </>
            )}
            <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
              {saving ? <span className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} /> : <><Save size={16} /> Save Changes</>}
            </button>
          </div>
        )}

        {/* Info Details */}
        <div className="card">
          <h3 style={{ fontWeight: 700, marginBottom: 4 }}>Account Information</h3>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem', marginBottom: 8 }}>Your profile details</p>

          <InfoRow icon={<User size={18} />}     label="Full Name"   value={p?.name} />
          <InfoRow icon={<Hash size={18} />}      label="Roll Number" value={p?.rollNumber} />
          <InfoRow icon={<Mail size={18} />}      label="Email"       value={p?.email} />
          <InfoRow icon={<Shield size={18} />}    label="Role"        value={p?.role} />
          <InfoRow icon={<Building2 size={18} />} label="Department"  value={p?.department?.name} />
          {p?.role === 'FACULTY' && (
            <>
              <InfoRow icon={<Mail size={18} />} label="Phone" value={p?.phone} />
              <InfoRow icon={<User size={18} />} label="Date of birth" value={p?.dateOfBirth} />
              <InfoRow icon={<User size={18} />} label="Gender" value={p?.gender} />
              <InfoRow icon={<Building2 size={18} />} label="Residential address" value={p?.residentialAddress} />
              <InfoRow icon={<Building2 size={18} />} label="Joining date" value={p?.joiningDate} />
              <InfoRow icon={<Building2 size={18} />} label="Employment status" value={p?.employmentStatus} />
              <InfoRow icon={<Shield size={18} />} label="Designation" value={p?.designation} />
              <InfoRow icon={<Shield size={18} />} label="Qualification" value={p?.qualification} />
              <InfoRow icon={<Shield size={18} />} label="Specialization" value={p?.specialization} />
              <InfoRow icon={<Shield size={18} />} label="Years of experience" value={p?.yearsOfExperience == null ? null : `${p.yearsOfExperience} years`} />
              <InfoRow icon={<Shield size={18} />} label="Employment type" value={p?.employmentType} />
            </>
          )}
          {p?.role === 'STUDENT' && (
            <>
              <InfoRow icon={<BookOpen size={18} />} label="Academic Year" value={p?.year ? `Year ${p.year}` : null} />
              <InfoRow icon={<BookOpen size={18} />} label="Semester" value={p?.semester ? `Semester ${p.semester}` : null} />
              <InfoRow
                icon={<BookOpen size={18} />}
                label="Enrolled Subjects"
                value={p?.selectedSubjects?.map((subject) => subject.name).join(', ')}
              />
            </>
          )}
          <div style={{ paddingTop: 12, fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            User ID: #{p?.id}
          </div>
        </div>
      </div>
    </Layout>
  );
}
