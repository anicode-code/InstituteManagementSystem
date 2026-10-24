import { useEffect, useState, useCallback, useMemo } from 'react';
import Layout from '../components/Layout';
import { useAuth } from '../context/AuthContext';
import {
  getAllAssignments, getAssignmentsByFaculty,
  createAssignment, deleteAssignment,
  submitAssignment, getMySubmission,
  getSubmissionsForAssignment, evaluateSubmission,
} from '../api/assignments';
import { getAllSubjects, getSubjectsByFaculty, getSubjectsByStudent } from '../api/subjects';
import {
  ClipboardList, Plus, Trash2, X, Search, Calendar,
  BookOpen, Send, Award, CheckCircle, Clock,
} from 'lucide-react';
import toast from 'react-hot-toast';

function AssignmentModal({ open, onClose, onSaved, subjects, facultyId }) {
  const empty = { title: '', description: '', dueDate: '', maxMarks: 100, subjectId: '' };
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
        maxMarks: form.maxMarks ? Number(form.maxMarks) : 100,
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
              <textarea className="form-textarea" value={form.description} onChange={(e) => set('description', e.target.value)} placeholder="Assignment instructions, guidelines, or submission requirements..." />
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
                <label className="form-label">Maximum Marks</label>
                <input className="form-input" type="number" min="1" max="1000" value={form.maxMarks} onChange={(e) => set('maxMarks', e.target.value)} placeholder="100" />
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">Due Date *</label>
              <input className="form-input" type="date" value={form.dueDate} onChange={(e) => set('dueDate', e.target.value)} min={new Date().toISOString().split('T')[0]} />
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

// ──────── Student Submission Modal ────────
function StudentSubmissionModal({ open, onClose, assignment }) {
  const [submission, setSubmission] = useState(null);
  const [text, setText] = useState('');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!open || !assignment) return;
    setLoading(true);
    setError('');
    getMySubmission(assignment.id)
      .then((res) => {
        if (res.data) {
          setSubmission(res.data);
          setText(res.data.submissionText || '');
        } else {
          setSubmission(null);
          setText('');
        }
      })
      .catch(() => {
        setSubmission(null);
        setText('');
      })
      .finally(() => setLoading(false));
  }, [open, assignment]);

  if (!open || !assignment) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!text.trim()) {
      setError('Please provide your submission text, code link, or solution.');
      return;
    }
    setSaving(true);
    try {
      const res = await submitAssignment(assignment.id, text.trim());
      setSubmission(res.data);
      toast.success('Assignment submitted successfully!');
      onClose();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'Submission failed');
    } finally {
      setSaving(false);
    }
  };

  const isEvaluated = submission?.status === 'EVALUATED';

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" style={{ maxWidth: 520 }} onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3 className="modal-title">Submit Assignment: {assignment.title}</h3>
          <button className="btn btn-ghost btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}

            {loading ? (
              <div className="loader"><div className="spinner" /></div>
            ) : (
              <>
                {/* Evaluated Marks Banner */}
                {isEvaluated && (
                  <div className="alert" style={{
                    background: 'var(--success-light)',
                    color: 'var(--success)',
                    border: '1px solid var(--success)',
                    marginBottom: 16,
                  }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontWeight: 700, fontSize: '1rem' }}>
                      <Award size={20} />
                      Score: {submission.marks} / {assignment.maxMarks || 100}
                    </div>
                    {submission.feedback && (
                      <p style={{ marginTop: 6, fontSize: '0.85rem', color: 'var(--text-primary)' }}>
                        <strong>Feedback:</strong> {submission.feedback}
                      </p>
                    )}
                  </div>
                )}

                {submission && !isEvaluated && (
                  <div style={{
                    display: 'flex', alignItems: 'center', gap: 6,
                    padding: '8px 12px', background: 'var(--bg-secondary)',
                    borderRadius: 'var(--radius-sm)', marginBottom: 12,
                    fontSize: '0.8rem', color: 'var(--text-secondary)',
                  }}>
                    <CheckCircle size={14} color="var(--success)" />
                    Submitted on: {new Date(submission.submittedAt).toLocaleString()} ({submission.status})
                  </div>
                )}

                <div className="form-group">
                  <label className="form-label">
                    {submission ? 'Your Submission (editable before due date):' : 'Enter your submission / solution / drive or github link:'}
                  </label>
                  <textarea
                    className="form-textarea"
                    rows={6}
                    value={text}
                    onChange={(e) => setText(e.target.value)}
                    placeholder="Enter answer notes, drive link, or github repository URL..."
                    disabled={saving}
                  />
                </div>
              </>
            )}
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Close</button>
            <button type="submit" className="btn btn-primary" disabled={saving || loading}>
              {saving ? <span className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} /> : (
                <>
                  <Send size={16} />
                  {submission ? 'Update Submission' : 'Submit'}
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ──────── Faculty Submissions & Grading Modal ────────
function FacultySubmissionsModal({ open, onClose, assignment }) {
  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState(null);
  const [marks, setMarks] = useState('');
  const [feedback, setFeedback] = useState('');
  const [evaluating, setEvaluating] = useState(false);

  const loadSubmissions = useCallback(() => {
    if (!open || !assignment) return;
    setLoading(true);
    getSubmissionsForAssignment(assignment.id)
      .then((res) => setSubmissions(res.data || []))
      .catch((err) => toast.error('Failed to load submissions'))
      .finally(() => setLoading(false));
  }, [open, assignment]);

  useEffect(() => {
    loadSubmissions();
    setEditingId(null);
  }, [loadSubmissions]);

  if (!open || !assignment) return null;

  const startEdit = (sub) => {
    setEditingId(sub.id);
    setMarks(sub.marks !== null && sub.marks !== undefined ? String(sub.marks) : '');
    setFeedback(sub.feedback || '');
  };

  const handleSaveEvaluation = async (subId) => {
    if (marks === '') {
      toast.error('Please enter marks');
      return;
    }
    setEvaluating(true);
    try {
      await evaluateSubmission(subId, {
        marks: Number(marks),
        feedback: feedback.trim(),
      });
      toast.success('Evaluation saved');
      setEditingId(null);
      loadSubmissions();
    } catch (err) {
      toast.error(err?.response?.data?.message || err?.message || 'Failed to save evaluation');
    } finally {
      setEvaluating(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" style={{ maxWidth: 720 }} onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div>
            <h3 className="modal-title">Submissions: {assignment.title}</h3>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Max Marks: {assignment.maxMarks || 100} · Total Submissions: {submissions.length}
            </p>
          </div>
          <button className="btn btn-ghost btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <div className="modal-body" style={{ maxHeight: '60vh', overflowY: 'auto' }}>
          {loading ? (
            <div className="loader"><div className="spinner" /></div>
          ) : submissions.length === 0 ? (
            <div className="empty-state" style={{ padding: '30px 0' }}>
              <Clock size={32} color="var(--text-muted)" />
              <p style={{ marginTop: 8 }}>No student submissions yet for this assignment.</p>
            </div>
          ) : (
            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>Student</th>
                    <th>Roll Number</th>
                    <th>Submitted At</th>
                    <th>Status</th>
                    <th>Marks</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {submissions.map((s) => (
                    <tr key={s.id}>
                      <td style={{ fontWeight: 600 }}>{s.student?.name}</td>
                      <td style={{ fontFamily: 'monospace', color: 'var(--accent)' }}>{s.student?.rollNumber}</td>
                      <td style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                        {new Date(s.submittedAt).toLocaleDateString()} {new Date(s.submittedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </td>
                      <td>
                        <span style={{
                          fontSize: '0.7rem', fontWeight: 700, padding: '2px 8px', borderRadius: 12,
                          background: s.status === 'EVALUATED' ? 'var(--success-light)' : (s.status === 'LATE' ? 'var(--danger-light)' : 'var(--accent-light)'),
                          color: s.status === 'EVALUATED' ? 'var(--success)' : (s.status === 'LATE' ? 'var(--danger)' : 'var(--accent)'),
                        }}>
                          {s.status}
                        </span>
                      </td>
                      <td style={{ fontWeight: 700 }}>
                        {s.marks !== null && s.marks !== undefined ? `${s.marks} / ${assignment.maxMarks || 100}` : '—'}
                      </td>
                      <td>
                        <button
                          className="btn btn-secondary btn-sm"
                          onClick={() => startEdit(s)}
                          style={{ fontSize: '0.78rem' }}
                        >
                          {editingId === s.id ? 'Editing' : 'Grade'}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Inline Grading Form */}
          {editingId && (
            <div style={{
              marginTop: 20, padding: 16,
              background: 'var(--bg-secondary)', borderRadius: 'var(--radius-sm)',
              border: '1px solid var(--border)',
            }}>
              <h4 style={{ fontSize: '0.9rem', fontWeight: 700, marginBottom: 12 }}>
                Grade Submission
              </h4>
              {/* Show student's submitted text */}
              {submissions.find((s) => s.id === editingId) && (
                <div style={{ marginBottom: 12, background: 'var(--bg-card)', padding: 10, borderRadius: 6, border: '1px solid var(--border)' }}>
                  <p style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', marginBottom: 4 }}>
                    STUDENT SUBMISSION CONTENT:
                  </p>
                  <p style={{ fontSize: '0.85rem', whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                    {submissions.find((s) => s.id === editingId)?.submissionText}
                  </p>
                </div>
              )}
              <div className="grid-2">
                <div className="form-group">
                  <label className="form-label">Marks (Max: {assignment.maxMarks || 100}) *</label>
                  <input
                    className="form-input"
                    type="number"
                    min="0"
                    max={assignment.maxMarks || 100}
                    value={marks}
                    onChange={(e) => setMarks(e.target.value)}
                    placeholder="Enter marks"
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Feedback</label>
                  <input
                    className="form-input"
                    value={feedback}
                    onChange={(e) => setFeedback(e.target.value)}
                    placeholder="Feedback / Remarks"
                  />
                </div>
              </div>
              <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 8 }}>
                <button type="button" className="btn btn-secondary btn-sm" onClick={() => setEditingId(null)}>Cancel</button>
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  disabled={evaluating}
                  onClick={() => handleSaveEvaluation(editingId)}
                >
                  {evaluating ? <span className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} /> : 'Save Grade'}
                </button>
              </div>
            </div>
          )}
        </div>
        <div className="modal-footer">
          <button type="button" className="btn btn-secondary" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  );
}

// ──────── Main AssignmentsPage ────────
export default function AssignmentsPage() {
  const { user, isFaculty, isStudent, isAdminOrHod } = useAuth();
  const [assignments, setAssignments] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);

  // Modals for submissions
  const [activeAssignmentForStudent, setActiveAssignmentForStudent] = useState(null);
  const [activeAssignmentForFaculty, setActiveAssignmentForFaculty] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [aRes, sRes] = await Promise.all([
        isFaculty && user?.id ? getAssignmentsByFaculty(user.id) : getAllAssignments(),
        isFaculty && user?.id
          ? getSubjectsByFaculty(user.id)
          : (isStudent && user?.id ? getSubjectsByStudent(user.id) : getAllSubjects()),
      ]);
      setAssignments(aRes.data || []);
      setSubjects(sRes.data || []);
    } finally {
      setLoading(false);
    }
  }, [user, isFaculty, isStudent]);

  useEffect(() => { load(); }, [load]);

  const studentSubjectIds = useMemo(
    () => new Set((subjects || []).map((s) => s.id)),
    [subjects]
  );
  const visibleAssignments = isStudent
    ? (assignments || []).filter((a) => a.subject?.id && studentSubjectIds.has(a.subject.id))
    : (assignments || []);

  const filtered = visibleAssignments.filter((a) => {
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
      toast.error(err?.response?.data?.message || err?.message || 'Delete failed');
    }
  };

  const isOverdue = (dateStr) => dateStr && new Date(dateStr) < new Date();

  return (
    <Layout pageTitle="Assignments">
      <div className="page-header">
        <div>
          <h2 className="page-title">Assignments</h2>
          <p className="page-subtitle">
            {isFaculty ? 'Assignments you have posted' : (isStudent ? 'Assignments for your subjects' : 'All assignments')}
          </p>
        </div>
        {(isFaculty || isAdminOrHod) && (
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
                      <span style={{
                        padding: '2px 8px', borderRadius: 4,
                        background: 'var(--accent-light)', color: 'var(--accent)',
                        fontSize: '0.72rem', fontWeight: 700,
                      }}>
                        Max Marks: {a.maxMarks || 100}
                      </span>
                    </div>

                    {a.description && (
                      <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: 10, lineHeight: 1.5 }}>
                        {a.description}
                      </p>
                    )}

                    <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: 8 }}>
                      <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                        <BookOpen size={13} /> {a.subject?.name} ({a.subject?.code})
                      </span>
                      <span>Faculty: {a.faculty?.name}</span>
                    </div>

                    {/* Action buttons */}
                    <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
                      {isStudent && (
                        <button
                          className="btn btn-primary btn-sm"
                          style={{ gap: 6, fontSize: '0.8rem' }}
                          onClick={() => setActiveAssignmentForStudent(a)}
                        >
                          <Send size={14} /> Submit / View Submission
                        </button>
                      )}

                      {(isFaculty || isAdminOrHod) && (
                        <button
                          className="btn btn-secondary btn-sm"
                          style={{ gap: 6, fontSize: '0.8rem' }}
                          onClick={() => setActiveAssignmentForFaculty(a)}
                        >
                          <Award size={14} /> View Submissions & Grade
                        </button>
                      )}
                    </div>
                  </div>

                  {(isFaculty || isAdminOrHod) && (
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

      {/* Creation Modal */}
      {(isFaculty || isAdminOrHod) && (
        <AssignmentModal
          open={modalOpen}
          onClose={() => setModalOpen(false)}
          onSaved={load}
          subjects={subjects}
          facultyId={user?.id}
        />
      )}

      {/* Student Submission Modal */}
      <StudentSubmissionModal
        open={Boolean(activeAssignmentForStudent)}
        onClose={() => setActiveAssignmentForStudent(null)}
        assignment={activeAssignmentForStudent}
      />

      {/* Faculty Submissions & Grading Modal */}
      <FacultySubmissionsModal
        open={Boolean(activeAssignmentForFaculty)}
        onClose={() => setActiveAssignmentForFaculty(null)}
        assignment={activeAssignmentForFaculty}
      />
    </Layout>
  );
}
