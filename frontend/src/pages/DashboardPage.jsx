import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import { useAuth } from '../context/AuthContext';
import { getAllUsers } from '../api/users';
import { getAllDepartments } from '../api/departments';
import { getAllSubjects, getSubjectsByFaculty, getSubjectsByStudent } from '../api/subjects';
import { getAllAssignments, getAssignmentsByFaculty } from '../api/assignments';
import { getAllAttendance, getAttendanceByStudent } from '../api/attendance';
import {
  Users, Building2, BookOpen, ClipboardList,
  CalendarCheck, TrendingUp, Award, Clock,
  AlertTriangle, CheckCircle, PlusCircle, ArrowRight, XCircle,
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
  const { user, isAdmin, isHod, isFaculty, isStudent, isAdminOrHod } = useAuth();
  const navigate = useNavigate();
  const [stats, setStats] = useState({});
  const [loading, setLoading] = useState(true);
  const [recentAssignments, setRecentAssignments] = useState([]);
  const [myAttendanceRecords, setMyAttendanceRecords] = useState([]);

  useEffect(() => {
    const load = async () => {
      try {
        const promises = [
          isAdminOrHod ? getAllUsers().catch(() => ({ data: [] })) : Promise.resolve({ data: [] }),
          getAllDepartments().catch(() => ({ data: [] })),
          isFaculty
            ? getSubjectsByFaculty(user.id).catch(() => ({ data: [] }))
            : (isStudent ? getSubjectsByStudent(user.id).catch(() => ({ data: [] })) : getAllSubjects().catch(() => ({ data: [] }))),
          isFaculty ? getAssignmentsByFaculty(user.id).catch(() => ({ data: [] })) : getAllAssignments().catch(() => ({ data: [] })),
          isStudent ? getAttendanceByStudent(user.id).catch(() => ({ data: [] })) : getAllAttendance().catch(() => ({ data: [] })),
        ];

        const [uRes, dRes, sRes, aRes, attRes] = await Promise.all(promises);
        const users = uRes.data || [];
        const departments = dRes.data || [];
        const subjects = sRes.data || [];
        const assignments = aRes.data || [];
        const attendance = attRes.data || [];

        setMyAttendanceRecords(attendance);

        // HOD specific scoping
        const deptId = user.department?.id;
        const deptStudents = users.filter((u) => u.role === 'STUDENT' && (!deptId || u.department?.id === deptId));
        const deptFaculty = users.filter((u) => u.role === 'FACULTY' && (!deptId || u.department?.id === deptId));
        const deptSubjects = subjects.filter((s) => !deptId || s.department?.id === deptId);

        const studentSubjectIds = new Set(subjects.map((s) => s.id));
        const relevantAssignments = isStudent
          ? assignments.filter((a) => a.subject?.id && studentSubjectIds.has(a.subject.id))
          : assignments;

        const presentAtt = attendance.filter((a) => a.status === 'PRESENT').length;
        const totalAtt = attendance.length;
        const attPct = totalAtt ? Math.round((presentAtt / totalAtt) * 100) : 0;

        setStats({
          users: users.length,
          departments: departments.length,
          subjects: subjects.length,
          assignments: isStudent ? relevantAssignments.length : assignments.length,
          attendance: totalAtt,
          present: presentAtt,
          absent: attendance.filter((a) => a.status === 'ABSENT').length,
          students: users.filter((u) => u.role === 'STUDENT').length,
          faculty: users.filter((u) => u.role === 'FACULTY').length,
          deptStudents: deptStudents.length,
          deptFaculty: deptFaculty.length,
          deptSubjects: deptSubjects.length,
          overallPct: attPct,
        });

        setRecentAssignments([...relevantAssignments].sort((a, b) => b.id - a.id).slice(0, 5));
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [user, isAdminOrHod, isFaculty, isStudent]);

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

  const isLowAttendance = isStudent && stats.attendance > 0 && stats.overallPct < 75;

  return (
    <Layout pageTitle="Dashboard">
      {/* Greeting Banner */}
      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <h2 style={{ fontSize: '1.75rem', fontWeight: 800, marginBottom: 4 }}>
            {greet()}, {user.name.split(' ')[0]} 👋
          </h2>
          <p style={{ color: 'var(--text-secondary)' }}>
            {isAdmin && 'Institute Overview & Management Control'}
            {isHod && `${user.department?.name || 'Department'} Head Dashboard`}
            {isFaculty && 'Faculty Academic & Evaluation Dashboard'}
            {isStudent && 'Student Academic Progress & Assignments'}
          </p>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          {isFaculty && (
            <button className="btn btn-primary btn-sm" onClick={() => navigate('/attendance')}>
              <PlusCircle size={16} /> Mark Attendance
            </button>
          )}
          {isStudent && (
            <button className="btn btn-primary btn-sm" onClick={() => navigate('/assignments')}>
              <ClipboardList size={16} /> View Assignments
            </button>
          )}
        </div>
      </div>

      {/* Student Low Attendance Warning */}
      {isLowAttendance && (
        <div className="alert" style={{
          background: 'rgba(239, 68, 68, 0.1)',
          color: 'var(--danger)',
          border: '1px solid var(--danger)',
          marginBottom: 20,
          display: 'flex',
          alignItems: 'center',
          gap: 12,
        }}>
          <AlertTriangle size={22} />
          <div>
            <strong>Attendance Warning ({stats.overallPct}%)!</strong> Your attendance is below the minimum required 75% threshold. Please attend regular classes to avoid exam debarment.
          </div>
        </div>
      )}

      {/* ──────── Role Specific Stats ──────── */}
      <div className="stats-grid">
        {/* ADMIN DASHBOARD */}
        {isAdmin && (
          <>
            <StatCard icon={<Users size={22} />} value={stats.users} label="Total Users" color="var(--accent)" bg="var(--accent-light)" />
            <StatCard icon={<Building2 size={22} />} value={stats.departments} label="Departments" color="var(--info)" bg="var(--info-light)" />
            <StatCard icon={<Users size={22} />} value={stats.students} label="Total Students" color="var(--success)" bg="var(--success-light)" />
            <StatCard icon={<Award size={22} />} value={stats.faculty} label="Faculty Members" color="var(--warning)" bg="var(--warning-light)" />
            <StatCard icon={<BookOpen size={22} />} value={stats.subjects} label="Subjects" color="var(--accent)" bg="var(--accent-light)" />
            <StatCard icon={<ClipboardList size={22} />} value={stats.assignments} label="Assignments" color="var(--warning)" bg="var(--warning-light)" />
            <StatCard icon={<CalendarCheck size={22} />} value={stats.attendance} label="Attendance Sessions" color="var(--info)" bg="var(--info-light)" />
            <StatCard icon={<TrendingUp size={22} />} value={`${stats.overallPct}%`} label="Overall Attendance" color="var(--success)" bg="var(--success-light)" />
          </>
        )}

        {/* HOD DASHBOARD */}
        {isHod && (
          <>
            <StatCard icon={<Users size={22} />} value={stats.deptStudents} label="Dept Students" color="var(--success)" bg="var(--success-light)" />
            <StatCard icon={<Award size={22} />} value={stats.deptFaculty} label="Dept Faculty" color="var(--warning)" bg="var(--warning-light)" />
            <StatCard icon={<BookOpen size={22} />} value={stats.deptSubjects} label="Dept Subjects" color="var(--accent)" bg="var(--accent-light)" />
            <StatCard icon={<ClipboardList size={22} />} value={stats.assignments} label="Assignments" color="var(--warning)" bg="var(--warning-light)" />
            <StatCard icon={<CalendarCheck size={22} />} value={stats.attendance} label="Classes Recorded" color="var(--info)" bg="var(--info-light)" />
            <StatCard icon={<TrendingUp size={22} />} value={`${stats.overallPct}%`} label="Dept Attendance" color="var(--success)" bg="var(--success-light)" />
          </>
        )}

        {/* FACULTY DASHBOARD */}
        {isFaculty && (
          <>
            <StatCard icon={<BookOpen size={22} />} value={stats.subjects} label="My Assigned Subjects" color="var(--accent)" bg="var(--accent-light)" />
            <StatCard icon={<CalendarCheck size={22} />} value={stats.attendance} label="Sessions Conducted" color="var(--info)" bg="var(--info-light)" />
            <StatCard icon={<ClipboardList size={22} />} value={stats.assignments} label="Assignments Created" color="var(--warning)" bg="var(--warning-light)" />
            <StatCard icon={<TrendingUp size={22} />} value={`${stats.overallPct}%`} label="Class Attendance Rate" color="var(--success)" bg="var(--success-light)" />
          </>
        )}

        {/* STUDENT DASHBOARD */}
        {isStudent && (
          <>
            <StatCard
              icon={<TrendingUp size={22} />}
              value={`${stats.overallPct}%`}
              label="Overall Attendance"
              color={stats.overallPct < 75 ? 'var(--danger)' : 'var(--success)'}
              bg={stats.overallPct < 75 ? 'var(--danger-light)' : 'var(--success-light)'}
            />
            <StatCard icon={<CheckCircle size={22} />} value={stats.present} label="Attended Classes" color="var(--success)" bg="var(--success-light)" />
            <StatCard icon={<XCircle size={22} />} value={stats.absent} label="Missed Classes" color="var(--danger)" bg="var(--danger-light)" />
            <StatCard icon={<ClipboardList size={22} />} value={stats.assignments} label="Available Assignments" color="var(--accent)" bg="var(--accent-light)" />
          </>
        )}
      </div>

      {/* Recent Assignments / Submissions */}
      {recentAssignments.length > 0 && (
        <div className="card" style={{ marginTop: 20 }}>
          <div className="card-header">
            <div>
              <div className="card-title">Recent Assignments</div>
              <div className="card-subtitle">
                {isFaculty ? 'Recent assignments you have created' : 'Latest assignments posted by faculty'}
              </div>
            </div>
            <button className="btn btn-secondary btn-sm" onClick={() => navigate('/assignments')}>
              View All <ArrowRight size={14} />
            </button>
          </div>
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Subject</th>
                  <th>Faculty</th>
                  <th>Max Marks</th>
                  <th>Due Date</th>
                </tr>
              </thead>
              <tbody>
                {recentAssignments.map((a) => (
                  <tr key={a.id}>
                    <td style={{ fontWeight: 600 }}>{a.title}</td>
                    <td style={{ color: 'var(--text-secondary)' }}>{a.subject?.name} ({a.subject?.code})</td>
                    <td style={{ color: 'var(--text-secondary)' }}>{a.faculty?.name}</td>
                    <td style={{ fontWeight: 600 }}>{a.maxMarks || 100}</td>
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

      {/* Quick Navigation Cards */}
      <div className="grid-2" style={{ marginTop: 20 }}>
        <div className="card" style={{ cursor: 'pointer' }} onClick={() => navigate('/subjects')}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div className="stat-icon" style={{ background: 'var(--accent-light)' }}>
              <BookOpen size={22} color="var(--accent)" />
            </div>
            <div>
              <div className="card-title">Subjects</div>
              <div className="card-subtitle">
                {isFaculty ? 'View your course allocations' : 'Browse subjects and curriculum'}
              </div>
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
              <div className="card-subtitle">
                {isFaculty ? 'Record student attendance' : (isStudent ? 'Track personal attendance history' : 'Monitor institute attendance')}
              </div>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
}
