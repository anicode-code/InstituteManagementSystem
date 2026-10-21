import { useEffect, useState, useCallback } from 'react';
import Layout from '../components/Layout';
import { useAuth } from '../context/AuthContext';
import {
  getAllAttendance, getAttendanceByStudent,
  getAttendanceBySubject, createAttendance, updateAttendance,
  getAttendancePercentage,
} from '../api/attendance';
import { getAllSubjects, getSubjectsByFaculty } from '../api/subjects';
import { getAllUsers } from '../api/users';
import { CalendarCheck, Plus, Pencil, X, Search, TrendingUp } from 'lucide-react';
import toast from 'react-hot-toast';

function MarkAttendanceModal({ open, onClose, onSaved, subjects, students }) {
  const empty = { studentId: '', subjectId: '', date: new Date().toISOString().split('T')[0], status: 'PRESENT' };
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
    if (!form.studentId || !form.subjectId || !form.date) {
      setError('Student, subject, and date are required.');
      return;
    }
    setLoading(true);
    try {
      await createAttendance(form);
      toast.success('Attendance marked');
      onSaved();
      onClose();
    } catch (err) {
      setError(err?.response?.data || 'Error marking attendance');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3 className="modal-title">Mark Attendance</h3>
          <button className="btn btn-ghost btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}
            <div className="form-group">
              <label className="form-label">Student *</label>
              <select className="form-select" value={form.studentId} onChange={(e) => set('studentId', e.target.value)}>
                <option value="">Select Student</option>
                {students.map((s) => <option key={s.id} value={s.id}>{s.name} ({s.rollNumber})</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Subject *</label>
              <select className="form-select" value={form.subjectId} onChange={(e) => set('subjectId', e.target.value)}>
                <option value="">Select Subject</option>
                {subjects.map((s) => <option key={s.id} value={s.id}>{s.name} ({s.code})</option>)}
              </select>
            </div>
            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">Date *</label>
                <input className="form-input" type="date" value={form.date} onChange={(e) => set('date', e.target.value)} />
              </div>
              <div className="form-group">
                <label className="form-label">Status *</label>
                <div style={{ display: 'flex', gap: 8, marginTop: 6 }}>
                  {['PRESENT', 'ABSENT'].map((s) => (
                    <button
                      key={s}
                      type="button"
                      onClick={() => set('status', s)}
                      className="btn"
                      style={{
                        flex: 1,
                        background: form.status === s
                          ? (s === 'PRESENT' ? 'var(--success)' : 'var(--danger)')
                          : 'var(--bg-hover)',
                        color: form.status === s ? '#fff' : 'var(--text-secondary)',
                        border: `1px solid ${form.status === s ? (s === 'PRESENT' ? 'var(--success)' : 'var(--danger)') : 'var(--border)'}`,
                        justifyContent: 'center',
                      }}
                    >
                      {s}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? <span className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} /> : 'Mark Attendance'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function UpdateStatusModal({ open, onClose, onSaved, record }) {
  const [status, setStatus] = useState('PRESENT');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (record) setStatus(record.status);
  }, [record]);

  if (!open || !record) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await updateAttendance(record.id, status);
      toast.success('Attendance updated');
      onSaved();
      onClose();
    } catch (err) {
      toast.error('Update failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" style={{ maxWidth: 360 }} onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3 className="modal-title">Update Status</h3>
          <button className="btn btn-ghost btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: 12 }}>
              {record.student?.name} · {record.subject?.name} · {record.date}
            </p>
            <div style={{ display: 'flex', gap: 8 }}>
              {['PRESENT', 'ABSENT'].map((s) => (
                <button
                  key={s}
                  type="button"
                  onClick={() => setStatus(s)}
                  className="btn"
                  style={{
                    flex: 1,
                    background: status === s ? (s === 'PRESENT' ? 'var(--success)' : 'var(--danger)') : 'var(--bg-hover)',
                    color: status === s ? '#fff' : 'var(--text-secondary)',
                    border: `1px solid ${status === s ? (s === 'PRESENT' ? 'var(--success)' : 'var(--danger)') : 'var(--border)'}`,
                    justifyContent: 'center',
                  }}
                >
                  {s}
                </button>
              ))}
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? <span className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} /> : 'Update'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function AttendancePage() {
  const { user, isFaculty, isStudent, isAdminOrHod } = useAuth();
  const [records, setRecords] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [markModal, setMarkModal] = useState(false);
  const [updateModal, setUpdateModal] = useState(false);
  const [selectedRecord, setSelectedRecord] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const tasks = [
        isStudent ? getAttendanceByStudent(user.id) : getAllAttendance(),
        isFaculty ? getSubjectsByFaculty(user.id) : getAllSubjects(),
        isAdminOrHod || isFaculty ? getAllUsers() : Promise.resolve({ data: [] }),
      ];
      const [aRes, sRes, uRes] = await Promise.all(tasks);
      setRecords(aRes.data);
      setSubjects(sRes.data);
      setStudents(uRes.data.filter((u) => u.role === 'STUDENT'));
    } finally {
      setLoading(false);
    }
  }, [user, isFaculty, isStudent, isAdminOrHod]);

  useEffect(() => { load(); }, [load]);

  const filtered = records.filter((r) => {
    const matchStatus = statusFilter === 'ALL' || r.status === statusFilter;
    const q = search.toLowerCase();
    const matchSearch = !q ||
      r.student?.name?.toLowerCase().includes(q) ||
      r.subject?.name?.toLowerCase().includes(q) ||
      r.date?.includes(q);
    return matchStatus && matchSearch;
  });

  const presentCount = records.filter((r) => r.status === 'PRESENT').length;
  const pct = records.length ? Math.round((presentCount / records.length) * 100) : 0;

  return (
    <Layout pageTitle="Attendance">
      <div className="page-header">
        <div>
          <h2 className="page-title">Attendance</h2>
          <p className="page-subtitle">
            {isStudent ? 'Your attendance records' : `${records.length} total records`}
          </p>
        </div>
        {(isFaculty || isAdminOrHod) && (
          <button className="btn btn-primary" onClick={() => setMarkModal(true)}>
            <Plus size={18} /> Mark Attendance
          </button>
        )}
      </div>

      {/* Summary */}
      {records.length > 0 && (
        <div className="stats-grid" style={{ marginBottom: 20 }}>
          <div className="stat-card">
            <div className="stat-icon" style={{ background: 'var(--accent-light)' }}>
              <CalendarCheck size={22} color="var(--accent)" />
            </div>
            <div>
              <div className="stat-value">{records.length}</div>
              <div className="stat-label">Total Records</div>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon" style={{ background: 'var(--success-light)' }}>
              <TrendingUp size={22} color="var(--success)" />
            </div>
            <div>
              <div className="stat-value">{pct}%</div>
              <div className="stat-label">Attendance Rate</div>
              <div className="progress-bar" style={{ width: 100, marginTop: 6 }}>
                <div
                  className={`progress-fill ${pct >= 75 ? 'progress-high' : pct >= 50 ? 'progress-medium' : 'progress-low'}`}
                  style={{ width: `${pct}%` }}
                />
              </div>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon" style={{ background: 'var(--success-light)' }}>
              <span style={{ color: 'var(--success)', fontWeight: 800, fontSize: '1.1rem' }}>P</span>
            </div>
            <div>
              <div className="stat-value">{presentCount}</div>
              <div className="stat-label">Present</div>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon" style={{ background: 'var(--danger-light)' }}>
              <span style={{ color: 'var(--danger)', fontWeight: 800, fontSize: '1.1rem' }}>A</span>
            </div>
            <div>
              <div className="stat-value">{records.length - presentCount}</div>
              <div className="stat-label">Absent</div>
            </div>
          </div>
        </div>
      )}

      <div className="toolbar">
        <div className="toolbar-left">
          <div className="search-bar">
            <Search />
            <input placeholder="Search by student, subject or date..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
          <div className="filter-tabs">
            {['ALL', 'PRESENT', 'ABSENT'].map((s) => (
              <button key={s} className={`filter-tab ${statusFilter === s ? 'active' : ''}`} onClick={() => setStatusFilter(s)}>
                {s === 'ALL' ? 'All' : s.charAt(0) + s.slice(1).toLowerCase()}
              </button>
            ))}
          </div>
        </div>
      </div>

      {loading ? (
        <div className="loader"><div className="spinner" /></div>
      ) : filtered.length === 0 ? (
        <div className="empty-state">
          <CalendarCheck />
          <h3>No attendance records</h3>
          <p>{records.length === 0 ? 'No attendance has been marked yet.' : 'No records match your filters.'}</p>
        </div>
      ) : (
        <div className="card" style={{ padding: 0 }}>
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  {!isStudent && <th>Student</th>}
                  <th>Subject</th>
                  <th>Date</th>
                  <th>Status</th>
                  {(isFaculty || isAdminOrHod) && <th>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {filtered.map((r) => (
                  <tr key={r.id}>
                    {!isStudent && (
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                          <div className="avatar sm" style={{ background: 'var(--success-light)', color: 'var(--success)', fontSize: '0.65rem' }}>
                            {r.student?.name?.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2)}
                          </div>
                          <span style={{ fontWeight: 600 }}>{r.student?.name}</span>
                        </div>
                      </td>
                    )}
                    <td style={{ color: 'var(--text-secondary)' }}>{r.subject?.name}</td>
                    <td style={{ color: 'var(--text-secondary)' }}>{r.date}</td>
                    <td>
                      <span className={`badge ${r.status === 'PRESENT' ? 'badge-present' : 'badge-absent'}`}>
                        {r.status}
                      </span>
                    </td>
                    {(isFaculty || isAdminOrHod) && (
                      <td>
                        <button
                          className="btn btn-ghost btn-icon btn-sm"
                          title="Edit"
                          onClick={() => { setSelectedRecord(r); setUpdateModal(true); }}
                        >
                          <Pencil size={14} />
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <MarkAttendanceModal
        open={markModal}
        onClose={() => setMarkModal(false)}
        onSaved={load}
        subjects={subjects}
        students={students}
      />
      <UpdateStatusModal
        open={updateModal}
        onClose={() => setUpdateModal(false)}
        onSaved={load}
        record={selectedRecord}
      />
    </Layout>
  );
}
