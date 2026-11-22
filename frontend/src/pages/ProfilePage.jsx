import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import Layout from '../components/Layout';
import ProfileAvatar from '../components/ProfileAvatar';
import { useAuth } from '../context/AuthContext';
import { getCurrentUser, removeProfilePicture, updateMyProfile, uploadProfilePicture } from '../api/users';
import { getCurriculumByDeptAndSemester } from '../data/curriculumSubjects';
import { User, Mail, Hash, Building2, Shield, Pencil, X, Save, BookOpen, CheckSquare, Square, Camera, Trash2 } from 'lucide-react';
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

const GENDERS = ['Male', 'Female', 'Other', 'Prefer not to say'];
const ACADEMIC_DESIGNATIONS = [
  'FACULTY',
  'ASSISTANT PROFESSOR',
  'ASSOCIATE PROFESSOR',
  'PROFESSOR',
  'LECTURER',
  'SENIOR LECTURER',
  'VISITING PROFESSOR',
];
const EMPLOYMENT_STATUSES = ['Employed', 'On Leave', 'Resigned', 'Retired', 'Terminated'];
const EMPLOYMENT_TYPES = ['Permanent', 'Contractual', 'Temporary', 'Visiting Faculty', 'Guest Faculty'];

const todayDate = () => {
  const today = new Date();
  const month = String(today.getMonth() + 1).padStart(2, '0');
  const day = String(today.getDate()).padStart(2, '0');
  return `${today.getFullYear()}-${month}-${day}`;
};

const isValidDate = (value) => {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value || '')) return false;
  const date = new Date(`${value}T00:00:00Z`);
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value;
};

const profileForm = (user) => ({
  name: user?.name || '',
  email: user?.email || '',
  registrationNumber: user?.registrationNumber || '',
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
  semesterSgpas: Object.fromEntries(
    Object.entries(user?.semesterSgpas || {}).map(([semester, sgpa]) => [semester, String(sgpa)]),
  ),
  optionalSubjectCodes: user?.selectedSubjects
    ?.filter((subject) => subject.type === 'OPTIONAL')
    .map((subject) => subject.code) || [],
});

function FieldError({ message }) {
  return message ? (
    <div role="alert" style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: 4 }}>
      {message}
    </div>
  ) : null;
}

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
  const [stagedPicture, setStagedPicture] = useState(null);
  const [pictureError, setPictureError] = useState('');
  const [picturePreviewOpen, setPicturePreviewOpen] = useState(false);
  const profilePictureInput = useRef(null);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});

  useEffect(() => {
    const load = async () => {
      try {
        const res = await getCurrentUser();
        setProfile(res.data);
        setForm(profileForm(res.data));
        syncUser({ ...authUser, profileImageUrl: res.data.profileImageUrl });
      } catch {
        setError('Could not load your profile. Please try again.');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  useEffect(() => {
    const previewUrl = stagedPicture?.type === 'upload' ? stagedPicture.previewUrl : null;
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
    };
  }, [stagedPicture]);

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
    setFieldErrors({});
    setPictureError('');
    setStagedPicture(null);
  };

  const updateFormField = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
    setFieldErrors((current) => ({ ...current, [field]: '' }));
  };

  const handleProfilePictureSelect = (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;

    setPictureError('');
    setStagedPicture({
      type: 'upload',
      file,
      previewUrl: URL.createObjectURL(file),
    });
  };

  const handleProfilePictureRemove = () => {
    setPictureError('');
    setStagedPicture({ type: 'remove' });
  };

  const syncSavedProfile = (savedProfile) => {
    syncUser({
      ...authUser,
      name: savedProfile.name,
      email: savedProfile.email,
      role: savedProfile.role,
      status: savedProfile.status,
      department: savedProfile.department,
      departments: savedProfile.departments,
      year: savedProfile.year,
      semester: savedProfile.semester,
      registrationNumber: savedProfile.registrationNumber,
      phone: savedProfile.phone,
      dateOfBirth: savedProfile.dateOfBirth,
      residentialAddress: savedProfile.residentialAddress,
      gender: savedProfile.gender,
      joiningDate: savedProfile.joiningDate,
      employmentStatus: savedProfile.employmentStatus,
      designation: savedProfile.designation,
      qualification: savedProfile.qualification,
      specialization: savedProfile.specialization,
      yearsOfExperience: savedProfile.yearsOfExperience,
      employmentType: savedProfile.employmentType,
      profileImageUrl: savedProfile.profileImageUrl,
      semesterSgpas: savedProfile.semesterSgpas,
      yearlyGpas: savedProfile.yearlyGpas,
    });
  };

  const updateSemesterSgpa = (semester, value) => {
    setForm((current) => ({
      ...current,
      semesterSgpas: { ...current.semesterSgpas, [semester]: value },
    }));
    setFieldErrors((current) => ({ ...current, [`sgpa-${semester}`]: '' }));
  };

  const validateFacultyForm = () => {
    const errors = {};
    if (!/^[6-9]\d{9}$/.test(form.phone.trim())) {
      errors.phone = 'Enter a valid 10-digit Indian mobile number.';
    }
    if (!GENDERS.includes(form.gender)) errors.gender = 'Select a gender.';
    if (form.dateOfBirth && !isValidDate(form.dateOfBirth)) {
      errors.dateOfBirth = 'Enter a valid date of birth.';
    } else if (form.dateOfBirth && form.dateOfBirth > todayDate()) {
      errors.dateOfBirth = 'Date of birth cannot be in the future.';
    }
    if (!form.joiningDate) {
      errors.joiningDate = 'Joining date is required.';
    } else if (!isValidDate(form.joiningDate)) {
      errors.joiningDate = 'Enter a valid joining date.';
    } else if (form.joiningDate > todayDate()) {
      errors.joiningDate = 'Joining date cannot be in the future.';
    } else if (form.dateOfBirth && isValidDate(form.dateOfBirth) && form.joiningDate < form.dateOfBirth) {
      errors.joiningDate = 'Joining date cannot be before date of birth.';
    }
    if (!EMPLOYMENT_STATUSES.includes(form.employmentStatus)) {
      errors.employmentStatus = 'Select an employment status.';
    }
    if (!ACADEMIC_DESIGNATIONS.includes(form.designation)) {
      errors.designation = 'Select an academic designation.';
    }
    if (!form.qualification.trim()) errors.qualification = 'Qualification is required.';
    if (!EMPLOYMENT_TYPES.includes(form.employmentType)) {
      errors.employmentType = 'Select an employment type.';
    }
    if (form.yearsOfExperience !== ''
      && (!Number.isFinite(Number(form.yearsOfExperience)) || Number(form.yearsOfExperience) < 0)) {
      errors.yearsOfExperience = 'Experience must be a non-negative number.';
    }
    if (form.email.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      errors.email = 'Enter a valid email address.';
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const validateStudentForm = () => {
    const errors = {};
    if (form.phone && !/^[6-9]\d{9}$/.test(form.phone)) {
      errors.phone = 'Enter a valid 10-digit Indian mobile number.';
    }
    if (form.dateOfBirth && !isValidDate(form.dateOfBirth)) {
      errors.dateOfBirth = 'Enter a valid date of birth.';
    } else if (form.dateOfBirth && form.dateOfBirth > todayDate()) {
      errors.dateOfBirth = 'Date of birth cannot be in the future.';
    }
    if (form.registrationNumber.length > 50) {
      errors.registrationNumber = 'Registration number cannot exceed 50 characters.';
    }
    if (form.semester) {
      const currentSemester = Number(form.semester);
      if (Object.entries(form.semesterSgpas).some(([semester, sgpa]) => (
        sgpa !== '' && Number(semester) >= currentSemester
      ))) {
        errors.semester = 'Current semester must be after every semester with an entered SGPA.';
      }
      for (const [semester, sgpa] of Object.entries(form.semesterSgpas)) {
        if (sgpa === '' || Number(semester) >= currentSemester) continue;
        const value = Number(sgpa);
        if (!Number.isFinite(value) || value < 0 || value > 10) {
          errors[`sgpa-${semester}`] = 'SGPA must be between 0 and 10.';
        }
      }
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSave = async () => {
    setError('');
    if (!form.name.trim()) {
      setFieldErrors((current) => ({ ...current, name: 'Name is required.' }));
      return;
    }
    if (form.email.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      setFieldErrors((current) => ({ ...current, email: 'Enter a valid email address.' }));
      return;
    }
    if (profile?.role === 'FACULTY' || profile?.role === 'HOD') {
      if (!validateFacultyForm()) return;
    }
    if (profile?.role === 'STUDENT' && !validateStudentForm()) return;
    setFieldErrors({});

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
        request.semesterSgpas = Object.fromEntries(
          Object.entries(form.semesterSgpas)
            .filter(([semester, sgpa]) => sgpa !== '' && Number(semester) < Number(form.semester))
            .map(([semester, sgpa]) => [semester, Number(sgpa)]),
        );
      }
      request.registrationNumber = form.registrationNumber.trim();
      request.phone = form.phone.trim();
      request.dateOfBirth = form.dateOfBirth || null;
      request.residentialAddress = form.residentialAddress.trim();
    }
    if (profile?.role === 'FACULTY' || profile?.role === 'HOD') {
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
      syncSavedProfile(res.data);

      if (stagedPicture?.type === 'upload' || stagedPicture?.type === 'remove') {
        try {
          let pictureResponse;
          if (stagedPicture.type === 'upload') {
            const formData = new FormData();
            formData.append('file', stagedPicture.file);
            pictureResponse = await uploadProfilePicture(formData);
          } else {
            pictureResponse = await removeProfilePicture();
          }
          setProfile(pictureResponse.data);
          syncSavedProfile(pictureResponse.data);
        } catch (pictureSaveError) {
          const message = pictureSaveError?.response?.data?.message;
          setPictureError(message || 'Could not save your profile picture.');
          setError('Your profile details were saved, but your picture was not. Please try Save Changes again.');
          return;
        }
      }

      toast.success('Profile updated');
      setStagedPicture(null);
      setEditing(false);
    } catch (err) {
      const responseErrors = err?.response?.data?.errors;
      if (responseErrors) {
        setFieldErrors((current) => ({ ...current, ...responseErrors }));
      }
      const message = err?.response?.data?.message;
      const lowerMessage = message?.toLowerCase() || '';
      if (lowerMessage.includes('email')) {
        setFieldErrors((current) => ({ ...current, email: message }));
      } else if (lowerMessage.includes('registration number')) {
        setFieldErrors((current) => ({ ...current, registrationNumber: message }));
      } else if (lowerMessage.includes('phone')) {
        setFieldErrors((current) => ({ ...current, phone: message }));
      } else if (lowerMessage.includes('date of birth')) {
        setFieldErrors((current) => ({ ...current, dateOfBirth: message }));
      }
      setError(message || 'Update failed');
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
  const roleStyle = ROLE_COLORS[p?.role] || {};
  const profilePictureSrc = stagedPicture?.type === 'upload'
    ? stagedPicture.previewUrl
    : stagedPicture?.type === 'remove'
      ? null
      : p?.profileImageUrl || p?.photoUrl;
  const curriculum = p?.role === 'STUDENT'
    ? getCurriculumByDeptAndSemester(p?.department?.code, form.semester)
    : null;

  return (
    <Layout pageTitle="My Profile">
      <div style={{ maxWidth: 640, margin: '0 auto' }}>
        {/* Profile Card */}
        <div className="card" style={{ marginBottom: 20 }}>
          <div className={`profile-card-header${editing ? ' is-editing' : ''}`}>
            <button
              className="profile-picture-trigger"
              type="button"
              onClick={() => setPicturePreviewOpen(true)}
              aria-label={`View ${p?.name || 'user'}'s profile picture`}
            >
              <ProfileAvatar src={profilePictureSrc} name={p?.name} size="lg" />
            </button>
            <div className="profile-card-identity">
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
              className={`btn ${editing ? 'btn-secondary' : 'btn-primary'} profile-card-edit-button`}
              onClick={() => { editing ? handleCancel() : setEditing(true); setError(''); }}
              disabled={saving}
              type="button"
            >
              {editing ? <><X size={16} /> Cancel</> : <><Pencil size={16} /> Edit</>}
            </button>
            {editing && (
              <div className="profile-picture-editor">
                <button
                  className="btn btn-secondary btn-sm"
                  type="button"
                  onClick={() => profilePictureInput.current?.click()}
                  disabled={saving}
                >
                  <Camera size={15} />
                  Choose new picture
                </button>
                {stagedPicture?.type === 'remove' ? (
                  <button
                    className="btn btn-secondary btn-sm"
                    type="button"
                    onClick={() => { setStagedPicture(null); setPictureError(''); }}
                    disabled={saving}
                  >
                    <Camera size={15} />
                    Undo removal
                  </button>
                ) : profilePictureSrc ? (
                  <button
                    className="btn btn-secondary btn-sm"
                    type="button"
                    onClick={handleProfilePictureRemove}
                    disabled={saving}
                  >
                    <Trash2 size={15} />
                    Remove picture
                  </button>
                ) : null}
                <input
                  ref={profilePictureInput}
                  type="file"
                  accept="image/jpeg,image/png,image/gif"
                  onChange={handleProfilePictureSelect}
                  disabled={saving}
                  style={{ display: 'none' }}
                  tabIndex={-1}
                />
                <span className="profile-picture-help">
                  JPG, PNG, or GIF · max 5 MB. Changes apply when you save.
                </span>
                {pictureError && <FieldError message={pictureError} />}
              </div>
            )}
          </div>
        </div>

        {picturePreviewOpen && createPortal(
          <div
            className="modal-overlay profile-picture-preview-overlay"
            onClick={() => setPicturePreviewOpen(false)}
          >
            <div
              className="modal profile-picture-preview-modal"
              role="dialog"
              aria-modal="true"
              aria-label={`${p?.name || 'User'}'s profile picture`}
              onClick={(event) => event.stopPropagation()}
            >
              <div className="modal-header">
                <h3 className="modal-title">{p?.name || 'User'}'s profile picture</h3>
                <button
                  className="btn btn-secondary btn-sm"
                  type="button"
                  onClick={() => setPicturePreviewOpen(false)}
                  aria-label="Close profile picture preview"
                >
                  <X size={16} />
                </button>
              </div>
              <div className="profile-picture-preview-body">
                <ProfileAvatar
                  src={profilePictureSrc}
                  name={p?.name}
                  className="profile-picture-preview-image"
                />
              </div>
            </div>
          </div>,
          document.body,
        )}

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
                onChange={(e) => updateFormField('name', e.target.value)}
                placeholder="Your full name"
                aria-invalid={Boolean(fieldErrors.name)}
              />
              <FieldError message={fieldErrors.name} />
            </div>
            <div className="form-group">
              <label className="form-label">Email</label>
              <input
                className="form-input"
                type="email"
                value={form.email}
                onChange={(e) => updateFormField('email', e.target.value)}
                placeholder="your@email.com"
                aria-invalid={Boolean(fieldErrors.email)}
              />
              <FieldError message={fieldErrors.email} />
            </div>
            {(p?.role === 'FACULTY' || p?.role === 'HOD') && (
              <>
                <h4 style={{ fontWeight: 700, margin: '20px 0 12px' }}>Personal Information</h4>
                <div className="grid-2">
                  <div className="form-group">
                    <label className="form-label" htmlFor="profile-phone">Phone *</label>
                    <input id="profile-phone" className="form-input" type="tel" inputMode="numeric" maxLength={10} value={form.phone} onChange={(e) => updateFormField('phone', e.target.value)} aria-invalid={Boolean(fieldErrors.phone)} />
                    <FieldError message={fieldErrors.phone} />
                  </div>
                  <div className="form-group">
                    <label className="form-label" htmlFor="profile-date-of-birth">Date of birth</label>
                    <input id="profile-date-of-birth" className="form-input" type="date" max={todayDate()} value={form.dateOfBirth} onChange={(e) => updateFormField('dateOfBirth', e.target.value)} aria-invalid={Boolean(fieldErrors.dateOfBirth)} />
                    <FieldError message={fieldErrors.dateOfBirth} />
                  </div>
                  <div className="form-group">
                    <label className="form-label" htmlFor="profile-gender">Gender *</label>
                    <select id="profile-gender" className="form-select" value={form.gender} onChange={(e) => updateFormField('gender', e.target.value)} aria-invalid={Boolean(fieldErrors.gender)}>
                      <option value="">Select gender</option>
                      {GENDERS.map((gender) => <option key={gender} value={gender}>{gender}</option>)}
                    </select>
                    <FieldError message={fieldErrors.gender} />
                  </div>
                  <div className="form-group">
                    <label className="form-label" htmlFor="profile-joining-date">Joining date *</label>
                    <input id="profile-joining-date" className="form-input" type="date" max={todayDate()} min={form.dateOfBirth || undefined} value={form.joiningDate} onChange={(e) => updateFormField('joiningDate', e.target.value)} aria-invalid={Boolean(fieldErrors.joiningDate)} />
                    <FieldError message={fieldErrors.joiningDate} />
                  </div>
                  <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                    <label className="form-label" htmlFor="profile-residential-address">Residential address</label>
                    <textarea id="profile-residential-address" className="form-input" rows={2} maxLength={1000} value={form.residentialAddress} onChange={(e) => updateFormField('residentialAddress', e.target.value)} />
                  </div>
                  <div className="form-group">
                    <label className="form-label" htmlFor="profile-employment-status">Employment status *</label>
                    <select id="profile-employment-status" className="form-select" value={form.employmentStatus} onChange={(e) => updateFormField('employmentStatus', e.target.value)} aria-invalid={Boolean(fieldErrors.employmentStatus)}>
                      <option value="">Select status</option>
                      {EMPLOYMENT_STATUSES.map((status) => <option key={status} value={status}>{status}</option>)}
                    </select>
                    <FieldError message={fieldErrors.employmentStatus} />
                  </div>
                </div>
                <h4 style={{ fontWeight: 700, margin: '8px 0 12px' }}>Professional Information</h4>
                <div className="grid-2">
                  <div className="form-group">
                    <label className="form-label" htmlFor="profile-designation">Academic Designation *</label>
                    <select id="profile-designation" className="form-select" value={form.designation} onChange={(e) => updateFormField('designation', e.target.value)} aria-invalid={Boolean(fieldErrors.designation)}>
                      <option value="">Select designation</option>
                      {ACADEMIC_DESIGNATIONS.map((designation) => <option key={designation} value={designation}>{designation}</option>)}
                    </select>
                    <FieldError message={fieldErrors.designation} />
                  </div>
                  <div className="form-group">
                    <label className="form-label" htmlFor="profile-qualification">Qualification *</label>
                    <input id="profile-qualification" className="form-input" value={form.qualification} onChange={(e) => updateFormField('qualification', e.target.value)} aria-invalid={Boolean(fieldErrors.qualification)} />
                    <FieldError message={fieldErrors.qualification} />
                  </div>
                  <div className="form-group">
                    <label className="form-label" htmlFor="profile-specialization">Specialization</label>
                    <input id="profile-specialization" className="form-input" value={form.specialization} onChange={(e) => updateFormField('specialization', e.target.value)} />
                  </div>
                  <div className="form-group">
                    <label className="form-label" htmlFor="profile-years-of-experience">Years of experience</label>
                    <input id="profile-years-of-experience" className="form-input" type="number" min="0" step="any" value={form.yearsOfExperience} onChange={(e) => updateFormField('yearsOfExperience', e.target.value)} aria-invalid={Boolean(fieldErrors.yearsOfExperience)} />
                    <FieldError message={fieldErrors.yearsOfExperience} />
                  </div>
                  <div className="form-group">
                    <label className="form-label" htmlFor="profile-employment-type">Employment type *</label>
                    <select id="profile-employment-type" className="form-select" value={form.employmentType} onChange={(e) => updateFormField('employmentType', e.target.value)} aria-invalid={Boolean(fieldErrors.employmentType)}>
                      <option value="">Select employment type</option>
                      {EMPLOYMENT_TYPES.map((type) => <option key={type} value={type}>{type}</option>)}
                    </select>
                    <FieldError message={fieldErrors.employmentType} />
                  </div>
                </div>
              </>
            )}
            {p?.role === 'STUDENT' && (
              <>
                <h4 style={{ fontWeight: 700, margin: '20px 0 12px' }}>Personal Information</h4>
                <div className="grid-2">
                  <div className="form-group">
                    <label className="form-label" htmlFor="student-phone">Phone</label>
                    <input
                      id="student-phone"
                      className="form-input"
                      type="tel"
                      inputMode="numeric"
                      maxLength={10}
                      value={form.phone}
                      onChange={(e) => updateFormField('phone', e.target.value.replace(/\D/g, '').slice(0, 10))}
                      aria-invalid={Boolean(fieldErrors.phone)}
                    />
                    <FieldError message={fieldErrors.phone} />
                  </div>
                  <div className="form-group">
                    <label className="form-label" htmlFor="student-date-of-birth">Date of birth</label>
                    <input
                      id="student-date-of-birth"
                      className="form-input"
                      type="date"
                      max={todayDate()}
                      value={form.dateOfBirth}
                      onChange={(e) => updateFormField('dateOfBirth', e.target.value)}
                      aria-invalid={Boolean(fieldErrors.dateOfBirth)}
                    />
                    <FieldError message={fieldErrors.dateOfBirth} />
                  </div>
                  <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                    <label className="form-label" htmlFor="student-address">Address</label>
                    <textarea
                      id="student-address"
                      className="form-input"
                      rows={2}
                      maxLength={1000}
                      value={form.residentialAddress}
                      onChange={(e) => updateFormField('residentialAddress', e.target.value)}
                    />
                  </div>
                </div>
                <h4 style={{ fontWeight: 700, margin: '20px 0 12px' }}>Academic Information</h4>
                <div className="grid-2">
                  <div className="form-group">
                    <label className="form-label" htmlFor="student-registration-number">Registration Number</label>
                    <input
                      id="student-registration-number"
                      className="form-input"
                      maxLength={50}
                      value={form.registrationNumber}
                      onChange={(e) => updateFormField('registrationNumber', e.target.value)}
                      aria-invalid={Boolean(fieldErrors.registrationNumber)}
                    />
                    <FieldError message={fieldErrors.registrationNumber} />
                  </div>
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
                      aria-invalid={Boolean(fieldErrors.semester)}
                    >
                      <option value="">Select semester</option>
                      {form.year && SEMESTERS_BY_YEAR[form.year]?.map((semester) => (
                        <option key={semester.value} value={semester.value}>{semester.label}</option>
                      ))}
                    </select>
                    <FieldError message={fieldErrors.semester} />
                  </div>
                </div>
                {form.semester && Number(form.semester) > 1 && (
                  <div style={{ margin: '8px 0 16px' }}>
                    <div className="form-label" style={{ marginBottom: 8 }}>Previous Semester SGPA (0–10)</div>
                    <div className="grid-2">
                      {Array.from({ length: Number(form.semester) - 1 }, (_, index) => index + 1).map((semester) => (
                        <div className="form-group" key={semester}>
                          <label className="form-label" htmlFor={`student-sgpa-${semester}`}>Semester {semester} SGPA</label>
                          <input
                            id={`student-sgpa-${semester}`}
                            className="form-input"
                            type="number"
                            min="0"
                            max="10"
                            step="0.01"
                            value={form.semesterSgpas[semester] || ''}
                            onChange={(e) => updateSemesterSgpa(semester, e.target.value)}
                            aria-invalid={Boolean(fieldErrors[`sgpa-${semester}`])}
                          />
                          <FieldError message={fieldErrors[`sgpa-${semester}`]} />
                        </div>
                      ))}
                    </div>
                  </div>
                )}
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
          <InfoRow icon={<Hash size={18} />}      label={p?.role === 'FACULTY' ? 'Faculty ID' : 'Roll Number'} value={p?.rollNumber} />
          <InfoRow icon={<Mail size={18} />}      label="Email"       value={p?.email} />
          <InfoRow icon={<Shield size={18} />}    label="Role"        value={p?.role} />
          <InfoRow
            icon={<Building2 size={18} />}
            label="Department"
            value={p?.role === 'FACULTY'
              ? (p?.departments?.map((department) => department.name).join(', ') || p?.department?.name)
              : p?.department?.name}
          />
          {(p?.role === 'FACULTY' || p?.role === 'HOD') && (
            <>
              <InfoRow icon={<Shield size={18} />} label="Account status" value={p?.status} />
              <InfoRow icon={<Shield size={18} />} label="Account created" value={p?.createdAt ? new Date(p.createdAt).toLocaleString() : null} />
              <InfoRow icon={<Mail size={18} />} label="Phone" value={p?.phone} />
              <InfoRow icon={<User size={18} />} label="Date of birth" value={p?.dateOfBirth} />
              <InfoRow icon={<User size={18} />} label="Gender" value={p?.gender} />
              <InfoRow icon={<Building2 size={18} />} label="Residential address" value={p?.residentialAddress} />
              <InfoRow icon={<Building2 size={18} />} label="Joining date" value={p?.joiningDate} />
              <InfoRow icon={<Building2 size={18} />} label="Employment status" value={p?.employmentStatus} />
              <InfoRow icon={<Shield size={18} />} label="Academic Designation" value={p?.designation} />
              <InfoRow icon={<Shield size={18} />} label="Administrative Role" value={p?.administrativeRole} />
              <InfoRow icon={<Shield size={18} />} label="Qualification" value={p?.qualification} />
              <InfoRow icon={<Shield size={18} />} label="Specialization" value={p?.specialization} />
              <InfoRow icon={<Shield size={18} />} label="Years of experience" value={p?.yearsOfExperience == null ? null : `${p.yearsOfExperience} years`} />
              <InfoRow icon={<Shield size={18} />} label="Employment type" value={p?.employmentType} />
            </>
          )}
          {p?.role === 'STUDENT' && (
            <>
              <InfoRow icon={<Hash size={18} />} label="Registration Number" value={p?.registrationNumber} />
              <InfoRow icon={<Mail size={18} />} label="Phone" value={p?.phone} />
              <InfoRow icon={<User size={18} />} label="Date of birth" value={p?.dateOfBirth} />
              <InfoRow icon={<Building2 size={18} />} label="Address" value={p?.residentialAddress} />
              <InfoRow icon={<BookOpen size={18} />} label="Academic Year" value={p?.year ? `Year ${p.year}` : null} />
              <InfoRow icon={<BookOpen size={18} />} label="Semester" value={p?.semester ? `Semester ${p.semester}` : null} />
              <InfoRow
                icon={<BookOpen size={18} />}
                label="Enrolled Subjects"
                value={p?.selectedSubjects?.map((subject) => subject.name).join(', ')}
              />
              <InfoRow
                icon={<BookOpen size={18} />}
                label="Semester SGPAs"
                value={Object.entries(p?.semesterSgpas || {}).sort(([a], [b]) => Number(a) - Number(b))
                  .map(([semester, sgpa]) => `Semester ${semester}: ${sgpa}`).join(', ')}
              />
              <InfoRow
                icon={<BookOpen size={18} />}
                label="Yearly YGPAs"
                value={Object.entries(p?.yearlyGpas || {}).sort(([a], [b]) => Number(a) - Number(b))
                  .map(([year, ygpa]) => `Year ${year}: ${ygpa}`).join(', ')}
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
