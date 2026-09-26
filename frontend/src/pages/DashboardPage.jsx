import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import { useAuth } from '../context/AuthContext';
import { getAllUsers } from '../api/users';
import { getAllDepartments } from '../api/departments';
import { getAllSubjects } from '../api/subjects';
import { getAllAssignments } from '../api/assignments';
import { getAllAttendance } from '../api/attendance';
import {
  Users, Building2, BookOpen, ClipboardList,
  CalendarCheck, TrendingUp, Award, Clock,
} from 'lucide-react';

function StatCard({ icon, value, label, color, bg }) {
  return (
    <div className="stat-card">
      <div className="stat-icon" style={{ background: bg }}>
        <span style={{ color }}>{icon}</span>
      </div>
      <div>
        <div className="stat-value">{value ?? '—'}</div>
        <div className="stat-label">{label}</div>
      </div>
    </div>
  );
}

export default function DashboardPage() {
  const { user, isAdminOrHod, isFaculty, isStudent } = useAuth();
  const navigate = useNavigate();
  const [stats, setStats] = useState({});
  const [loading, setLoading] = useState(true);
  const [recentAssignments, setRecentAssignments] = useState([]);

  useEffect(() => {
    const load = async () => {
      try {
        const results = await Promise.allSettled([
          getAllUsers(),
          getAllDepartments(),
          getAllSubjects(),
          getAllAssignments(),
          getAllAttendance(),
        ]);
        const users       = results[0].status === 'fulfilled' ? results[0].value.data : [];
        const departments = results[1].status === 'fulfilled' ? results[1].value.data : [];
        const subjects    = results[2].status === 'fulfilled' ? results[2].value.data : [];
        const assignments = results[3].status === 'fulfilled' ? results[3].value.data : [];
        const attendance  = results[4].status === 'fulfilled' ? results[4].value.data : [];

        setStats({
          users:       users.length,
          departments: departments.length,
          subjects:    subjects.length,
          assignments: assignments.length,
          attendance:  attendance.length,
          students:    users.filter((u) => u.role === 'STUDENT').length,
          faculty:     users.filter((u) => u.role === 'FACULTY').length,
          present:     attendance.filter((a) => a.status === 'PRESENT').length,
        });

        // Show 5 most recent assignments (sorted by id desc as proxy)
        setRecentAssignments([...assignments].sort((a, b) => b.id - a.id).slice(0, 5));
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const greet = () => {
    const h = new Date().getHours();
    if (h < 12) return 'Good morning';
    if (h < 17) return 'Good afternoon';
    return 'Good evening';
  };

  if (loading) {
    return (
      <Layout pageTitle="Dashboard">
        <div className="loader"><div className="spinner" /></div>
      </Layout>
    );
  }

  const attendancePct = stats.attendance
    ? Math.round((stats.present / stats.attendance) * 100)
    : 0;

  return (
    <Layout pageTitle="Dashboard">
      {/* Greeting */}
      <div style={{ marginBottom: 28 }}>
        <h2 style={{ fontSize: '1.75rem', fontWeight: 800, marginBottom: 4 }}>
          {greet()}, {user.name.split(' ')[0]} 👋
        </h2>
        <p style={{ color: 'var(--text-secondary)' }}>
          Here's what's happening in your institute today.
        </p>
      </div>

      {/* Stats */}
      <div className="stats-grid">
        {isAdminOrHod && (
          <>
            <StatCard icon={<Users size={22} />}    value={stats.users}       label="Total Users"      color="var(--accent)"  bg="var(--accent-light)" />
            <StatCard icon={<Building2 size={22} />} value={stats.departments} label="Departments"      color="var(--info)"    bg="var(--info-light)" />
            <StatCard icon={<Users size={22} />}    value={stats.students}    label="Students"         color="var(--success)" bg="var(--success-light)" />
            <StatCard icon={<Award size={22} />}    value={stats.faculty}     label="Faculty Members"  color="var(--warning)" bg="var(--warning-light)" />
          </>
        )}
        <StatCard icon={<BookOpen size={22} />}    value={stats.subjects}    label="Subjects"         color="var(--accent)"  bg="var(--accent-light)" />
        <StatCard icon={<ClipboardList size={22} />} value={stats.assignments} label="Assignments"    color="var(--warning)" bg="var(--warning-light)" />
        <StatCard icon={<CalendarCheck size={22} />} value={stats.attendance}  label="Attendance Records" color="var(--info)" bg="var(--info-light)" />
        {isAdminOrHod && (
          <StatCard icon={<TrendingUp size={22} />} value={`${attendancePct}%`} label="Overall Attendance" color="var(--success)" bg="var(--success-light)" />
        )}
      </div>

      {/* Recent Assignments */}
      {recentAssignments.length > 0 && (
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Recent Assignments</div>
              <div className="card-subtitle">Latest 5 assignments across all subjects</div>
            </div>
            <button className="btn btn-secondary btn-sm" onClick={() => navigate('/assignments')}>
              View All
            </button>
          </div>
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Subject</th>
                  <th>Faculty</th>
                  <th>Due Date</th>
                </tr>
              </thead>
              <tbody>
                {recentAssignments.map((a) => (
                  <tr key={a.id}>
                    <td style={{ fontWeight: 600 }}>{a.title}</td>
                    <td style={{ color: 'var(--text-secondary)' }}>{a.subject?.name}</td>
                    <td style={{ color: 'var(--text-secondary)' }}>{a.faculty?.name}</td>
                    <td>
                      {a.dueDate ? (
                        <span style={{
                          color: new Date(a.dueDate) < new Date() ? 'var(--danger)' : 'var(--success)',
                          fontWeight: 600,
                        }}>
                          {a.dueDate}
                        </span>
                      ) : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Quick Links */}
      <div className="grid-2" style={{ marginTop: 20 }}>
        <div className="card" style={{ cursor: 'pointer' }} onClick={() => navigate('/subjects')}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div className="stat-icon" style={{ background: 'var(--accent-light)' }}>
              <BookOpen size={22} color="var(--accent)" />
            </div>
            <div>
              <div className="card-title">Subjects</div>
              <div className="card-subtitle">Browse all subjects</div>
            </div>
          </div>
        </div>
        <div className="card" style={{ cursor: 'pointer' }} onClick={() => navigate('/attendance')}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div className="stat-icon" style={{ background: 'var(--success-light)' }}>
              <CalendarCheck size={22} color="var(--success)" />
            </div>
            <div>
              <div className="card-title">Attendance</div>
              <div className="card-subtitle">View & manage attendance</div>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
}
