import { useEffect, useMemo, useState } from 'react';
import { BriefcaseBusiness, Mail, Phone, Search, Users } from 'lucide-react';
import Layout from '../components/Layout';
import { getHodFacultyRoster } from '../api/users';

const verificationStyles = {
  VERIFIED: { background: 'var(--success-light)', color: 'var(--success)' },
  PENDING: { background: 'var(--warning-light)', color: 'var(--warning)' },
};

function display(value) {
  return value || '—';
}

function initials(name) {
  return (name || '?')
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase();
}

export default function HodFacultyPage() {
  const [faculty, setFaculty] = useState([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    getHodFacultyRoster()
      .then((response) => {
        if (active) setFaculty(response.data || []);
      })
      .catch((requestError) => {
        if (active) {
          setError(requestError.response?.data?.message || 'Failed to load faculty. Please try again.');
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => { active = false; };
  }, []);

  const filteredFaculty = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return faculty;
    return faculty.filter((member) => [
      member.name,
      member.facultyId,
      member.role,
      member.email,
      member.phone,
      member.designation,
      member.qualification,
      member.specialization,
      member.employmentType,
      member.employmentStatus,
      ...(member.departments || []),
    ].filter(Boolean).some((value) => String(value).toLowerCase().includes(query)));
  }, [faculty, search]);

  const verifiedCount = faculty.filter((member) => member.status === 'VERIFIED').length;

  return (
    <Layout pageTitle="Faculty List">
      <div className="page-header">
        <div>
          <h2 className="page-title">Faculty List</h2>
          <p className="page-subtitle">Faculty members associated with your appointed department(s).</p>
        </div>
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '10px 14px' }}>
            <Users size={18} color="var(--accent)" />
            <span style={{ color: 'var(--text-secondary)', fontSize: '0.82rem' }}>Faculty</span>
            <strong>{faculty.length}</strong>
          </div>
          <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '10px 14px' }}>
            <BriefcaseBusiness size={18} color="var(--success)" />
            <span style={{ color: 'var(--text-secondary)', fontSize: '0.82rem' }}>Verified</span>
            <strong>{verifiedCount}</strong>
          </div>
        </div>
      </div>

      <section className="card hod-faculty-directory">
        <div className="toolbar">
          <div className="toolbar-left">
            <div className="search-bar">
              <Search />
              <input
                aria-label="Search faculty"
                placeholder="Search name, faculty ID, department, or email..."
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
            </div>
          </div>
          <div className="toolbar-right">
            <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>
              {filteredFaculty.length} of {faculty.length} faculty members
            </span>
          </div>
        </div>

        {loading ? (
          <div className="loader"><div className="spinner" /></div>
        ) : error ? (
          <div className="alert alert-error" role="alert">{error}</div>
        ) : !filteredFaculty.length ? (
          <div className="empty-state">
            <Users />
            <h3>{faculty.length ? 'No faculty match your search' : 'No faculty found'}</h3>
            <p>{faculty.length ? 'Try another name, faculty ID, department, or email.' : 'No faculty members are associated with your appointed department(s).'}</p>
          </div>
        ) : (
          <div className="table-container">
            <table className="hod-faculty-table">
              <thead>
                <tr>
                  <th>Faculty Member</th>
                  <th>Department</th>
                  <th>Designation & Expertise</th>
                  <th>Contact</th>
                  <th>Experience</th>
                  <th>Employment</th>
                  <th>Account</th>
                </tr>
              </thead>
              <tbody>
                {filteredFaculty.map((member) => (
                  <tr key={member.id}>
                    <td>
                      <div className="hod-faculty-person">
                        <div className="hod-faculty-avatar" aria-hidden="true">{initials(member.name)}</div>
                        <div>
                          <strong>{member.name}</strong>
                          <div className="text-muted">
                            {member.facultyId}{member.role === 'HOD' ? ' · HOD' : ''}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td>
                      {(member.departments || []).length
                        ? <div className="hod-faculty-departments">{member.departments.map((department) => <span key={department}>{department}</span>)}</div>
                        : '—'}
                    </td>
                    <td>
                      <strong>{display(member.designation)}</strong>
                      <div className="text-muted">{[member.qualification, member.specialization].filter(Boolean).join(' · ') || '—'}</div>
                    </td>
                    <td>
                      {member.email ? <a className="hod-faculty-contact" href={`mailto:${member.email}`}><Mail size={14} />{member.email}</a> : <span className="text-muted">—</span>}
                      {member.phone && <a className="hod-faculty-contact" href={`tel:${member.phone}`}><Phone size={14} />{member.phone}</a>}
                    </td>
                    <td>{member.yearsOfExperience == null ? '—' : `${member.yearsOfExperience} years`}</td>
                    <td>
                      <strong>{display(member.employmentType)}</strong>
                      <div className="text-muted">{display(member.employmentStatus)}</div>
                    </td>
                    <td>
                      <span className="badge" style={verificationStyles[member.status] || {}}>
                        {member.status || 'Unknown'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </Layout>
  );
}
