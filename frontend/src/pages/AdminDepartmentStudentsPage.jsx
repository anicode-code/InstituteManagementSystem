import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import DepartmentSectionNav from '../components/DepartmentSectionNav';
import Layout from '../components/Layout';
import SortableTh from '../components/SortableTh';
import { useTableSorting } from '../utils/tableSorting';
import {
  getAdminDepartmentStudentProfile,
  getAdminDepartmentStudents,
} from '../api/adminDepartmentStudents';
import {
  ArrowLeft,
  BookOpen,
  CalendarCheck,
  Search,
  ShieldAlert,
  Users,
} from 'lucide-react';

const YEARS = [
  { value: 1, label: 'Year 1' },
  { value: 2, label: 'Year 2' },
  { value: 3, label: 'Year 3' },
  { value: 4, label: 'Year 4' },
];

const SEMESTERS_BY_YEAR = {
  1: [1, 2],
  2: [3, 4],
  3: [5, 6],
  4: [7, 8],
};

const tableHeaderStyle = {
  textAlign: 'left',
  padding: '10px 12px',
  color: 'var(--text-muted)',
  fontSize: '0.7rem',
  textTransform: 'uppercase',
  letterSpacing: '0.04em',
  borderBottom: '1px solid var(--border)',
  whiteSpace: 'nowrap',
};

const tableCellStyle = {
  padding: '10px 12px',
  borderBottom: '1px solid var(--border)',
  verticalAlign: 'middle',
};

function formatPercent(value) {
  return value == null ? 'N/A' : `${Number(value).toFixed(1)}%`;
}

function formatPerformance(value) {
  return value == null ? 'N/A' : `${Number(value).toFixed(1)} / 10`;
}

function display(value) {
  return value == null || value === '' ? 'N/A' : value;
}

function initials(name) {
  return name?.split(/\s+/).filter(Boolean).map((part) => part[0]).join('').toUpperCase().slice(0, 2) || '?';
}

function errorMessage(error) {
  return error?.response?.data?.message || 'Could not load student information. Please try again.';
}

function SummaryCard({ label, value, icon, detail }) {
  return (
    <article className="card" style={{ padding: 14, minWidth: 0 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'flex-start' }}>
        <div style={{ minWidth: 0 }}>
          <div style={{ color: 'var(--text-muted)', fontSize: '0.7rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            {label}
          </div>
          <div style={{ marginTop: 6, fontSize: '1.35rem', fontWeight: 800 }}>{value}</div>
          {detail && <div style={{ marginTop: 3, color: 'var(--text-muted)', fontSize: '0.72rem' }}>{detail}</div>}
        </div>
        <div className="stat-icon" style={{ width: 36, height: 36, flexShrink: 0 }}>{icon}</div>
      </div>
    </article>
  );
}

function Metric({ label, value }) {
  return (
    <div style={{ padding: 12, background: 'var(--bg-secondary)', borderRadius: 8, minWidth: 0 }}>
      <div style={{ color: 'var(--text-muted)', fontSize: '0.68rem', textTransform: 'uppercase', marginBottom: 4 }}>{label}</div>
      <div style={{ fontWeight: 700, overflowWrap: 'anywhere' }}>{display(value)}</div>
    </div>
  );
}

function StudentProfile({ departmentId, studentId }) {
  const navigate = useNavigate();
  const { getSorts, getSortedRows, toggleSort } = useTableSorting();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const studentsPath = `/departments/${departmentId}/students`;

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    getAdminDepartmentStudentProfile(departmentId, studentId)
      .then((response) => {
        if (active) setProfile(response.data);
      })
      .catch((requestError) => {
        if (active) setError(errorMessage(requestError));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, [departmentId, studentId]);

  const personal = profile?.personalInformation;
  const academic = profile?.academicInformation;
  const summary = profile?.academicSummary;
  const semesterSgpaByNumber = new Map(
    (profile?.semesterGpas || []).map((result) => [result.semester, result.sgpa]),
  );
  const yearlyGpaByYear = new Map(
    (profile?.yearlyGpas || []).map((result) => [result.year, result.ygpa]),
  );
  const gpaYears = [...new Set([
    ...[...semesterSgpaByNumber.keys()].map((semester) => Math.ceil(semester / 2)),
    ...yearlyGpaByYear.keys(),
  ])].sort((a, b) => a - b);

  return (
    <Layout pageTitle="Student Profile">
      <DepartmentSectionNav departmentId={departmentId} />
      <div style={{ marginBottom: 16 }}>
        <button className="btn btn-secondary btn-sm" type="button" onClick={() => navigate(studentsPath)}>
          <ArrowLeft size={15} /> Back to Students
        </button>
      </div>
      {loading ? <div className="loader"><div className="spinner" /></div> : null}
      {!loading && error && <div className="alert alert-error" role="alert">{error}</div>}
      {!loading && profile && (
        <>
          <section className="card" style={{ marginBottom: 16, padding: 16 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 14, flexWrap: 'wrap' }}>
              {personal.photoUrl ? (
                <img
                  src={personal.photoUrl}
                  alt=""
                  className="avatar lg"
                  style={{ objectFit: 'cover' }}
                />
              ) : (
                <div className="avatar lg" aria-hidden="true">{initials(personal.name)}</div>
              )}
              <div style={{ flex: 1, minWidth: 180 }}>
                <h2 style={{ fontSize: '1.35rem', fontWeight: 800, marginBottom: 3 }}>{display(personal.name)}</h2>
                <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                  {display(academic.rollNumber)} · {display(academic.departmentName)}
                </div>
              </div>
              <span className="badge" style={{
                background: academic.accountStatus === 'VERIFIED' ? 'var(--success-light)' : 'var(--warning-light)',
                color: academic.accountStatus === 'VERIFIED' ? 'var(--success)' : 'var(--warning)',
              }}>
                {display(academic.accountStatus)}
              </span>
            </div>
          </section>

          <section className="card" style={{ marginBottom: 16 }}>
            <h3 className="card-title" style={{ marginBottom: 12 }}>Personal Information</h3>
            <div className="grid-3">
              <Metric label="Email" value={personal.email} />
              <Metric label="Phone" value={personal.phone} />
              <Metric label="Date of birth" value={personal.dateOfBirth} />
              <Metric label="Address" value={personal.address} />
              <Metric label="Photo" value={personal.photoUrl ? 'Available' : null} />
              <Metric label="Registered" value={academic.registeredAt ? new Date(academic.registeredAt).toLocaleDateString() : null} />
            </div>
          </section>

          <section className="card" style={{ marginBottom: 16 }}>
            <h3 className="card-title" style={{ marginBottom: 12 }}>Academic Information</h3>
            <div className="grid-3">
              <Metric label="Roll number" value={academic.rollNumber} />
              <Metric label="Registration number" value={academic.registrationNumber} />
              <Metric label="Department" value={academic.departmentName} />
              <Metric label="Department code" value={academic.departmentCode} />
              <Metric label="Admission year" value={academic.admissionYear} />
              <Metric label="Current year" value={academic.year ? `Year ${academic.year}` : null} />
              <Metric label="Semester" value={academic.semester ? `Semester ${academic.semester}` : null} />
              <Metric label="Account status" value={academic.accountStatus} />
            </div>
          </section>

          <section className="card" style={{ marginBottom: 16 }}>
            <h3 className="card-title" style={{ marginBottom: 12 }}>Semester and Yearly GPA</h3>
            {!gpaYears.length ? (
              <div className="empty-state"><p>No semester SGPA values have been entered.</p></div>
            ) : (
              <div style={{ display: 'grid', gap: 12 }}>
                {gpaYears.map((year) => {
                  const oddSemester = year * 2 - 1;
                  const evenSemester = year * 2;
                  return (
                    <div
                      key={year}
                      style={{
                        padding: 14,
                        border: '1px solid var(--border)',
                        borderRadius: 10,
                        background: 'var(--bg-primary)',
                      }}
                    >
                      <h4 style={{ fontSize: '0.9rem', fontWeight: 700, marginBottom: 10 }}>Year {year}</h4>
                      <div className="student-year-gpa-row">
                        <div className="student-semester-pair">
                          <Metric label={`Semester ${oddSemester} SGPA`} value={semesterSgpaByNumber.get(oddSemester)} />
                          <Metric label={`Semester ${evenSemester} SGPA`} value={semesterSgpaByNumber.get(evenSemester)} />
                        </div>
                        <div style={{
                          padding: 12,
                          border: '1px solid var(--accent)',
                          borderRadius: 8,
                          background: 'var(--accent-light)',
                          minWidth: 0,
                        }}>
                          <div style={{ color: 'var(--accent)', fontSize: '0.68rem', fontWeight: 700, textTransform: 'uppercase', marginBottom: 4 }}>
                            Yearly GPA (YGPA)
                          </div>
                          <div style={{ fontSize: '1.2rem', fontWeight: 800, overflowWrap: 'anywhere' }}>
                            {display(yearlyGpaByYear.get(year))}
                          </div>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </section>

          <section className="card" style={{ marginBottom: 16 }}>
            <h3 className="card-title" style={{ marginBottom: 12 }}>Academic Summary</h3>
            <div className="stats-grid">
              <Metric label="Overall attendance" value={formatPercent(summary.overallAttendance)} />
              <Metric label="Classes attended" value={summary.attendedClasses} />
              <Metric label="Classes missed" value={summary.missedClasses} />
              <Metric label="Total classes" value={summary.totalClasses} />
              <Metric label="Academic performance" value={formatPerformance(summary.academicPerformance)} />
              <Metric label="Assignments submitted" value={`${summary.submittedAssignments} / ${summary.totalAssignments}`} />
              <Metric label="Pending assignments" value={summary.pendingAssignments} />
            </div>
          </section>

          <section className="card" style={{ marginBottom: 16 }}>
            <div className="card-header">
              <div><h3 className="card-title">Subject-wise Performance</h3><p className="card-subtitle">Calculated from recorded attendance and performance marks.</p></div>
            </div>
            {!profile.subjectPerformance.length ? (
              <div className="empty-state"><p>No enrolled subjects are recorded.</p></div>
            ) : (
              <div className="table-container">
                <table>
                  <thead><tr>
                    <SortableTh tableId="student-subject-performance" columnKey="subject" label="Subject" sorts={getSorts('student-subject-performance')} onSort={toggleSort} />
                    <SortableTh tableId="student-subject-performance" columnKey="code" label="Code" sorts={getSorts('student-subject-performance')} onSort={toggleSort} />
                    <SortableTh tableId="student-subject-performance" columnKey="attendancePercentage" label="Attendance" sorts={getSorts('student-subject-performance')} onSort={toggleSort} />
                    <SortableTh tableId="student-subject-performance" columnKey="averageMarks" label="Marks (/10)" sorts={getSorts('student-subject-performance')} onSort={toggleSort} />
                    <SortableTh tableId="student-subject-performance" columnKey="grade" label="Grade" sorts={getSorts('student-subject-performance')} onSort={toggleSort} />
                  </tr></thead>
                  <tbody>
                    {getSortedRows('student-subject-performance', profile.subjectPerformance).map((subject) => (
                      <tr key={subject.subjectId}>
                        <td>{subject.subject}</td>
                        <td>{subject.code}</td>
                        <td>{subject.attendedClasses} / {subject.totalClasses} · {formatPercent(subject.attendancePercentage)}</td>
                        <td>{formatPerformance(subject.averageMarks)}</td>
                        <td>{display(subject.grade)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section className="card" style={{ marginBottom: 16 }}>
            <div className="card-header">
              <div><h3 className="card-title">Attendance Details</h3><p className="card-subtitle">Date-wise recorded attendance.</p></div>
            </div>
            {!profile.attendanceDetails.length ? (
              <div className="empty-state"><p>No attendance records are available.</p></div>
            ) : (
              <div className="table-container">
                <table>
                  <thead><tr>
                    <SortableTh tableId="student-attendance" columnKey="date" label="Date" sorts={getSorts('student-attendance')} onSort={toggleSort} />
                    <SortableTh tableId="student-attendance" columnKey="subject" label="Subject" sorts={getSorts('student-attendance')} onSort={toggleSort} />
                    <SortableTh tableId="student-attendance" columnKey="code" label="Code" sorts={getSorts('student-attendance')} onSort={toggleSort} />
                    <SortableTh tableId="student-attendance" columnKey="status" label="Status" sorts={getSorts('student-attendance')} onSort={toggleSort} />
                    <SortableTh tableId="student-attendance" columnKey="performanceMarks" label="Performance marks" sorts={getSorts('student-attendance')} onSort={toggleSort} />
                  </tr></thead>
                  <tbody>
                    {getSortedRows('student-attendance', profile.attendanceDetails).map((record) => (
                      <tr key={record.id}>
                        <td>{record.date || 'N/A'}</td>
                        <td>{record.subject}</td>
                        <td>{record.code}</td>
                        <td>{record.status}</td>
                        <td>{display(record.performanceMarks)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section className="card" style={{ marginBottom: 16 }}>
            <div className="card-header">
              <div><h3 className="card-title">Assignment History</h3><p className="card-subtitle">Assignments for the student's enrolled subjects.</p></div>
            </div>
            {!profile.assignmentHistory.length ? (
              <div className="empty-state"><p>No assignment records are available.</p></div>
            ) : (
              <div className="table-container">
                <table>
                  <thead><tr>
                    <SortableTh tableId="student-assignments" columnKey="assignment" label="Assignment" sorts={getSorts('student-assignments')} onSort={toggleSort} />
                    <SortableTh tableId="student-assignments" columnKey="subject" label="Subject" sorts={getSorts('student-assignments')} onSort={toggleSort} />
                    <SortableTh tableId="student-assignments" columnKey="dueDate" label="Due date" sorts={getSorts('student-assignments')} onSort={toggleSort} />
                    <SortableTh tableId="student-assignments" columnKey="submissionStatus" label="Status" sorts={getSorts('student-assignments')} onSort={toggleSort} />
                    <SortableTh tableId="student-assignments" columnKey="submittedAt" label="Submitted" sorts={getSorts('student-assignments')} onSort={toggleSort} />
                    <SortableTh tableId="student-assignments" columnKey="marks" label="Marks" sorts={getSorts('student-assignments')} onSort={toggleSort} />
                  </tr></thead>
                  <tbody>
                    {getSortedRows('student-assignments', profile.assignmentHistory).map((assignment) => (
                      <tr key={assignment.id}>
                        <td>{assignment.assignment}</td>
                        <td>{assignment.subject} ({assignment.code})</td>
                        <td>{assignment.dueDate || 'N/A'}</td>
                        <td>{assignment.submissionStatus}</td>
                        <td>{assignment.submittedAt ? new Date(assignment.submittedAt).toLocaleDateString() : 'N/A'}</td>
                        <td>{assignment.marks == null ? 'N/A' : `${assignment.marks} / ${assignment.maxMarks}`}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section className="card">
            <h3 className="card-title" style={{ marginBottom: 8 }}>Examination Results</h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
              Examination, SGPA, and CGPA records are not available in the current system.
            </p>
          </section>
        </>
      )}
    </Layout>
  );
}

function StudentsList({ departmentId }) {
  const navigate = useNavigate();
  const { getSorts, toggleSort } = useTableSorting();
  const [search, setSearch] = useState('');
  const [year, setYear] = useState('');
  const [semester, setSemester] = useState('');
  const [status, setStatus] = useState('');
  const [attention, setAttention] = useState('');
  const sorts = getSorts('student-roster');
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    const timer = window.setTimeout(() => {
      setLoading(true);
      setError('');
      getAdminDepartmentStudents(departmentId, {
        ...(year ? { year: Number(year) } : {}),
        ...(semester ? { semester: Number(semester) } : {}),
        ...(status ? { status } : {}),
        ...(search.trim() ? { search: search.trim() } : {}),
        ...(attention ? { attention } : {}),
        ...(sorts.length ? { sort: sorts.map(({ key, direction }) => `${key}:${direction}`).join(',') } : {}),
        page,
        size: 25,
      })
        .then((response) => {
          if (active) setResult(response.data);
        })
        .catch((requestError) => {
          if (active) {
            setResult(null);
            setError(errorMessage(requestError));
          }
        })
        .finally(() => {
          if (active) setLoading(false);
        });
    }, search ? 250 : 0);
    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [departmentId, year, semester, status, search, attention, sorts, page]);

  const semesterOptions = useMemo(
    () => (year ? SEMESTERS_BY_YEAR[year] || [] : []),
    [year],
  );
  const summary = result?.summary;
  const totalPages = result?.totalPages || 0;
  const pageNumber = result ? result.page + 1 : 0;

  const changeFilter = (setter) => (event) => {
    setter(event.target.value);
    setPage(0);
  };
  const handleSort = (tableId, key) => {
    toggleSort(tableId, key);
    setPage(0);
  };

  const openStudent = (studentId) => {
    navigate(`/departments/${departmentId}/students/${studentId}`);
  };

  return (
    <Layout pageTitle="Department Students">
      <DepartmentSectionNav departmentId={departmentId} />
      <div className="page-header" style={{ marginBottom: 14 }}>
        <div>
          <h2 className="page-title">Students</h2>
          <p className="page-subtitle">Students in this department only. Account status is shown as Student Status.</p>
        </div>
      </div>

      <div className="stats-grid" style={{ marginBottom: 14 }}>
        <SummaryCard label="Total Students" value={summary?.totalStudents ?? '—'} icon={<Users size={18} color="var(--accent)" />} />
        <SummaryCard
          label="Average Attendance"
          value={summary ? formatPercent(summary.averageAttendance) : '—'}
          icon={<CalendarCheck size={18} color="var(--accent)" />}
          detail={result ? `Attention threshold: ${result.attendanceThreshold}%` : null}
        />
        <SummaryCard
          label="Average Academic Performance"
          value={summary ? formatPerformance(summary.averageAcademicPerformance) : '—'}
          icon={<BookOpen size={18} color="var(--accent)" />}
          detail={result ? `Attention threshold: below ${result.performanceThreshold}/10` : null}
        />
        <SummaryCard
          label="Students Requiring Attention"
          value={summary?.studentsRequiringAttention ?? '—'}
          icon={<ShieldAlert size={18} color="var(--warning)" />}
          detail="Low attendance, performance, pending work, or decline"
        />
      </div>

      <section className="card" style={{ marginBottom: 14, padding: 12 }}>
        <div className="admin-student-filters">
          <div className="search-bar" style={{ minWidth: 0 }}>
            <Search />
            <input
              aria-label="Search students"
              placeholder="Name, roll/enrollment no., email"
              value={search}
              onChange={(event) => { setSearch(event.target.value); setPage(0); }}
            />
          </div>
          <select className="form-select" aria-label="Filter by year" value={year} onChange={(event) => {
            setYear(event.target.value);
            setSemester('');
            setPage(0);
          }}>
            <option value="">All years</option>
            {YEARS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
          </select>
          <select className="form-select" aria-label="Filter by semester" value={semester} disabled={!year} onChange={changeFilter(setSemester)}>
            <option value="">All semesters</option>
            {semesterOptions.map((value) => <option key={value} value={value}>Semester {value}</option>)}
          </select>
          <select className="form-select" aria-label="Filter by student status" value={status} onChange={changeFilter(setStatus)}>
            <option value="">All statuses</option>
            <option value="VERIFIED">Verified</option>
            <option value="PENDING">Pending verification</option>
          </select>
          <select className="form-select" aria-label="Filter students requiring attention" value={attention} onChange={changeFilter(setAttention)}>
            <option value="">All attention levels</option>
            <option value="LOW_ATTENDANCE">Low attendance</option>
            <option value="POOR_PERFORMANCE">Poor performance</option>
            <option value="PENDING_ASSIGNMENTS">2+ pending assignments</option>
            <option value="DECLINING_PERFORMANCE">Declining performance</option>
          </select>
        </div>
        <p style={{ margin: '8px 0 0', color: 'var(--text-muted)', fontSize: '0.75rem' }}>
          Click column headings to sort; click again to reverse, and a third time to restore the default order.
        </p>
      </section>

      {loading && <div className="loader"><div className="spinner" /></div>}
      {!loading && error && <div className="alert alert-error" role="alert">{error}</div>}
      {!loading && !error && result?.students.length === 0 && (
        <div className="empty-state">
          <Users size={30} />
          <h3>No students found</h3>
          <p>No students match the current search and filters.</p>
        </div>
      )}
      {!loading && !error && result?.students.length > 0 && (
        <section className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <div className="table-container">
            <table style={{ minWidth: 1080 }}>
              <thead>
                <tr>
                  <SortableTh tableId="student-roster" columnKey="rollNumber" label="Roll number" sorts={sorts} onSort={handleSort} style={tableHeaderStyle} />
                  <SortableTh tableId="student-roster" columnKey="name" label="Student" sorts={sorts} onSort={handleSort} style={tableHeaderStyle} />
                  <SortableTh tableId="student-roster" columnKey="yearSemester" label="Year / Semester" sorts={sorts} onSort={handleSort} style={tableHeaderStyle} />
                  <SortableTh tableId="student-roster" columnKey="attendance" label="Attendance" sorts={sorts} onSort={handleSort} style={tableHeaderStyle} />
                  <SortableTh tableId="student-roster" columnKey="performance" label="Performance" sorts={sorts} onSort={handleSort} style={tableHeaderStyle} />
                  <SortableTh tableId="student-roster" columnKey="assignments" label="Assignments (pending)" sorts={sorts} onSort={handleSort} style={tableHeaderStyle} />
                  <SortableTh tableId="student-roster" columnKey="status" label="Status" sorts={sorts} onSort={handleSort} style={tableHeaderStyle} />
                  <SortableTh tableId="student-roster" columnKey="attention" label="Attention" sorts={sorts} onSort={handleSort} style={tableHeaderStyle} />
                  <th style={tableHeaderStyle}></th>
                </tr>
              </thead>
              <tbody>
                {result.students.map((student) => (
                  <tr
                    key={student.id}
                    className="admin-student-row"
                    tabIndex={0}
                    role="link"
                    aria-label={`View ${student.name}'s profile`}
                    onClick={() => openStudent(student.id)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault();
                        openStudent(student.id);
                      }
                    }}
                    style={{ cursor: 'pointer' }}
                  >
                    <td style={tableCellStyle}>{student.rollNumber}</td>
                    <td style={tableCellStyle}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 9 }}>
                        <div className="avatar sm" aria-hidden="true">{initials(student.name)}</div>
                        <span style={{ fontWeight: 600 }}>{student.name}</span>
                      </div>
                    </td>
                    <td style={tableCellStyle}>
                      {student.year ? `Year ${student.year}` : 'N/A'}
                      {' / '}
                      {student.semester ? `Sem ${student.semester}` : 'N/A'}
                    </td>
                    <td style={tableCellStyle}>{formatPercent(student.attendancePercentage)}</td>
                    <td style={tableCellStyle}>{formatPerformance(student.academicPerformance)}</td>
                    <td style={tableCellStyle}>{student.submittedAssignments} / {student.totalAssignments}</td>
                    <td style={tableCellStyle}>
                      <span className="badge" style={{
                        background: student.status === 'VERIFIED' ? 'var(--success-light)' : 'var(--warning-light)',
                        color: student.status === 'VERIFIED' ? 'var(--success)' : 'var(--warning)',
                      }}>
                        {student.status === 'VERIFIED' ? 'Verified' : 'Pending'}
                      </span>
                    </td>
                    <td style={tableCellStyle}>
                      {student.requiresAttention ? (
                        <span title={[
                          student.lowAttendance && 'Low attendance',
                          student.poorPerformance && 'Poor performance',
                          student.pendingAssignments >= 2 && 'Multiple pending assignments',
                          student.decliningPerformance && 'Declining performance',
                        ].filter(Boolean).join(', ')} style={{ color: 'var(--warning)', fontSize: '0.75rem', fontWeight: 700 }}>
                          <ShieldAlert size={15} style={{ verticalAlign: 'middle', marginRight: 4 }} /> Review
                        </span>
                      ) : <span style={{ color: 'var(--text-muted)' }}>—</span>}
                    </td>
                    <td style={tableCellStyle}>
                      <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        onClick={(event) => { event.stopPropagation(); openStudent(student.id); }}
                      >
                        View Details
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 14px', gap: 8, flexWrap: 'wrap', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
            <span>{result.totalElements} student{result.totalElements === 1 ? '' : 's'} · Page {pageNumber} of {totalPages || 1}</span>
            <div style={{ display: 'flex', gap: 8 }}>
              <button type="button" className="btn btn-secondary btn-sm" disabled={page <= 0} onClick={() => setPage((current) => current - 1)}>Previous</button>
              <button type="button" className="btn btn-secondary btn-sm" disabled={page + 1 >= totalPages} onClick={() => setPage((current) => current + 1)}>Next</button>
            </div>
          </div>
        </section>
      )}
    </Layout>
  );
}

export default function AdminDepartmentStudentsPage({ departmentId, studentId }) {
  return studentId
    ? <StudentProfile departmentId={departmentId} studentId={studentId} />
    : <StudentsList departmentId={departmentId} />;
}
