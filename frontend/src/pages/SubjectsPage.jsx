import { Fragment, useEffect, useState, useCallback } from 'react';
import Layout from '../components/Layout';
import { useAuth } from '../context/AuthContext';
import { getAllSubjects, getSubjectsByStudent, createSubject, updateSubject, deleteSubject } from '../api/subjects';
import { getAllDepartments } from '../api/departments';
import { getAllUsers } from '../api/users';
import { Plus, Pencil, Trash2, BookOpen, X, Search } from 'lucide-react';
import toast from 'react-hot-toast';

const semestersByYear = {
  1: [1, 2],
  2: [3, 4],
  3: [5, 6],
  4: [7, 8],
};

function SubjectModal({ open, onClose, onSaved, departments, facultyList, editSubject }) {
  const empty = {
    name: '', code: '', credits: '', departmentId: '', facultyId: '',
    academicYear: 1, semester: 1, type: 'COMPULSORY',
  };
  const [form, setForm] = useState(empty);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (editSubject) {
      setForm({
        name: editSubject.name || '',
        code: editSubject.code || '',
        credits: editSubject.credits ?? '',
        departmentId: editSubject.department?.id || '',
        facultyId: editSubject.faculty?.id || '',
        academicYear: editSubject.academicYear || 1,
        semester: editSubject.semester || 1,
        type: editSubject.type || 'COMPULSORY',
      });
    } else {
      setForm(empty);
    }
    setError('');
  }, [editSubject, open]);

  if (!open) return null;

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));
  const formSemesters = semestersByYear[Number(form.academicYear)] || [];

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.name || !form.code) { setError('Name and code are required.'); return; }
    setLoading(true);
    try {
      if (editSubject) {
        await updateSubject(editSubject.id, {
          name: form.name,
          code: form.code,
          credits: form.credits ? Number(form.credits) : null,
          academicYear: Number(form.academicYear),
          semester: Number(form.semester),
          type: form.type,
        });
        toast.success('Subject updated');
      } else {
        if (!form.departmentId || !form.facultyId) { setError('Department and faculty are required.'); setLoading(false); return; }
        await createSubject({
          name: form.name,
          code: form.code,
          credits: form.credits ? Number(form.credits) : null,
          departmentId: Number(form.departmentId),
          facultyId: Number(form.facultyId),
          academicYear: Number(form.academicYear),
          semester: Number(form.semester),
          type: form.type,
        });
        toast.success('Subject created');
      }
      onSaved();
      onClose();
    } catch (err) {
      setError(err?.response?.data || err?.message || 'Error saving subject');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3 className="modal-title">{editSubject ? 'Edit Subject' : 'Add Subject'}</h3>
          <button className="btn btn-ghost btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}
            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">Subject Name *</label>
                <input className="form-input" value={form.name} onChange={(e) => set('name', e.target.value)} placeholder="Data Structures" />
              </div>
              <div className="form-group">
                <label className="form-label">Subject Code *</label>
                <input className="form-input" value={form.code} onChange={(e) => set('code', e.target.value.toUpperCase())} placeholder="CS301" />
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">Credits</label>
              <input className="form-input" type="number" min={1} max={10} value={form.credits} onChange={(e) => set('credits', e.target.value)} placeholder="e.g. 4" />
            </div>
            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">Year *</label>
                <select className="form-select" value={form.academicYear} onChange={(e) => {
                  const academicYear = Number(e.target.value);
                  setForm((current) => ({
                    ...current,
                    academicYear,
                    semester: semestersByYear[academicYear][0],
                  }));
                }}>
                  {[1, 2, 3, 4].map((year) => <option key={year} value={year}>Year {year}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Semester *</label>
                <select className="form-select" value={form.semester} onChange={(e) => set('semester', Number(e.target.value))}>
                  {formSemesters.map((semester) => <option key={semester} value={semester}>Semester {semester}</option>)}
                </select>
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">Subject Type *</label>
              <select className="form-select" value={form.type} onChange={(e) => set('type', e.target.value)}>
                <option value="COMPULSORY">Compulsory</option>
                <option value="OPTIONAL">Optional</option>
              </select>
            </div>
            {!editSubject && (
              <div className="grid-2">
                <div className="form-group">
                  <label className="form-label">Department *</label>
                  <select className="form-select" value={form.departmentId} onChange={(e) => set('departmentId', e.target.value)}>
                    <option value="">Select Department</option>
                    {departments.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Faculty *</label>
                  <select className="form-select" value={form.facultyId} onChange={(e) => set('facultyId', e.target.value)}>
                    <option value="">Select Faculty</option>
                    {facultyList.map((f) => <option key={f.id} value={f.id}>{f.name}</option>)}
                  </select>
                </div>
              </div>
            )}
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? <span className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} /> : (editSubject ? 'Update' : 'Create')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function SubjectsPage() {
  const { user, isAdmin, isAdminOrHod, isFaculty, isStudent } = useAuth();
  const [subjects, setSubjects] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [facultyList, setFacultyList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editSubject, setEditSubject] = useState(null);
  const [departmentFilter, setDepartmentFilter] = useState('');
  const [yearFilter, setYearFilter] = useState('');
  const [semesterFilter, setSemesterFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [subRes, depRes, userRes] = await Promise.all([
        isStudent ? getSubjectsByStudent(user.id) : getAllSubjects(),
        getAllDepartments(),
        getAllUsers(),
      ]);
      setSubjects(subRes.data);
      setDepartments(depRes.data);
      setFacultyList(userRes.data.filter((u) => u.role === 'FACULTY'));
    } finally {
      setLoading(false);
    }
  }, [user, isStudent]);

  useEffect(() => { load(); }, [load]);

  // Faculty only see their own subjects
  const visibleSubjects = isFaculty
    ? subjects.filter((s) => s.faculty?.id === user.id)
    : subjects;

  const filtered = visibleSubjects.filter((s) => {
    const q = search.toLowerCase();
    const year = s.academicYear || (s.semester ? Math.ceil(s.semester / 2) : null);
    return (!q || s.name?.toLowerCase().includes(q) || s.code?.toLowerCase().includes(q) || s.department?.name?.toLowerCase().includes(q))
      && (!isAdmin || !departmentFilter || String(s.department?.id) === departmentFilter)
      && (!yearFilter || String(year) === yearFilter)
      && (!semesterFilter || String(s.semester) === semesterFilter)
      && (!typeFilter || s.type === typeFilter);
  });

  const years = [...new Set(visibleSubjects.map((s) => s.academicYear || (s.semester ? Math.ceil(s.semester / 2) : null)).filter(Boolean))].sort((a, b) => a - b);
  const semesters = yearFilter
    ? semestersByYear[Number(yearFilter)]
    : [1, 2, 3, 4, 5, 6, 7, 8];
  const groupedSubjects = (() => {
    const groups = new Map();
    filtered.forEach((subject) => {
      const year = subject.academicYear || (subject.semester ? Math.ceil(subject.semester / 2) : 'Unspecified year');
      const semester = subject.semester || 'Unspecified semester';
      const department = isAdmin ? (subject.department?.name || 'Unassigned department') : '';
      const key = [department, year, semester, subject.type || 'COMPULSORY'].join('|');
      if (!groups.has(key)) {
        groups.set(key, { department, year, semester, type: subject.type || 'COMPULSORY', subjects: [] });
      }
      groups.get(key).subjects.push(subject);
    });
    return [...groups.values()].sort((a, b) =>
      a.department.localeCompare(b.department)
      || String(a.year).localeCompare(String(b.year), undefined, { numeric: true })
      || String(a.semester).localeCompare(String(b.semester), undefined, { numeric: true })
      || a.type.localeCompare(b.type)
    );
  })();

  const handleDelete = async (s) => {
    if (!window.confirm(`Delete subject "${s.name}"?`)) return;
    try {
      await deleteSubject(s.id);
      toast.success('Subject deleted');
      load();
    } catch (err) {
      toast.error('Delete failed');
    }
  };

  return (
    <Layout pageTitle="Subjects">
      <div className="page-header">
        <div>
          <h2 className="page-title">Subjects</h2>
          <p className="page-subtitle">
            {isFaculty
              ? `${filtered.length} subjects assigned to you`
              : (isStudent
                ? `${filtered.length} subjects enrolled / chosen`
                : (isAdminOrHod ? `${filtered.length} subjects shown` : `${subjects.length} total subjects`))}
          </p>
        </div>
        {isAdminOrHod && (
          <button className="btn btn-primary" onClick={() => { setEditSubject(null); setModalOpen(true); }}>
            <Plus size={18} /> Add Subject
          </button>
        )}
      </div>

      <div className="toolbar">
        <div className="toolbar-left">
          <div className="search-bar">
            <Search />
            <input placeholder="Search subjects..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
          {isAdminOrHod && (
            <div className="role-filter-row">
              {isAdmin && (
                <select className="form-select" aria-label="Filter by department" value={departmentFilter} onChange={(e) => setDepartmentFilter(e.target.value)}>
                  <option value="">All Departments</option>
                  {departments.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
                </select>
              )}
              <select className="form-select" aria-label="Filter by year" value={yearFilter} onChange={(e) => {
                const nextYear = e.target.value;
                setYearFilter(nextYear);
                if (semesterFilter && nextYear && !semestersByYear[Number(nextYear)].includes(Number(semesterFilter))) {
                  setSemesterFilter('');
                }
              }}>
                <option value="">All Years</option>
                {years.map((year) => <option key={year} value={year}>Year {year}</option>)}
              </select>
              <select className="form-select" aria-label="Filter by semester" value={semesterFilter} onChange={(e) => setSemesterFilter(e.target.value)}>
                <option value="">All Semesters</option>
                {semesters.map((semester) => <option key={semester} value={semester}>Semester {semester}</option>)}
              </select>
              <select className="form-select" aria-label="Filter by subject type" value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}>
                <option value="">All Types</option>
                <option value="COMPULSORY">Compulsory</option>
                <option value="OPTIONAL">Optional</option>
              </select>
            </div>
          )}
        </div>
      </div>

      {loading ? (
        <div className="loader"><div className="spinner" /></div>
      ) : filtered.length === 0 ? (
        <div className="empty-state">
          <BookOpen />
          <h3>No subjects found</h3>
          <p>{subjects.length === 0 ? 'No subjects have been created yet.' : 'Try adjusting your search.'}</p>
        </div>
      ) : (
        <div className="card" style={{ padding: 0 }}>
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Subject</th>
                  <th>Code</th>
                  <th>Credits</th>
                  <th>Department</th>
                  <th>Faculty</th>
                  {isAdminOrHod && <th>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {groupedSubjects.map((group) => (
                  <Fragment key={`${group.department}-${group.year}-${group.semester}-${group.type}`}>
                    <tr>
                      <td colSpan={isAdminOrHod ? 6 : 5} style={{ background: 'var(--bg-secondary)', fontWeight: 700, color: 'var(--text-secondary)' }}>
                        {[
                          group.department && `Department: ${group.department}`,
                          group.year && `Year ${group.year}`,
                          group.semester && `Semester ${group.semester}`,
                          group.type === 'OPTIONAL' ? 'Optional' : 'Compulsory',
                        ].filter(Boolean).join(' · ')}
                      </td>
                    </tr>
                    {group.subjects.map((s) => (
                      <tr key={s.id}>
                        <td style={{ fontWeight: 600 }}>
                          {s.name}
                          <span style={{
                            marginLeft: 8, padding: '2px 7px', borderRadius: 10, fontSize: '0.7rem', fontWeight: 700,
                            background: s.type === 'OPTIONAL' ? 'var(--warning-light)' : 'var(--success-light)',
                            color: s.type === 'OPTIONAL' ? 'var(--warning)' : 'var(--success)',
                          }}>{s.type === 'OPTIONAL' ? 'Optional' : 'Compulsory'}</span>
                        </td>
                        <td>
                          <span style={{ background: 'var(--bg-hover)', padding: '2px 8px', borderRadius: 4, fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)' }}>
                            {s.code}
                          </span>
                        </td>
                        <td style={{ color: 'var(--text-secondary)' }}>{s.credits ?? '—'}</td>
                        <td style={{ color: 'var(--text-secondary)' }}>{s.department?.name || '—'}</td>
                        <td>
                          {s.faculty ? (
                            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                              <div className="avatar sm" style={{ background: 'var(--warning-light)', color: 'var(--warning)', fontSize: '0.65rem' }}>
                                {s.faculty.name?.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2)}
                              </div>
                              <span style={{ fontSize: '0.875rem' }}>{s.faculty.name}</span>
                            </div>
                          ) : '—'}
                        </td>
                        {isAdminOrHod && (
                      <td>
                        <div style={{ display: 'flex', gap: 6 }}>
                          <button className="btn btn-ghost btn-icon btn-sm" onClick={() => { setEditSubject(s); setModalOpen(true); }}>
                            <Pencil size={14} />
                          </button>
                          <button className="btn btn-ghost btn-icon btn-sm" style={{ color: 'var(--danger)' }} onClick={() => handleDelete(s)}>
                            <Trash2 size={14} />
                          </button>
                        </div>
                      </td>
                        )}
                      </tr>
                    ))}
                  </Fragment>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <SubjectModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSaved={load}
        departments={departments}
        facultyList={facultyList}
        editSubject={editSubject}
      />
    </Layout>
  );
}
