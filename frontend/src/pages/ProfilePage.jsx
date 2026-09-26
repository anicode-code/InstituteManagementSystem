import { useEffect, useState } from 'react';
import Layout from '../components/Layout';
import { useAuth } from '../context/AuthContext';
import { getCurrentUser, updateUser } from '../api/users';
import { User, Mail, Hash, Building2, Shield, Pencil, X, Save } from 'lucide-react';
import toast from 'react-hot-toast';

const ROLE_COLORS = {
  ADMIN:   { bg: 'var(--accent-light)',   color: 'var(--accent)' },
  HOD:     { bg: 'var(--info-light)',     color: 'var(--info)' },
  FACULTY: { bg: 'var(--warning-light)',  color: 'var(--warning)' },
  STUDENT: { bg: 'var(--success-light)',  color: 'var(--success)' },
};

function InfoRow({ icon, label, value }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 12,
      padding: '14px 0', borderBottom: '1px solid var(--border)',
    }}>
      <div style={{ color: 'var(--text-muted)', flexShrink: 0 }}>{icon}</div>
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.08em', marginBottom: 2 }}>{label}</div>
        <div style={{ fontWeight: 500, color: value ? 'var(--text-primary)' : 'var(--text-muted)' }}>{value || 'Not provided'}</div>
      </div>
    </div>
  );
}

export default function ProfilePage() {
  const { user: authUser, login } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({ name: '', email: '' });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const res = await getCurrentUser();
        setProfile(res.data);
        setForm({ name: res.data.name || '', email: res.data.email || '' });
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const handleSave = async () => {
    setError('');
    if (!form.name) { setError('Name is required.'); return; }
    setSaving(true);
    try {
      const res = await updateUser(authUser.id, { name: form.name, email: form.email });
      setProfile(res.data);
      toast.success('Profile updated');
      setEditing(false);
    } catch (err) {
      setError(err?.response?.data || 'Update failed');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <Layout pageTitle="My Profile">
        <div className="loader"><div className="spinner" /></div>
      </Layout>
    );
  }

  const p = profile || authUser;
  const initials = p?.name?.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2) || '?';
  const roleStyle = ROLE_COLORS[p?.role] || {};

  return (
    <Layout pageTitle="My Profile">
      <div style={{ maxWidth: 640, margin: '0 auto' }}>
        {/* Profile Card */}
        <div className="card" style={{ marginBottom: 20 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 20, flexWrap: 'wrap' }}>
            <div className="avatar lg" style={{ background: roleStyle.color || 'var(--accent)', boxShadow: `0 0 24px ${roleStyle.color || 'var(--accent)'}33` }}>
              {initials}
            </div>
            <div style={{ flex: 1 }}>
              <h2 style={{ fontSize: '1.5rem', fontWeight: 800, marginBottom: 4 }}>{p?.name}</h2>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
                <span style={{
                  background: roleStyle.bg, color: roleStyle.color,
                  padding: '4px 12px', borderRadius: 20,
                  fontSize: '0.75rem', fontWeight: 700,
                }}>
                  {p?.role}
                </span>
                <span style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
                  @{p?.username}
                </span>
              </div>
            </div>
            <button
              className={`btn ${editing ? 'btn-secondary' : 'btn-primary'}`}
              onClick={() => { setEditing(!editing); setError(''); }}
            >
              {editing ? <><X size={16} /> Cancel</> : <><Pencil size={16} /> Edit</>}
            </button>
          </div>
        </div>

        {/* Edit Form */}
        {editing && (
          <div className="card" style={{ marginBottom: 20 }}>
            <h3 style={{ fontWeight: 700, marginBottom: 16 }}>Edit Profile</h3>
            {error && <div className="alert alert-error">{error}</div>}
            <div className="form-group">
              <label className="form-label">Full Name *</label>
              <input
                className="form-input"
                value={form.name}
                onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                placeholder="Your full name"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Email</label>
              <input
                className="form-input"
                type="email"
                value={form.email}
                onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
                placeholder="your@email.com"
              />
            </div>
            <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
              {saving ? <span className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} /> : <><Save size={16} /> Save Changes</>}
            </button>
          </div>
        )}

        {/* Info Details */}
        <div className="card">
          <h3 style={{ fontWeight: 700, marginBottom: 4 }}>Account Information</h3>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem', marginBottom: 8 }}>Your profile details</p>

          <InfoRow icon={<User size={18} />}     label="Full Name"   value={p?.name} />
          <InfoRow icon={<Hash size={18} />}      label="Username"    value={p?.username} />
          <InfoRow icon={<Mail size={18} />}      label="Email"       value={p?.email} />
          <InfoRow icon={<Shield size={18} />}    label="Role"        value={p?.role} />
          <InfoRow icon={<Building2 size={18} />} label="Department"  value={p?.department?.name} />
          <div style={{ paddingTop: 12, fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            User ID: #{p?.id}
          </div>
        </div>
      </div>
    </Layout>
  );
}
