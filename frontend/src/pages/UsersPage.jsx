import { useEffect, useState, useCallback } from 'react';
import Layout from '../components/Layout';
import { useAuth } from '../context/AuthContext';
import { getAllUsers, updateUser, deleteUser, getPendingUsers, verifyUser, verifyBulk } from '../api/users';
import { getAllDepartments } from '../api/departments';
import { Search, Pencil, Trash2, X, Users, ShieldCheck, CheckSquare, Square } from 'lucide-react';
import toast from 'react-hot-toast';

const ROLES = ['ADMIN', 'HOD', 'FACULTY', 'STUDENT'];

const ROLE_COLORS = {
  ADMIN:   { bg: 'var(--accent-light)',   color: 'var(--accent)' },
  HOD:     { bg: 'var(--info-light)',     color: 'var(--info)' },
  FACULTY: { bg: 'var(--warning-light)',  color: 'var(--warning)' },
  STUDENT: { bg: 'var(--success-light)',  color: 'var(--success)' },
};

const STATUS_COLORS = {
  PENDING:  { bg: 'rgba(245,158,11,0.12)', color: '#f59e0b' },
  VERIFIED: { bg: 'rgba(34,197,94,0.12)',  color: '#22c55e' },
};

function RoleBadge({ role }) {
  const s = ROLE_COLORS[role] || {};
  return (
    <span style={{
      background: s.bg, color: s.color,
      padding: '3px 10px', borderRadius: 20,
      fontSize: '0.7rem', fontWeight: 700,
    }}>
      {role}
    </span>
  );
}

function StatusBadge({ status }) {
  const s = STATUS_COLORS[status] || {};
  return (
    <span style={{
      background: s.bg, color: s.color,
      padding: '3px 10px', borderRadius: 20,
      fontSize: '0.7rem', fontWeight: 700,
    }}>
      {status}
    </span>
  );
}

function EditModal({ open, onClose, onSaved, departments, editUser }) {
  const empty = { name: '', email: '', role: 'STUDENT', departmentId: '' };
  const [form, setForm] = useState(empty);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (editUser) {
      setForm({
        name: editUser.name || '',
        email: editUser.email || '',
        role: editUser.role || 'STUDENT',
        departmentId: editUser.department?.id || '',
      });
    } else {
      setForm(empty);
    }
    setError('');
  }, [editUser, open]);

  if (!open) return null;

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!form.name || !form.role) {
      setError('Name and role are required.');
      return;
    }
    setLoading(true);
    try {
      const payload = {
        name: form.name,
        email: form.email || undefined,
        role: form.role,
        departmentId: form.departmentId ? Number(form.departmentId) : undefined,
      };
      await updateUser(editUser.id, payload);
      toast.success('User updated');
      onSaved();
      onClose();
    } catch (err) {
      const msg = err?.response?.data || err?.message || 'Error saving user';
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3 className="modal-title">Edit User</h3>
          <button className="btn btn-ghost btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}
            <div className="form-group">
              <label className="form-label">Roll Number</label>
              <input className="form-input" value={editUser?.rollNumber || ''} disabled />
            </div>
            <div className="form-group">
              <label className="form-label">Full Name *</label>
              <input className="form-input" value={form.name} onChange={(e) => set('name', e.target.value)} placeholder="John Doe" />
            </div>
            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">Email</label>
                <input className="form-input" type="email" value={form.email} onChange={(e) => set('email', e.target.value)} placeholder="john@example.com" />
              </div>
              <div className="form-group">
                <label className="form-label">Role *</label>
                <select className="form-select" value={form.role} onChange={(e) => set('role', e.target.value)}>
                  {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
                </select>
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">Department</label>
              <select className="form-select" value={form.departmentId} onChange={(e) => set('departmentId', e.target.value)}>
                <option value="">None</option>
                {departments.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
              </select>
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

// ─── Pending Verification Panel ───────────────────────────────────
function PendingVerificationPanel({ onVerified }) {
  const [pending, setPending] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(new Set());
  const [verifying, setVerifying] = useState(false);

  const loadPending = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getPendingUsers();
      setPending(res.data);
      setSelected(new Set());
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadPending(); }, [loadPending]);

  const allSelected = pending.length > 0 && selected.size === pending.length;
  const someSelected = selected.size > 0 && !allSelected;

  const toggleSelectAll = () => {
    if (allSelected) {
      setSelected(new Set());
    } else {
      setSelected(new Set(pending.map((u) => u.id)));
    }
  };

  const toggleOne = (id) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleVerifyOne = async (user) => {
    if (verifying) return;
    setVerifying(true);
    try {
      await verifyUser(user.id);
      toast.success(`${user.name} verified`);
      await loadPending();
      onVerified();
    } catch (err) {
      const msg = err?.response?.data?.message || err?.message || 'Verification failed';
      toast.error(typeof msg === 'string' ? msg : 'Verification failed');
    } finally {
      setVerifying(false);
    }
  };

  const handleVerifySelected = async () => {
    if (selected.size === 0) { toast.error('No users selected'); return; }
    if (verifying) return;
    setVerifying(true);
    try {
      const res = await verifyBulk([...selected]);
      toast.success(res.data.message);
      await loadPending();
      onVerified();
    } catch (err) {
      const msg = err?.response?.data?.message || err?.message || 'Bulk verification failed';
      toast.error(typeof msg === 'string' ? msg : 'Bulk verification failed');
    } finally {
      setVerifying(false);
    }
  };

  if (loading) {
    return (
      <div className="card" style={{ marginBottom: 24 }}>
        <div className="loader"><div className="spinner" /></div>
      </div>
    );
  }

  return (
    <div className="card" style={{ marginBottom: 24, padding: 0 }}>
      <div style={{ padding: '20px 24px 16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12, borderBottom: '1px solid var(--border)' }}>
        <div>
          <h3 style={{ fontWeight: 700, fontSize: '1rem', display: 'flex', alignItems: 'center', gap: 8 }}>
            <ShieldCheck size={18} style={{ color: 'var(--warning, #f59e0b)' }} />
            Pending Verification
            <span style={{ background: 'rgba(245,158,11,0.15)', color: '#f59e0b', borderRadius: 20, fontSize: '0.7rem', fontWeight: 700, padding: '2px 8px' }}>
              {pending.length}
            </span>
          </h3>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem', marginTop: 2 }}>
            Self-registered users awaiting admin verification
          </p>
        </div>
        {selected.size > 0 && (
          <button
            className="btn btn-primary"
            onClick={handleVerifySelected}
            disabled={verifying}
            style={{ gap: 6 }}
          >
            {verifying
              ? <span className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} />
              : <ShieldCheck size={15} />
            }
            Verify Selected ({selected.size})
          </button>
        )}
      </div>

      {pending.length === 0 ? (
        <div className="empty-state" style={{ padding: '32px 24px' }}>
          <ShieldCheck style={{ color: 'var(--success)' }} />
          <h3>No pending users</h3>
          <p>All registered users have been verified</p>
        </div>
      ) : (
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th style={{ width: 40 }}>
                  <input
                    type="checkbox"
                    checked={allSelected}
                    ref={(el) => { if (el) el.indeterminate = someSelected; }}
                    onChange={toggleSelectAll}
                    title="Select all"
                    style={{ cursor: 'pointer', width: 16, height: 16 }}
                  />
                </th>
                <th>Roll Number</th>
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
                <th>Department</th>
                <th>Registered</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {pending.map((u) => {
                const initials = u.name?.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2) || '?';
                const isChecked = selected.has(u.id);
                return (
                  <tr key={u.id} style={{ background: isChecked ? 'rgba(var(--accent-rgb, 99,102,241),0.06)' : undefined }}>
                    <td>
                      <input
                        type="checkbox"
                        checked={isChecked}
                        onChange={() => toggleOne(u.id)}
                        style={{ cursor: 'pointer', width: 16, height: 16 }}
                      />
                    </td>
                    <td style={{ fontFamily: 'monospace', fontWeight: 600, color: 'var(--accent)' }}>
                      {u.rollNumber}
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                        <div className="avatar sm" style={{ background: ROLE_COLORS[u.role]?.color || 'var(--accent)', flexShrink: 0 }}>{initials}</div>
                        <span style={{ fontWeight: 600 }}>{u.name}</span>
                      </div>
                    </td>
                    <td style={{ color: 'var(--text-secondary)' }}>{u.email || '—'}</td>
                    <td><RoleBadge role={u.role} /></td>
                    <td style={{ color: 'var(--text-secondary)' }}>{u.department?.name || '—'}</td>
                    <td style={{ color: 'var(--text-muted)', fontSize: '0.78rem' }}>
                      {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '—'}
                    </td>
                    <td>
                      <button
                        className="btn btn-primary btn-sm"
                        onClick={() => handleVerifyOne(u)}
                        disabled={verifying}
                        style={{ gap: 4, fontSize: '0.78rem', padding: '4px 10px' }}
                      >
                        <ShieldCheck size={13} />
                        Verify
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

// ─── Main UsersPage ───────────────────────────────────────────────
export default function UsersPage() {
  const { user: me, isAdminOrHod } = useAuth();
  const [users, setUsers] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editUser, setEditUser] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [usersRes, depsRes] = await Promise.all([getAllUsers(), getAllDepartments()]);
      setUsers(usersRes.data);
      setDepartments(depsRes.data);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const filtered = users.filter((u) => {
    const matchRole = roleFilter === 'ALL' || u.role === roleFilter;
    const matchStatus = statusFilter === 'ALL' || u.status === statusFilter;
    const q = search.toLowerCase();
    const matchSearch = !q
      || u.name?.toLowerCase().includes(q)
      || u.rollNumber?.toLowerCase().includes(q)
      || u.email?.toLowerCase().includes(q);
    return matchRole && matchStatus && matchSearch;
  });

  const handleDelete = async (u) => {
    if (u.id === me.id) { toast.error("You can't delete yourself"); return; }
    if (!window.confirm(`Delete user "${u.name}"?`)) return;
    try {
      await deleteUser(u.id);
      toast.success('User deleted');
      load();
    } catch (err) {
      const data = err?.response?.data;
      let msg;
      if (typeof data === 'string') {
        msg = data;
      } else if (data?.message) {
        msg = data.message;
      } else if (err?.response?.status === 500) {
        msg = `Cannot delete ${u.role.toLowerCase()} "${u.name}" — they still have linked records (attendance / subjects / assignments). Remove those first.`;
      } else {
        msg = err?.message || 'Delete failed';
      }
      toast.error(msg, { duration: 5000 });
    }
  };

  return (
    <Layout pageTitle="Users">
      <div className="page-header">
        <div>
          <h2 className="page-title">User Management</h2>
          <p className="page-subtitle">{users.length} total users</p>
        </div>
      </div>

      {/* Pending verification panel — admin/HOD only */}
      {isAdminOrHod && (
        <PendingVerificationPanel onVerified={load} />
      )}

      {/* All users table */}
      <div className="toolbar">
        <div className="toolbar-left">
          <div className="search-bar">
            <Search />
            <input
              placeholder="Search by name, roll number or email..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <div className="filter-tabs">
            {['ALL', ...ROLES].map((r) => (
              <button key={r} className={`filter-tab ${roleFilter === r ? 'active' : ''}`} onClick={() => setRoleFilter(r)}>
                {r === 'ALL' ? 'All Roles' : r}
              </button>
            ))}
          </div>
          <div className="filter-tabs">
            {['ALL', 'PENDING', 'VERIFIED'].map((s) => (
              <button key={s} className={`filter-tab ${statusFilter === s ? 'active' : ''}`} onClick={() => setStatusFilter(s)}>
                {s === 'ALL' ? 'All Status' : s}
              </button>
            ))}
          </div>
        </div>
        <div className="toolbar-right">
          <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>{filtered.length} results</span>
        </div>
      </div>

      <div className="card" style={{ padding: 0 }}>
        {loading ? (
          <div className="loader"><div className="spinner" /></div>
        ) : filtered.length === 0 ? (
          <div className="empty-state">
            <Users />
            <h3>No users found</h3>
            <p>Try adjusting your search or filters</p>
          </div>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>User</th>
                  <th>Roll Number</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Department</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((u) => {
                  const initials = u.name?.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2) || '?';
                  return (
                    <tr key={u.id}>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                          <div className="avatar sm" style={{ background: ROLE_COLORS[u.role]?.color || 'var(--accent)' }}>{initials}</div>
                          <span style={{ fontWeight: 600 }}>{u.name}</span>
                          {u.id === me.id && <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>(you)</span>}
                        </div>
                      </td>
                      <td style={{ fontFamily: 'monospace', color: 'var(--accent)', fontWeight: 600 }}>{u.rollNumber}</td>
                      <td style={{ color: 'var(--text-secondary)' }}>{u.email || '—'}</td>
                      <td><RoleBadge role={u.role} /></td>
                      <td><StatusBadge status={u.status} /></td>
                      <td style={{ color: 'var(--text-secondary)' }}>{u.department?.name || '—'}</td>
                      <td>
                        <div style={{ display: 'flex', gap: 6 }}>
                          <button className="btn btn-ghost btn-icon btn-sm" title="Edit" onClick={() => { setEditUser(u); setEditModalOpen(true); }}>
                            <Pencil size={15} />
                          </button>
                          <button className="btn btn-ghost btn-icon btn-sm" title="Delete" style={{ color: 'var(--danger)' }} onClick={() => handleDelete(u)}>
                            <Trash2 size={15} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <EditModal
        open={editModalOpen}
        onClose={() => setEditModalOpen(false)}
        onSaved={load}
        departments={departments}
        editUser={editUser}
      />
    </Layout>
  );
}
