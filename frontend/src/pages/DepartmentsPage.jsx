import { useEffect, useState, useCallback } from 'react';
import Layout from '../components/Layout';
import { useAuth } from '../context/AuthContext';
import {
  getAllDepartments, createDepartment, updateDepartment, deleteDepartment,
  getEligibleHods, assignHod, removeHod,
} from '../api/departments';
import { Plus, Pencil, Trash2, Building2, X, Search, UserCheck, UserMinus, Crown } from 'lucide-react';
import toast from 'react-hot-toast';

// ─── Department Create/Edit Modal ───────────────────────────────────
function DeptModal({ open, onClose, onSaved, editDept }) {
  const [form, setForm] = useState({ name: '', code: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setForm(editDept ? { name: editDept.name, code: editDept.code } : { name: '', code: '' });
    setError('');
  }, [editDept, open]);

  if (!open) return null;

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.name || !form.code) { setError('Name and code are required.'); return; }
    setLoading(true);
    try {
      if (editDept) {
        await updateDepartment(editDept.id, form);
        toast.success('Department updated');
      } else {
        await createDepartment(form);
        toast.success('Department created');
      }
      onSaved();
      onClose();
    } catch (err) {
      setError(err?.response?.data || err?.message || 'Error saving department');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3 className="modal-title">{editDept ? 'Edit Department' : 'Add Department'}</h3>
          <button className="btn btn-ghost btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}
            <div className="form-group">
              <label className="form-label">Department Name *</label>
              <input className="form-input" value={form.name} onChange={(e) => set('name', e.target.value)} placeholder="Computer Science" />
            </div>
            <div className="form-group">
              <label className="form-label">Department Code *</label>
              <input className="form-input" value={form.code} onChange={(e) => set('code', e.target.value.toUpperCase())} placeholder="CSE" maxLength={10} />
              <div className="form-error" style={{ color: 'var(--text-muted)' }}>Must be unique (e.g. CSE, ECE, MECH)</div>
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? <span className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} /> : (editDept ? 'Update' : 'Create')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ─── Assign HOD Modal ────────────────────────────────────────────────
function AssignHodModal({ open, onClose, onSaved, department }) {
  const [eligibleFaculty, setEligibleFaculty] = useState([]);
  const [loadingFaculty, setLoadingFaculty] = useState(false);
  const [selectedFacultyId, setSelectedFacultyId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!open || !department) return;
    setSelectedFacultyId('');
    setError('');
    setLoadingFaculty(true);
    getEligibleHods(department.id)
      .then((res) => setEligibleFaculty(res.data || []))
      .catch(() => setError('Failed to load eligible faculty.'))
      .finally(() => setLoadingFaculty(false));
  }, [open, department]);

  if (!open || !department) return null;

  const currentHodId = department.hod?.id;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!selectedFacultyId) { setError('Please select a faculty member.'); return; }
    setLoading(true);
    try {
      await assignHod(department.id, Number(selectedFacultyId));
      toast.success(`HOD assigned for ${department.name}`);
      onSaved();
      onClose();
    } catch (err) {
      const msg = err?.response?.data || err?.message || 'Failed to assign HOD.';
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3 className="modal-title">
            {department.hod ? 'Replace HOD' : 'Assign HOD'} — {department.name}
          </h3>
          <button className="btn btn-ghost btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error" style={{ marginBottom: 12 }}>{error}</div>}

            {department.hod && (
              <div className="alert" style={{
                background: 'rgba(245,158,11,0.1)',
                color: '#f59e0b',
                border: '1px solid #f59e0b',
                borderRadius: 'var(--radius-sm)',
                padding: '10px 14px',
                fontSize: '0.82rem',
                marginBottom: 16,
              }}>
                ⚠️ This will replace <strong>{department.hod.name}</strong> as HOD of {department.name}.
                Their role will revert to Faculty unless they are HOD of another department.
              </div>
            )}

            {loadingFaculty ? (
              <div className="loader" style={{ padding: 24 }}><div className="spinner" /></div>
            ) : eligibleFaculty.length === 0 ? (
              <div className="empty-state" style={{ padding: 24 }}>
                <UserCheck size={32} style={{ opacity: 0.3 }} />
                <p style={{ marginTop: 8, color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                  No eligible faculty found. Faculty members must register with this department
                  to be eligible as HOD.
                </p>
              </div>
            ) : (
              <div className="form-group">
                <label className="form-label">Select Faculty Member *</label>
                <div style={{
                  border: '1px solid var(--border)',
                  borderRadius: 'var(--radius-sm)',
                  overflow: 'hidden',
                }}>
                  {eligibleFaculty.map((f, idx) => {
                    const isCurrentHod = f.id === currentHodId;
                    const isSelected = String(selectedFacultyId) === String(f.id);
                    return (
                      <button
                        key={f.id}
                        type="button"
                        onClick={() => setSelectedFacultyId(f.id)}
                        disabled={isCurrentHod}
                        style={{
                          width: '100%',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '10px 14px',
                          background: isSelected ? 'var(--accent-light)' : 'transparent',
                          border: 'none',
                          borderTop: idx > 0 ? '1px solid var(--border)' : 'none',
                          cursor: isCurrentHod ? 'default' : 'pointer',
                          opacity: isCurrentHod ? 0.5 : 1,
                          textAlign: 'left',
                          color: isSelected ? 'var(--accent)' : 'var(--text-primary)',
                          transition: 'background 0.15s',
                        }}
                      >
                        <span>
                          <span style={{ fontWeight: 600 }}>{f.name}</span>
                          <span style={{ marginLeft: 8, fontSize: '0.72rem', color: 'var(--text-muted)' }}>
                            {f.rollNumber}{f.email ? ` · ${f.email}` : ''}
                          </span>
                        </span>
                        {isCurrentHod && (
                          <span style={{
                            background: 'var(--info-light)',
                            color: 'var(--info)',
                            padding: '2px 8px',
                            borderRadius: 12,
                            fontSize: '0.68rem',
                            fontWeight: 700,
                          }}>Current HOD</span>
                        )}
                      </button>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading || loadingFaculty || eligibleFaculty.length === 0 || !selectedFacultyId}
            >
              {loading
                ? <span className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} />
                : (department.hod ? 'Replace HOD' : 'Assign HOD')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────
export default function DepartmentsPage() {
  const { isAdmin } = useAuth();
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editDept, setEditDept] = useState(null);
  const [hodModalOpen, setHodModalOpen] = useState(false);
  const [hodModalDept, setHodModalDept] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getAllDepartments();
      setDepartments(res.data);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const filtered = departments.filter((d) => {
    const q = search.toLowerCase();
    return !q || d.name?.toLowerCase().includes(q) || d.code?.toLowerCase().includes(q);
  });

  const handleDelete = async (d) => {
    if (!window.confirm(`Delete department "${d.name}"? This may fail if users/subjects reference it.`)) return;
    try {
      await deleteDepartment(d.id);
      toast.success('Department deleted');
      load();
    } catch (err) {
      toast.error(err?.response?.data || 'Delete failed. Department may have dependencies.');
    }
  };

  const handleRemoveHod = async (d) => {
    if (!window.confirm(`Remove ${d.hod?.name} as HOD of ${d.name}?`)) return;
    try {
      await removeHod(d.id);
      toast.success('HOD removed');
      load();
    } catch (err) {
      const msg = err?.response?.data || 'Failed to remove HOD.';
      toast.error(typeof msg === 'string' ? msg : 'Failed to remove HOD.');
    }
  };

  const openHodModal = (d) => {
    setHodModalDept(d);
    setHodModalOpen(true);
  };

  return (
    <Layout pageTitle="Departments">
      <div className="page-header">
        <div>
          <h2 className="page-title">Departments</h2>
          <p className="page-subtitle">{departments.length} departments registered</p>
        </div>
        {isAdmin && (
          <button className="btn btn-primary" onClick={() => { setEditDept(null); setModalOpen(true); }}>
            <Plus size={18} /> Add Department
          </button>
        )}
      </div>

      <div className="toolbar">
        <div className="toolbar-left">
          <div className="search-bar">
            <Search />
            <input
              placeholder="Search departments..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
        </div>
      </div>

      {loading ? (
        <div className="loader"><div className="spinner" /></div>
      ) : filtered.length === 0 ? (
        <div className="empty-state">
          <Building2 />
          <h3>No departments found</h3>
          <p>{departments.length === 0 ? 'Create your first department to get started.' : 'No matches for your search.'}</p>
        </div>
      ) : (
        <div className="grid-3">
          {filtered.map((d) => (
            <div key={d.id} className="card">
              {/* Header Row */}
              <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 12 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div className="stat-icon" style={{ background: 'var(--accent-light)', width: 44, height: 44 }}>
                    <Building2 size={20} color="var(--accent)" />
                  </div>
                  <div>
                    <div style={{ fontWeight: 700, fontSize: '1rem', marginBottom: 2 }}>{d.name}</div>
                    <span style={{
                      background: 'var(--bg-hover)',
                      padding: '2px 8px',
                      borderRadius: 4,
                      fontSize: '0.72rem',
                      fontWeight: 700,
                      color: 'var(--text-muted)',
                      letterSpacing: '0.06em',
                    }}>{d.code}</span>
                  </div>
                </div>
                {isAdmin && (
                  <div style={{ display: 'flex', gap: 4, flexShrink: 0 }}>
                    <button className="btn btn-ghost btn-icon btn-sm" title="Edit department" onClick={() => { setEditDept(d); setModalOpen(true); }}>
                      <Pencil size={14} />
                    </button>
                    <button className="btn btn-ghost btn-icon btn-sm" title="Delete department" style={{ color: 'var(--danger)' }} onClick={() => handleDelete(d)}>
                      <Trash2 size={14} />
                    </button>
                  </div>
                )}
              </div>

              {/* HOD Section */}
              <div style={{
                marginTop: 14,
                paddingTop: 12,
                borderTop: '1px solid var(--border)',
              }}>
                <div style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-muted)', letterSpacing: '0.06em', marginBottom: 8 }}>
                  HEAD OF DEPARTMENT
                </div>

                {d.hod ? (
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <div style={{
                        width: 30, height: 30,
                        borderRadius: '50%',
                        background: 'var(--info-light)',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        flexShrink: 0,
                      }}>
                        <Crown size={14} color="var(--info)" />
                      </div>
                      <div>
                        <div style={{ fontWeight: 600, fontSize: '0.85rem' }}>{d.hod.name}</div>
                        <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>{d.hod.rollNumber}</div>
                      </div>
                    </div>
                    {isAdmin && (
                      <div style={{ display: 'flex', gap: 4, flexShrink: 0 }}>
                        <button
                          className="btn btn-ghost btn-icon btn-sm"
                          title="Replace HOD"
                          onClick={() => openHodModal(d)}
                          style={{ color: 'var(--info)' }}
                        >
                          <UserCheck size={14} />
                        </button>
                        <button
                          className="btn btn-ghost btn-icon btn-sm"
                          title="Remove HOD"
                          onClick={() => handleRemoveHod(d)}
                          style={{ color: 'var(--danger)' }}
                        >
                          <UserMinus size={14} />
                        </button>
                      </div>
                    )}
                  </div>
                ) : (
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <span style={{ color: 'var(--text-muted)', fontSize: '0.82rem', fontStyle: 'italic' }}>
                      No HOD assigned
                    </span>
                    {isAdmin && (
                      <button
                        className="btn btn-ghost btn-sm"
                        style={{ fontSize: '0.75rem', padding: '4px 10px' }}
                        onClick={() => openHodModal(d)}
                      >
                        <UserCheck size={12} /> Assign HOD
                      </button>
                    )}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      <DeptModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSaved={load}
        editDept={editDept}
      />

      <AssignHodModal
        open={hodModalOpen}
        onClose={() => { setHodModalOpen(false); setHodModalDept(null); }}
        onSaved={load}
        department={hodModalDept}
      />
    </Layout>
  );
}
