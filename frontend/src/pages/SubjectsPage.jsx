import { useEffect, useState, useCallback } from 'react';
import Layout from '../components/Layout';
import { useAuth } from '../context/AuthContext';
import { getAllSubjects, createSubject, updateSubject, deleteSubject } from '../api/subjects';
import { getAllDepartments } from '../api/departments';
import { getAllUsers } from '../api/users';
import { Plus, Pencil, Trash2, BookOpen, X, Search } from 'lucide-react';
import toast from 'react-hot-toast';

function SubjectModal({ open, onClose, onSaved, departments, facultyList, editSubject }) {
  const empty = { name: '', code: '', credits: '', departmentId: '', facultyId: '' };
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
      });
    } else {
      setForm(empty);
    }
    setError('');
  }, [editSubject, open]);

  if (!open) return null;

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

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
  const { user, isAdminOrHod, isFaculty } = useAuth();
  const [subjects, setSubjects] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [facultyList, setFacultyList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editSubject, setEditSubject] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [subRes, depRes, userRes] = await Promise.all([
        getAllSubjects(),
        getAllDepartments(),
        getAllUsers(),
      ]);
      setSubjects(subRes.data);
      setDepartments(depRes.data);
      setFacultyList(userRes.data.filter((u) => u.role === 'FACULTY'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  // Faculty only see their own subjects
  const visibleSubjects = isFaculty
    ? subjects.filter((s) => s.faculty?.id === user.id)
    : subjects;

  const filtered = visibleSubjects.filter((s) => {
    const q = search.toLowerCase();
    return !q || s.name?.toLowerCase().includes(q) || s.code?.toLowerCase().includes(q) || s.department?.name?.toLowerCase().includes(q);
  });

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
            {isFaculty ? `${filtered.length} subjects assigned to you` : `${subjects.length} total subjects`}
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
                {filtered.map((s) => (
                  <tr key={s.id}>
                    <td style={{ fontWeight: 600 }}>{s.name}</td>
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
