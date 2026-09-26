import { useEffect, useState, useCallback } from 'react';
import Layout from '../components/Layout';
import { useAuth } from '../context/AuthContext';
import { getAllUsers, createUser, updateUser, deleteUser } from '../api/users';
import { getAllDepartments } from '../api/departments';
import { Plus, Search, Pencil, Trash2, X, Users } from 'lucide-react';
import toast from 'react-hot-toast';

const ROLES = ['ADMIN', 'HOD', 'FACULTY', 'STUDENT'];

const ROLE_COLORS = {
  ADMIN:   { bg: 'var(--accent-light)',   color: 'var(--accent)' },
  HOD:     { bg: 'var(--info-light)',     color: 'var(--info)' },
  FACULTY: { bg: 'var(--warning-light)',  color: 'var(--warning)' },
  STUDENT: { bg: 'var(--success-light)',  color: 'var(--success)' },
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

function UserModal({ open, onClose, onSaved, departments, editUser }) {
  const empty = { name: '', username: '', password: '', email: '', role: 'STUDENT', departmentId: '' };
  const [form, setForm] = useState(empty);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (editUser) {
      setForm({
        name: editUser.name || '',
        username: editUser.username || '',
        password: '',
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
    if (!form.name || !form.username || (!editUser && !form.password) || !form.role) {
      setError('Name, username, password and role are required.');
      return;
    }
    setLoading(true);
    try {
      const payload = {
        name: form.name,
        username: form.username,
        email: form.email || undefined,
        role: form.role,
        departmentId: form.departmentId ? Number(form.departmentId) : undefined,
      };
      if (!editUser) payload.password = form.password;

      if (editUser) {
        await updateUser(editUser.id, payload);
        toast.success('User updated');
      } else {
        await createUser({ ...payload, password: form.password });
        toast.success('User created');
      }
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
          <h3 className="modal-title">{editUser ? 'Edit User' : 'Create User'}</h3>
          <button className="btn btn-ghost btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}
            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">Full Name *</label>
                <input className="form-input" value={form.name} onChange={(e) => set('name', e.target.value)} placeholder="John Doe" />
              </div>
              <div className="form-group">
                <label className="form-label">Username *</label>
                <input className="form-input" value={form.username} onChange={(e) => set('username', e.target.value)} placeholder="johndoe" disabled={!!editUser} />
              </div>
            </div>
            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">Email</label>
                <input className="form-input" type="email" value={form.email} onChange={(e) => set('email', e.target.value)} placeholder="john@example.com" />
              </div>
              {!editUser && (
                <div className="form-group">
                  <label className="form-label">Password *</label>
                  <input className="form-input" type="password" value={form.password} onChange={(e) => set('password', e.target.value)} placeholder="••••••••" />
                </div>
              )}
            </div>
            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">Role *</label>
                <select className="form-select" value={form.role} onChange={(e) => set('role', e.target.value)}>
                  {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Department</label>
                <select className="form-select" value={form.departmentId} onChange={(e) => set('departmentId', e.target.value)}>
                  <option value="">None</option>
                  {departments.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
                </select>
              </div>
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? <span className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} /> : (editUser ? 'Update' : 'Create')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function UsersPage() {
  const { user: me } = useAuth();
  const [users, setUsers] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [modalOpen, setModalOpen] = useState(false);
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
    const q = search.toLowerCase();
    const matchSearch = !q || u.name?.toLowerCase().includes(q) || u.username?.toLowerCase().includes(q) || u.email?.toLowerCase().includes(q);
    return matchRole && matchSearch;
  });

  const handleDelete = async (u) => {
    if (u.id === me.id) { toast.error("You can't delete yourself"); return; }
    if (!window.confirm(`Delete user "${u.name}"?`)) return;
    try {
      await deleteUser(u.id);
      toast.success('User deleted');
      load();
    } catch (err) {
      // Backend GlobalExceptionHandler returns { message, timestamp } JSON object.
      // DB constraint violations (FK references to attendance/subjects/assignments)
      // come back as 500s with a different shape — handle both cases.
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
        <button className="btn btn-primary" onClick={() => { setEditUser(null); setModalOpen(true); }}>
          <Plus size={18} /> Add User
        </button>
      </div>

      <div className="toolbar">
        <div className="toolbar-left">
          <div className="search-bar">
            <Search />
            <input
              placeholder="Search by name, username or email..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <div className="filter-tabs">
            {['ALL', ...ROLES].map((r) => (
              <button key={r} className={`filter-tab ${roleFilter === r ? 'active' : ''}`} onClick={() => setRoleFilter(r)}>
                {r === 'ALL' ? 'All' : r}
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
                  <th>Username</th>
                  <th>Email</th>
                  <th>Role</th>
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
                      <td style={{ color: 'var(--text-secondary)' }}>{u.username}</td>
                      <td style={{ color: 'var(--text-secondary)' }}>{u.email || '—'}</td>
                      <td><RoleBadge role={u.role} /></td>
                      <td style={{ color: 'var(--text-secondary)' }}>{u.department?.name || '—'}</td>
                      <td>
                        <div style={{ display: 'flex', gap: 6 }}>
                          <button className="btn btn-ghost btn-icon btn-sm" title="Edit" onClick={() => { setEditUser(u); setModalOpen(true); }}>
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

      <UserModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSaved={load}
        departments={departments}
        editUser={editUser}
      />
    </Layout>
  );
}
