import { useEffect, useState, useCallback } from 'react';
import Layout from '../components/Layout';
import { getAllDepartments, createDepartment, updateDepartment, deleteDepartment } from '../api/departments';
import { Plus, Pencil, Trash2, Building2, X, Search } from 'lucide-react';
import toast from 'react-hot-toast';

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

export default function DepartmentsPage() {
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editDept, setEditDept] = useState(null);

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

  return (
    <Layout pageTitle="Departments">
      <div className="page-header">
        <div>
          <h2 className="page-title">Departments</h2>
          <p className="page-subtitle">{departments.length} departments registered</p>
        </div>
        <button className="btn btn-primary" onClick={() => { setEditDept(null); setModalOpen(true); }}>
          <Plus size={18} /> Add Department
        </button>
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
                <div style={{ display: 'flex', gap: 4, flexShrink: 0 }}>
                  <button className="btn btn-ghost btn-icon btn-sm" onClick={() => { setEditDept(d); setModalOpen(true); }}>
                    <Pencil size={14} />
                  </button>
                  <button className="btn btn-ghost btn-icon btn-sm" style={{ color: 'var(--danger)' }} onClick={() => handleDelete(d)}>
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
              <div style={{ marginTop: 12, paddingTop: 12, borderTop: '1px solid var(--border)', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                ID: {d.id}
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
    </Layout>
  );
}
