import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, BookOpen, CalendarCheck, Search, Users } from 'lucide-react';
import DepartmentSectionNav from '../components/DepartmentSectionNav';
import Layout from '../components/Layout';
import SortableTh from '../components/SortableTh';
import { useTableSorting } from '../utils/tableSorting';
import { getAdminDepartmentFaculty, getAdminDepartmentFacultyProfile } from '../api/adminDepartmentFaculty';

const HEADER_STYLE = {
  textAlign: 'left',
  padding: '10px 12px',
  color: 'var(--text-muted)',
  fontSize: '0.7rem',
  textTransform: 'uppercase',
  letterSpacing: '0.04em',
  borderBottom: '1px solid var(--border)',
  whiteSpace: 'nowrap',
};

const CELL_STYLE = { padding: '10px 12px', borderBottom: '1px solid var(--border)', verticalAlign: 'middle' };

function display(value) {
  return value == null || value === '' ? 'N/A' : value;
}

function formatPercent(value) {
  return value == null ? 'N/A' : `${Number(value).toFixed(1)}%`;
}

function formatPerformance(value) {
  return value == null ? 'N/A' : `${Number(value).toFixed(1)} / 10`;
}

function formatDate(value) {
  return value ? new Date(value).toLocaleDateString() : 'N/A';
}

function initials(name) {
  return name?.split(/\s+/).filter(Boolean).map((part) => part[0]).join('').toUpperCase().slice(0, 2) || '?';
}

function errorMessage(error) {
  return error?.response?.data?.message || 'Could not load faculty information. Please try again.';
}

function Metric({ label, value }) {
  return (
    <div style={{ padding: 12, background: 'var(--bg-secondary)', borderRadius: 8, minWidth: 0 }}>
      <div style={{ color: 'var(--text-muted)', fontSize: '0.68rem', textTransform: 'uppercase', marginBottom: 4 }}>{label}</div>
      <div style={{ fontWeight: 700, overflowWrap: 'anywhere' }}>{display(value)}</div>
    </div>
  );
}

function SummaryCard({ label, value, icon, detail }) {
  return (
    <article className="card" style={{ padding: 14, minWidth: 0 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'flex-start' }}>
        <div style={{ minWidth: 0 }}>
          <div style={{ color: 'var(--text-muted)', fontSize: '0.7rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.04em' }}>{label}</div>
          <div style={{ marginTop: 6, fontSize: '1.35rem', fontWeight: 800 }}>{value}</div>
          {detail && <div style={{ marginTop: 3, color: 'var(--text-muted)', fontSize: '0.72rem' }}>{detail}</div>}
        </div>
        <div className="stat-icon" style={{ width: 36, height: 36, flexShrink: 0 }}>{icon}</div>
      </div>
    </article>
  );
}

function StatusBadge({ status }) {
  const active = status === 'VERIFIED';
  return (
    <span className="badge" style={{
      background: active ? 'var(--success-light)' : 'var(--warning-light)',
      color: active ? 'var(--success)' : 'var(--warning)',
    }}>
      {status === 'VERIFIED' ? 'Active' : display(status)}
    </span>
  );
}

function Section({ title, subtitle, children, open = false, actions }) {
  return (
    <details className="card admin-faculty-section" open={open}>
      <summary>
        <span>
          <strong>{title}</strong>
          {subtitle && <small>{subtitle}</small>}
        </span>
        {actions}
      </summary>
      <div className="admin-faculty-section-content">{children}</div>
    </details>
  );
}

function FacultyProfile({ departmentId, facultyId }) {
  const navigate = useNavigate();
  const { getSorts, getSortedRows, toggleSort } = useTableSorting();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const facultyPath = `/departments/${departmentId}/faculty`;

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    getAdminDepartmentFacultyProfile(departmentId, facultyId)
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
  }, [departmentId, facultyId]);

  const personal = profile?.personalInformation;
  const professional = profile?.professionalInformation;
  const teaching = profile?.teachingOverview;
  const attendance = profile?.attendanceOverview;
  const performance = profile?.academicPerformanceOverview;
  const assignments = profile?.assignments || [];
  const subjectList = profile?.assignedSubjects || [];
  const openAssignment = (assignment) => navigate(
    `/departments/${departmentId}/assignments`,
    { state: { adminAssignmentId: assignment.id } },
  );

  return (
    <Layout pageTitle="Faculty Profile">
      <DepartmentSectionNav departmentId={departmentId} />
      <div style={{ marginBottom: 16 }}>
        <button className="btn btn-secondary btn-sm" type="button" onClick={() => navigate(facultyPath)}>
          <ArrowLeft size={15} /> Back to Faculty
        </button>
      </div>
      {loading && <div className="loader"><div className="spinner" /></div>}
      {!loading && error && <div className="alert alert-error" role="alert">{error}</div>}
      {!loading && profile && (
        <>
          <section className="card admin-faculty-hero">
            {personal.profileImageUrl ? (
              <img src={personal.profileImageUrl} alt="" className="avatar lg" style={{ objectFit: 'cover' }} />
            ) : (
              <div className="avatar lg" aria-hidden="true">{initials(personal.fullName)}</div>
            )}
            <div style={{ flex: 1, minWidth: 180 }}>
              <h2 style={{ fontSize: '1.3rem', fontWeight: 800, marginBottom: 3 }}>{display(personal.fullName)}</h2>
              <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                {display(personal.facultyId)} · {display(professional.departmentName)}
              </div>
            </div>
            <StatusBadge status={personal.accountStatus} />
          </section>

          <Section title="Personal Information" open>
            <div className="grid-3">
              <Metric label="Full name" value={personal.fullName} />
              <Metric label="Faculty ID" value={personal.facultyId} />
              <Metric label="Email" value={personal.email} />
              <Metric label="Phone" value={personal.phone} />
              <Metric label="Date of birth" value={personal.dateOfBirth} />
              <Metric label="Gender" value={personal.gender} />
              <Metric label="Residential address" value={personal.residentialAddress} />
              <Metric label="Joining date" value={personal.joiningDate} />
              <Metric label="Employment status" value={personal.employmentStatus} />
              <Metric label="Account status" value={personal.accountStatus} />
              <Metric label="Account created" value={formatDate(personal.accountCreatedAt)} />
            </div>
          </Section>

          <Section title="Professional Information" open>
            <div className="grid-3">
              <Metric label="Department" value={professional.departmentName} />
              <Metric label="Department code" value={professional.departmentCode} />
              <Metric label="Designation" value={professional.designation} />
              <Metric label="Qualification" value={professional.qualification} />
              <Metric label="Specialization" value={professional.specialization} />
              <Metric label="Experience" value={professional.yearsOfExperience == null ? null : `${professional.yearsOfExperience} years`} />
              <Metric label="Employment type" value={professional.employmentType} />
              <Metric label="Assigned academic years" value={professional.assignedAcademicYears.length ? professional.assignedAcademicYears.map((year) => `Year ${year}`).join(', ') : null} />
              <Metric label="Assigned semesters" value={professional.assignedSemesters.length ? professional.assignedSemesters.join(', ') : null} />
            </div>
          </Section>

          <Section title="Teaching Overview" open>
            <div className="stats-grid">
              <Metric label="Subjects assigned" value={teaching.totalSubjectsAssigned} />
              <Metric label="Classes conducted" value={teaching.totalClassesConducted} />
              <Metric label="Students teaching" value={teaching.totalStudentsTeaching} />
              <Metric label="Assigned academic years" value={teaching.assignedAcademicYears.length ? teaching.assignedAcademicYears.join(', ') : null} />
              <Metric label="Assigned semesters" value={teaching.assignedSemesters.length ? teaching.assignedSemesters.join(', ') : null} />
            </div>
          </Section>

          <Section title="Assigned Subjects" subtitle={`${subjectList.length} subjects`}>
            {!subjectList.length ? (
              <div className="empty-state"><p>No subjects are assigned to this faculty member.</p></div>
            ) : (
              <div className="table-container">
                <table>
                  <thead><tr>
                    <SortableTh tableId="faculty-subjects" columnKey="subjectName" label="Subject" sorts={getSorts('faculty-subjects')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-subjects" columnKey="subjectCode" label="Code" sorts={getSorts('faculty-subjects')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-subjects" columnKey="year" label="Year" sorts={getSorts('faculty-subjects')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-subjects" columnKey="semester" label="Semester" sorts={getSorts('faculty-subjects')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-subjects" columnKey="enrolledStudents" label="Enrolled students" sorts={getSorts('faculty-subjects')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-subjects" columnKey="classesConducted" label="Classes" sorts={getSorts('faculty-subjects')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-subjects" columnKey="studentAttendancePercentage" label="Attendance" sorts={getSorts('faculty-subjects')} onSort={toggleSort} />
                  </tr></thead>
                  <tbody>
                    {getSortedRows('faculty-subjects', subjectList).map((subject) => (
                      <tr key={subject.subjectId}>
                        <td>{subject.subjectName}</td>
                        <td>{subject.subjectCode}</td>
                        <td>{subject.year == null ? 'N/A' : `Year ${subject.year}`}</td>
                        <td>{display(subject.semester)}</td>
                        <td>{subject.enrolledStudents}</td>
                        <td>{display(subject.classesConducted)}</td>
                        <td>{formatPercent(subject.studentAttendancePercentage)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Section>

          <Section title="Attendance Management" subtitle="Attendance is calculated from recorded student attendance rows.">
            <div className="stats-grid" style={{ marginBottom: 14 }}>
              <Metric label="Total classes conducted" value={attendance.totalClassesConducted} />
              <Metric label="Student attendance" value={formatPercent(attendance.studentAttendancePercentage)} />
              <Metric label="Attendance records" value={attendance.totalAttendanceRecords} />
            </div>
            <h4 className="admin-faculty-subheading">Subject-wise Attendance</h4>
            {!attendance.subjectStatistics.length ? (
              <div className="empty-state"><p>No attendance records are available.</p></div>
            ) : (
              <div className="table-container">
                <table>
                  <thead><tr>
                    <SortableTh tableId="faculty-attendance-subjects" columnKey="subjectName" label="Subject" sorts={getSorts('faculty-attendance-subjects')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-attendance-subjects" columnKey="subjectCode" label="Code" sorts={getSorts('faculty-attendance-subjects')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-attendance-subjects" columnKey="classesConducted" label="Classes" sorts={getSorts('faculty-attendance-subjects')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-attendance-subjects" columnKey="enrolledStudents" label="Enrolled students" sorts={getSorts('faculty-attendance-subjects')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-attendance-subjects" columnKey="attendanceRecords" label="Records" sorts={getSorts('faculty-attendance-subjects')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-attendance-subjects" columnKey="attendancePercentage" label="Attendance" sorts={getSorts('faculty-attendance-subjects')} onSort={toggleSort} />
                  </tr></thead>
                  <tbody>
                    {getSortedRows('faculty-attendance-subjects', attendance.subjectStatistics).map((subject) => (
                      <tr key={subject.subjectId}>
                        <td>{subject.subjectName}</td><td>{subject.subjectCode}</td>
                        <td>{display(subject.classesConducted)}</td><td>{subject.enrolledStudents}</td>
                        <td>{subject.attendanceRecords}</td><td>{formatPercent(subject.attendancePercentage)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <h4 className="admin-faculty-subheading">Attendance History</h4>
            {!attendance.records.length ? (
              <div className="empty-state"><p>No attendance history is available.</p></div>
            ) : (
              <div className="table-container">
                <table>
                  <thead><tr>
                    <SortableTh tableId="faculty-attendance-history" columnKey="date" label="Date" sorts={getSorts('faculty-attendance-history')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-attendance-history" columnKey="studentName" label="Student" sorts={getSorts('faculty-attendance-history')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-attendance-history" columnKey="studentRollNumber" label="Student ID" sorts={getSorts('faculty-attendance-history')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-attendance-history" columnKey="subjectName" label="Subject" sorts={getSorts('faculty-attendance-history')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-attendance-history" columnKey="status" label="Status" sorts={getSorts('faculty-attendance-history')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-attendance-history" columnKey="performanceMarks" label="Performance marks" sorts={getSorts('faculty-attendance-history')} onSort={toggleSort} />
                  </tr></thead>
                  <tbody>
                    {getSortedRows('faculty-attendance-history', attendance.records).map((record) => (
                      <tr key={record.id}>
                        <td>{formatDate(record.date)}</td>
                        <td>{record.studentName}</td>
                        <td>{record.studentRollNumber}</td>
                        <td>{record.subjectName} ({record.subjectCode})</td>
                        <td>{record.status}</td>
                        <td>{display(record.performanceMarks)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Section>

          <Section
            title="Assignment Management"
            subtitle={`${assignments.length} assignments`}
            actions={(
              <button
                className="btn btn-secondary btn-sm"
                type="button"
                onClick={(event) => {
                  event.preventDefault();
                  event.stopPropagation();
                  navigate(`/departments/${departmentId}/assignments`);
                }}
              >
                Open Assignments
              </button>
            )}
          >
            {!assignments.length ? (
              <div className="empty-state"><p>No assignments created by this faculty member are recorded.</p></div>
            ) : (
              <div className="table-container">
                <table>
                  <thead><tr>
                    <SortableTh tableId="faculty-assignments" columnKey="title" label="Assignment" sorts={getSorts('faculty-assignments')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-assignments" columnKey="subjectName" label="Subject" sorts={getSorts('faculty-assignments')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-assignments" columnKey="creationDate" label="Created" sorts={getSorts('faculty-assignments')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-assignments" columnKey="dueDate" label="Due date" sorts={getSorts('faculty-assignments')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-assignments" columnKey="totalSubmissions" label="Submissions" sorts={getSorts('faculty-assignments')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-assignments" columnKey="pendingSubmissions" label="Pending" sorts={getSorts('faculty-assignments')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-assignments" columnKey="gradingStatus" label="Grading status" sorts={getSorts('faculty-assignments')} onSort={toggleSort} />
                    <th></th>
                  </tr></thead>
                  <tbody>
                    {getSortedRows('faculty-assignments', assignments).map((assignment) => (
                      <tr key={assignment.id}>
                        <td>{assignment.title}</td>
                        <td>{assignment.subjectName} ({assignment.subjectCode})</td>
                        <td>{formatDate(assignment.creationDate)}</td>
                        <td>{formatDate(assignment.dueDate)}</td>
                        <td>{assignment.totalSubmissions}</td>
                        <td>{assignment.pendingSubmissions}</td>
                        <td>{assignment.gradingStatus}</td>
                        <td>
                          <button className="btn btn-secondary btn-sm" type="button" onClick={() => openAssignment(assignment)}>
                            View details
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Section>

          <Section title="Academic Performance Overview">
            <div className="stats-grid" style={{ marginBottom: 14 }}>
              <Metric label="Average subject performance" value={formatPerformance(performance.averageSubjectPerformance)} />
              <Metric label="Grade distribution" value={performance.gradeDistribution} />
            </div>
            {!performance.subjectPerformance.length ? (
              <div className="empty-state"><p>No performance marks are available.</p></div>
            ) : (
              <div className="table-container">
                <table>
                  <thead><tr>
                    <SortableTh tableId="faculty-performance" columnKey="subjectName" label="Subject" sorts={getSorts('faculty-performance')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-performance" columnKey="subjectCode" label="Code" sorts={getSorts('faculty-performance')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-performance" columnKey="averageMarks" label="Average marks (/10)" sorts={getSorts('faculty-performance')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-performance" columnKey="assessedRecords" label="Assessed records" sorts={getSorts('faculty-performance')} onSort={toggleSort} />
                  </tr></thead>
                  <tbody>
                    {getSortedRows('faculty-performance', performance.subjectPerformance).map((subject) => (
                      <tr key={subject.subjectId}>
                        <td>{subject.subjectName}</td><td>{subject.subjectCode}</td>
                        <td>{formatPerformance(subject.averageMarks)}</td><td>{subject.assessedRecords}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <h4 className="admin-faculty-subheading">Students Requiring Academic Attention</h4>
            {!performance.studentsRequiringAttention.length ? (
              <div className="empty-state"><p>No students meet the configured attention thresholds, or performance records are unavailable.</p></div>
            ) : (
              <div className="table-container">
                <table>
                  <thead><tr>
                    <SortableTh tableId="faculty-attention" columnKey="studentName" label="Student" sorts={getSorts('faculty-attention')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-attention" columnKey="studentRollNumber" label="ID" sorts={getSorts('faculty-attention')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-attention" columnKey="attendancePercentage" label="Attendance" sorts={getSorts('faculty-attention')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-attention" columnKey="averagePerformance" label="Performance" sorts={getSorts('faculty-attention')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-attention" columnKey="subjects" label="Subjects" sorts={getSorts('faculty-attention')} onSort={toggleSort} />
                    <SortableTh tableId="faculty-attention" columnKey="reasons" label="Indicators" sorts={getSorts('faculty-attention')} onSort={toggleSort} />
                  </tr></thead>
                  <tbody>
                    {getSortedRows('faculty-attention', performance.studentsRequiringAttention, {
                      subjects: (student) => student.subjects.join(', '),
                      reasons: (student) => student.reasons.join(', '),
                    }).map((student) => (
                      <tr key={student.studentId}>
                        <td>{student.studentName}</td><td>{student.studentRollNumber}</td>
                        <td>{formatPercent(student.attendancePercentage)}</td>
                        <td>{formatPerformance(student.averagePerformance)}</td>
                        <td>{student.subjects.join(', ') || 'N/A'}</td>
                        <td>{student.reasons.join(', ')}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Section>

          <Section title="Additional Information">
            {profile.additionalInformation.documents.length ? (
              <ul>{profile.additionalInformation.documents.map((document) => <li key={document}>{document}</li>)}</ul>
            ) : (
              <div className="empty-state"><p>No professional documents or certifications are stored for this faculty member.</p></div>
            )}
          </Section>
        </>
      )}
    </Layout>
  );
}

function FacultyDirectory({ departmentId }) {
  const navigate = useNavigate();
  const { getSorts, toggleSort } = useTableSorting();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [academicYear, setAcademicYear] = useState('');
  const [subjectId, setSubjectId] = useState('');
  const [status, setStatus] = useState('');
  const sorts = getSorts('faculty-roster');
  const [page, setPage] = useState(0);
  const size = 25;

  useEffect(() => {
    let active = true;
    const timer = setTimeout(() => {
      setLoading(true);
      setError('');
      getAdminDepartmentFaculty(departmentId, {
        academicYear: academicYear || undefined,
        subjectId: subjectId || undefined,
        status: status || undefined,
        search: search.trim() || undefined,
        ...(sorts.length ? { sort: sorts.map(({ key, direction }) => `${key}:${direction}`).join(',') } : {}),
        page,
        size,
      })
        .then((response) => {
          if (active) setData(response.data);
        })
        .catch((requestError) => {
          if (active) setError(errorMessage(requestError));
        })
        .finally(() => {
          if (active) setLoading(false);
        });
    }, 250);
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [departmentId, academicYear, subjectId, status, search, sorts, page]);

  const filterOptions = data?.filters;
  const summary = data?.summary;
  const pageNumbers = useMemo(() => {
    const totalPages = data?.totalPages || 0;
    const start = Math.min(Math.max(0, page - 2), Math.max(0, totalPages - 5));
    const end = Math.min(totalPages, start + 5);
    return Array.from({ length: end - start }, (_, index) => start + index);
  }, [data?.totalPages, page]);

  const openFaculty = (faculty) => navigate(`/departments/${departmentId}/faculty/${faculty.userId}`);
  const handleSort = (tableId, key) => {
    toggleSort(tableId, key);
    setPage(0);
  };
  return (
    <Layout pageTitle="Faculty Management">
      <DepartmentSectionNav departmentId={departmentId} />
      <div className="page-header">
        <div>
          <h2 className="page-title">Faculty Management</h2>
          <p className="page-subtitle">Faculty associated with this department and their teaching activity.</p>
        </div>
      </div>

      <div className="stats-grid" style={{ marginBottom: 18 }}>
        <SummaryCard label="Total faculty members" value={summary?.totalFacultyMembers ?? '—'} icon={<Users size={18} />} />
        <SummaryCard label="Active faculty" value={summary?.activeFaculty ?? '—'} icon={<Users size={18} />} detail="Verified accounts" />
        <SummaryCard label="Subjects assigned" value={summary?.totalSubjectsAssigned ?? '—'} icon={<BookOpen size={18} />} />
        <SummaryCard label="Average student attendance" value={formatPercent(summary?.averageStudentAttendance)} icon={<CalendarCheck size={18} />} detail="Weighted from recorded attendance" />
      </div>

      <section className="card admin-faculty-directory">
        <div className="toolbar">
          <div className="toolbar-left">
            <div className="search-bar">
              <Search />
              <input
                aria-label="Search faculty"
                placeholder="Search name, faculty ID, or email..."
                value={search}
                onChange={(event) => { setSearch(event.target.value); setPage(0); }}
              />
            </div>
            <div className="admin-faculty-filters" role="group" aria-label="Faculty filters">
              <select className="form-select" aria-label="Filter by academic year" value={academicYear} onChange={(event) => { setAcademicYear(event.target.value); setPage(0); }}>
                <option value="">All academic years</option>
                {(filterOptions?.academicYears || []).map((year) => <option key={year} value={year}>Year {year}</option>)}
              </select>
              <select className="form-select" aria-label="Filter by subject" value={subjectId} onChange={(event) => { setSubjectId(event.target.value); setPage(0); }}>
                <option value="">All subjects</option>
                {(filterOptions?.subjects || []).map((subject) => <option key={subject.id} value={subject.id}>{subject.name} ({subject.code})</option>)}
              </select>
              <select className="form-select" aria-label="Filter by faculty status" value={status} onChange={(event) => { setStatus(event.target.value); setPage(0); }}>
                <option value="">All statuses</option>
                {(filterOptions?.statuses || []).map((option) => <option key={option} value={option}>{option === 'VERIFIED' ? 'Active' : option}</option>)}
              </select>
            </div>
          </div>
        </div>

        {loading ? (
          <div className="loader"><div className="spinner" /></div>
        ) : error ? (
          <div className="alert alert-error" role="alert">{error}</div>
        ) : !data?.items?.length ? (
          <div className="empty-state">
            <Users />
            <h3>No faculty found</h3>
            <p>{search || subjectId || academicYear || status ? 'Try adjusting your search or filters.' : 'No faculty members are associated with this department yet.'}</p>
          </div>
        ) : (
          <>
            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <SortableTh tableId="faculty-roster" columnKey="facultyId" label="Faculty ID" sorts={sorts} onSort={handleSort} style={HEADER_STYLE} />
                    <SortableTh tableId="faculty-roster" columnKey="name" label="Faculty" sorts={sorts} onSort={handleSort} style={HEADER_STYLE} />
                    <SortableTh tableId="faculty-roster" columnKey="designation" label="Designation" sorts={sorts} onSort={handleSort} style={HEADER_STYLE} />
                    <SortableTh tableId="faculty-roster" columnKey="email" label="Email" sorts={sorts} onSort={handleSort} style={HEADER_STYLE} />
                    <SortableTh tableId="faculty-roster" columnKey="phone" label="Phone" sorts={sorts} onSort={handleSort} style={HEADER_STYLE} />
                    <SortableTh tableId="faculty-roster" columnKey="subjects" label="Subjects" sorts={sorts} onSort={handleSort} style={HEADER_STYLE} />
                    <SortableTh tableId="faculty-roster" columnKey="students" label="Students" sorts={sorts} onSort={handleSort} style={HEADER_STYLE} />
                    <SortableTh tableId="faculty-roster" columnKey="status" label="Status" sorts={sorts} onSort={handleSort} style={HEADER_STYLE} />
                    <SortableTh tableId="faculty-roster" columnKey="attendance" label="Attendance" sorts={sorts} onSort={handleSort} style={HEADER_STYLE} />
                    <th style={HEADER_STYLE}></th>
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((faculty) => (
                    <tr
                      key={faculty.userId}
                      className="admin-faculty-row"
                      tabIndex={0}
                      onClick={() => openFaculty(faculty)}
                      onKeyDown={(event) => {
                        if (event.key === 'Enter' || event.key === ' ') {
                          event.preventDefault();
                          openFaculty(faculty);
                        }
                      }}
                    >
                      <td style={CELL_STYLE}>{faculty.facultyId}</td>
                      <td style={CELL_STYLE}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 9, minWidth: 150 }}>
                          {faculty.profileImageUrl ? (
                            <img src={faculty.profileImageUrl} alt="" className="avatar sm" style={{ objectFit: 'cover' }} />
                          ) : (
                            <div className="avatar sm" aria-hidden="true">{initials(faculty.name)}</div>
                          )}
                          <span style={{ fontWeight: 700 }}>{faculty.name}</span>
                        </div>
                      </td>
                      <td style={CELL_STYLE}>{display(faculty.designation)}</td>
                      <td style={CELL_STYLE}>{display(faculty.email)}</td>
                      <td style={CELL_STYLE}>{display(faculty.phone)}</td>
                      <td style={CELL_STYLE}>{faculty.assignedSubjects}</td>
                      <td style={CELL_STYLE}>{faculty.studentsTeaching}</td>
                      <td style={CELL_STYLE}><StatusBadge status={faculty.status} /></td>
                      <td style={CELL_STYLE}>{formatPercent(faculty.averageStudentAttendance)}</td>
                      <td style={CELL_STYLE}>
                        <button className="btn btn-secondary btn-sm" type="button" onClick={(event) => { event.stopPropagation(); openFaculty(faculty); }}>
                          View Details
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="admin-faculty-pagination">
              <span>{data.totalElements} faculty members</span>
              <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                <button className="btn btn-secondary btn-sm" type="button" disabled={page === 0} onClick={() => setPage((value) => Math.max(0, value - 1))}>Previous</button>
                {pageNumbers.map((pageNumber) => (
                  <button
                    key={pageNumber}
                    className={`btn btn-sm ${pageNumber === page ? 'btn-primary' : 'btn-secondary'}`}
                    type="button"
                    aria-current={pageNumber === page ? 'page' : undefined}
                    onClick={() => setPage(pageNumber)}
                  >
                    {pageNumber + 1}
                  </button>
                ))}
                <button className="btn btn-secondary btn-sm" type="button" disabled={page + 1 >= data.totalPages} onClick={() => setPage((value) => value + 1)}>Next</button>
              </div>
            </div>
          </>
        )}
      </section>
    </Layout>
  );
}

export default function AdminDepartmentFacultyPage({ departmentId, facultyId }) {
  return facultyId
    ? <FacultyProfile departmentId={departmentId} facultyId={facultyId} />
    : <FacultyDirectory departmentId={departmentId} />;
}
