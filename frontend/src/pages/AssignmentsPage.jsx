import { useEffect, useState, useCallback } from 'react';
import Layout from '../components/Layout';
import { useAuth } from '../context/AuthContext';
import {
  getAllAssignments, getAssignmentsByFaculty,
  createAssignment, deleteAssignment,
} from '../api/assignments';
import { getAllSubjects, getSubjectsByFaculty } from '../api/subjects';
import { ClipboardList, Plus, Trash2, X, Search, Calendar, BookOpen } from 'lucide-react';
import toast from 'react-hot-toast';

function AssignmentModal({ open, onClose, onSaved, subjects, facultyId }) {
  const empty = { title: '', description: '', dueDate: '', subjectId: '' };
  const [form, setForm] = useState(empty);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setForm(empty);
    setError('');
  }, [open]);

  if (!open) return null;
  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.title || !form.dueDate || !form.subjectId) {
      setError('Title, due date, and subject are required.');
      return;
    }
    setLoading(true);
    try {
      await createAssignment({
        title: form.title,
        description: form.description,
        dueDate: form.dueDate,
        subjectId: Number(form.subjectId),
        facultyId: Number(facultyId),
      });
      toast.success('Assignment created');
      onSaved();
      onClose();
    } catch (err) {
      setError(err?.response?.data || err?.message || 'Error creating assignment');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3 className="modal-title">Create Assignment</h3>
          <button className="btn btn-ghost btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}
            <div className="form-group">
              <label className="form-label">Title *</label>
              <input className="form-input" value={form.title} onChange={(e) => set('title', e.target.value)} placeholder="Assignment title" />
            </div>
            <div className="form-group">
              <label className="form-label">Description</label>
              <textarea className="form-textarea" value={form.description} onChange={(e) => set('description', e.target.value)} placeholder="Assignment description..." />
            </div>
            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">Subject *</label>
                <select className="form-select" value={form.subjectId} onChange={(e) => set('subjectId', e.target.value)}>
                  <option value="">Select Subject</option>
                  {subjects.map((s) => <option key={s.id} value={s.id}>{s.name} ({s.code})</option>)}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Due Date *</label>
                <input className="form-input" type="date" value={form.dueDate} onChange={(e) => set('dueDate', e.target.value)} min={new Date().toISOString().split('T')[0]} />
              </div>
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? <span className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} /> : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function AssignmentsPage() {
  const { user, isFaculty, isStudent } = useAuth();
  const [assignments, setAssignments] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [aRes, sRes] = await Promise.all([
        isFaculty ? getAssignmentsByFaculty(user.id) : getAllAssignments(),
        isFaculty ? getSubjectsByFaculty(user.id) : getAllSubjects(),
      ]);
      setAssignments(aRes.data);
      setSubjects(sRes.data);
    } finally {
      setLoading(false);
    }
  }, [user, isFaculty]);

  useEffect(() => { load(); }, [load]);

  const filtered = assignments.filter((a) => {
    const q = search.toLowerCase();
    return !q || a.title?.toLowerCase().includes(q) || a.subject?.name?.toLowerCase().includes(q) || a.faculty?.name?.toLowerCase().includes(q);
  });

  const handleDelete = async (a) => {
    if (!window.confirm(`Delete assignment "${a.title}"?`)) return;
    try {
      await deleteAssignment(a.id);
      toast.success('Assignment deleted');
      load();
    } catch (err) {
      toast.error('Delete failed');
    }
  };

  const isOverdue = (dateStr) => dateStr && new Date(dateStr) < new Date();

  return (
    <Layout pageTitle="Assignments">
      <div className="page-header">
        <div>
          <h2 className="page-title">Assignments</h2>
          <p className="page-subtitle">
            {isFaculty ? 'Assignments you have posted' : 'All assignments'}
          </p>
        </div>
        {isFaculty && (
          <button className="btn btn-primary" onClick={() => setModalOpen(true)}>
            <Plus size={18} /> New Assignment
          </button>
        )}
      </div>

      <div className="toolbar">
        <div className="toolbar-left">
          <div className="search-bar">
            <Search />
            <input placeholder="Search assignments..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
        </div>
        <div className="toolbar-right">
          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{filtered.length} assignments</span>
        </div>
      </div>

      {loading ? (
        <div className="loader"><div className="spinner" /></div>
      ) : filtered.length === 0 ? (
        <div className="empty-state">
          <ClipboardList />
          <h3>No assignments found</h3>
          <p>{isFaculty ? 'Create your first assignment.' : 'No assignments have been posted yet.'}</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {filtered.map((a) => {
            const overdue = isOverdue(a.dueDate);
            return (
              <div key={a.id} className="card" style={{ transition: 'all 0.2s' }}>
                <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 16 }}>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8, flexWrap: 'wrap' }}>
                      <h3 style={{ fontWeight: 700, fontSize: '1rem' }}>{a.title}</h3>
                      {a.dueDate && (
                        <span style={{
                          display: 'flex', alignItems: 'center', gap: 4,
                          padding: '2px 8px', borderRadius: 4,
                          background: overdue ? 'var(--danger-light)' : 'var(--success-light)',
                          color: overdue ? 'var(--danger)' : 'var(--success)',
                          fontSize: '0.72rem', fontWeight: 700,
                        }}>
                          <Calendar size={11} />
                          {overdue ? 'Overdue: ' : 'Due: '}{a.dueDate}
                        </span>
                      )}
                    </div>
                    {a.description && (
                      <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: 10, lineHeight: 1.5 }}>
                        {a.description}
                      </p>
                    )}
                    <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                      <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                        <BookOpen size={13} /> {a.subject?.name} ({a.subject?.code})
                      </span>
                      <span>Faculty: {a.faculty?.name}</span>
                    </div>
                  </div>
                  {isFaculty && (
                    <button
                      className="btn btn-ghost btn-icon"
                      style={{ color: 'var(--danger)', flexShrink: 0 }}
                      onClick={() => handleDelete(a)}
                      title="Delete"
                    >
                      <Trash2 size={16} />
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {isFaculty && (
        <AssignmentModal
          open={modalOpen}
          onClose={() => setModalOpen(false)}
          onSaved={load}
          subjects={subjects}
          facultyId={user.id}
        />
      )}
    </Layout>
  );
}
