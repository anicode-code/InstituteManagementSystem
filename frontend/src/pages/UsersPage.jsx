import { useEffect, useState, useCallback } from 'react';
import Layout from '../components/Layout';
import { useAuth } from '../context/AuthContext';
import { getAllUsers, getPendingUsers, verifyUser, verifyBulk, rejectUser, rejectUsers } from '../api/users';
import { Search, Users, ShieldCheck, Check, X } from 'lucide-react';
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

  const handleApproveOne = async (user) => {
    if (verifying) return;
    setVerifying(true);
    try {
      await verifyUser(user.id);
      toast.success(`${user.name} approved`);
      await loadPending();
      onVerified();
    } catch (err) {
      const msg = err?.response?.data?.message || err?.message || 'Verification failed';
      toast.error(typeof msg === 'string' ? msg : 'Verification failed');
    } finally {
      setVerifying(false);
    }
  };

  const handleRejectOne = async (user) => {
    if (verifying) return;
    if (!window.confirm(`Reject ${user.name}'s registration? This will permanently delete the pending account.`)) return;
    setVerifying(true);
    try {
      await rejectUser(user.id);
      toast.success(`${user.name}'s registration was rejected`);
      await loadPending();
      onVerified();
    } catch (err) {
      const msg = err?.response?.data?.message || err?.message || 'Registration rejection failed';
      toast.error(typeof msg === 'string' ? msg : 'Registration rejection failed');
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

  const handleRejectSelected = async () => {
    if (selected.size === 0) { toast.error('No users selected'); return; }
    if (!window.confirm(`Reject and permanently delete ${selected.size} selected pending registration(s)?`)) return;
    if (verifying) return;
    setVerifying(true);
    try {
      const res = await rejectUsers([...selected]);
      toast.success(res.data);
      await loadPending();
      onVerified();
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data || err?.message || 'Bulk rejection failed';
      toast.error(typeof msg === 'string' ? msg : 'Bulk rejection failed');
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
            Self-registered users awaiting approval
          </p>
        </div>
        {selected.size > 0 && (
          <div style={{ display: 'flex', gap: 8 }}>
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
              Approve Selected ({selected.size})
            </button>
            <button
              className="btn btn-secondary"
              onClick={handleRejectSelected}
              disabled={verifying}
              style={{ gap: 6, color: 'var(--danger)' }}
            >
              <X size={15} />
              Reject Selected ({selected.size})
            </button>
          </div>
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
                <th>Registered</th>
                <th>Decision</th>
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
                    <td style={{ color: 'var(--text-muted)', fontSize: '0.78rem' }}>
                      {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '—'}
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: 6 }}>
                        <button
                          className="btn btn-primary btn-sm"
                          onClick={() => handleApproveOne(u)}
                          disabled={verifying}
                          style={{ gap: 4, fontSize: '0.78rem', padding: '4px 10px' }}
                        >
                          <Check size={13} />
                          Approve
                        </button>
                        <button
                          className="btn btn-secondary btn-sm"
                          onClick={() => handleRejectOne(u)}
                          disabled={verifying}
                          style={{ gap: 4, fontSize: '0.78rem', padding: '4px 10px', color: 'var(--danger)' }}
                        >
                          <X size={13} />
                          Reject
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
  );
}

// ─── Main UsersPage ───────────────────────────────────────────────
export default function UsersPage() {
  const { user: me, isAdminOrHod } = useAuth();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const load = useCallback(async () => {
    setLoading(true);
    try {
      const usersRes = await getAllUsers();
      setUsers(usersRes.data);
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
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

    </Layout>
  );
}
