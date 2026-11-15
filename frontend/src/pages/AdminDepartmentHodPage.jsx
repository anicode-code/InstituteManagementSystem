import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ArrowLeft, ArrowRight, BookOpen, Building2, CalendarCheck, ClipboardList, GraduationCap, Mail, Users,
} from 'lucide-react';
import DepartmentSectionNav from '../components/DepartmentSectionNav';
import Layout from '../components/Layout';
import SortableTh from '../components/SortableTh';
import { useTableSorting } from '../utils/tableSorting';
import { getAdminDepartmentHodDirectory, getAdminDepartmentHodProfile } from '../api/adminDepartmentHod';

function display(value) {
  return value == null || value === '' ? 'N/A' : value;
}

function percent(value) {
  return value == null ? 'N/A' : `${Number(value).toFixed(1)}%`;
}

function marks(value) {
  return value == null ? 'N/A' : `${Number(value).toFixed(1)} / 10`;
}

function date(value) {
  return value ? new Date(value).toLocaleDateString() : 'N/A';
}

function initials(name) {
  return name?.split(/\s+/).filter(Boolean).map((part) => part[0]).join('').toUpperCase().slice(0, 2) || '?';
}

function errorMessage(error) {
  return error?.response?.data?.message || 'Could not load HOD information. Please try again.';
}

function Metric({ label, value }) {
  return (
    <div className="admin-hod-metric">
      <div className="admin-hod-metric-label">{label}</div>
      <div className="admin-hod-metric-value">{display(value)}</div>
    </div>
  );
}

function SummaryCard({ label, value, icon, detail }) {
  return (
    <article className="card admin-hod-summary-card">
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
    <details className="card admin-hod-section" open={open}>
      <summary>
        <span>
          <strong>{title}</strong>
          {subtitle && <small>{subtitle}</small>}
        </span>
        {actions}
      </summary>
      <div className="admin-hod-section-content">{children}</div>
    </details>
  );
}

function DistributionTable({ title, entries, year = false }) {
  const { getSorts, getSortedRows, toggleSort } = useTableSorting();
  const tableId = `hod-distribution-${title}`;
  return (
    <div className="admin-hod-distribution">
      <h4 className="admin-hod-subheading">{title}</h4>
      {!entries.length ? (
        <div className="empty-state"><p>No distribution data is available.</p></div>
      ) : (
        <div className="table-container">
          <table>
            <thead><tr>
              <SortableTh tableId={tableId} columnKey="value" label={year ? 'Academic year' : 'Semester'} sorts={getSorts(tableId)} onSort={toggleSort} />
              <SortableTh tableId={tableId} columnKey="count" label="Students" sorts={getSorts(tableId)} onSort={toggleSort} />
            </tr></thead>
            <tbody>
              {getSortedRows(tableId, entries).map((entry) => (
                <tr key={entry.value}>
                  <td>{year ? `Year ${entry.value}` : `Semester ${entry.value}`}</td>
                  <td>{entry.count}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function StudentAttentionTable({ students, departmentId, navigate }) {
  const { getSorts, getSortedRows, toggleSort } = useTableSorting();
  const tableId = 'hod-student-attention';
  return !students.length ? (
    <div className="empty-state"><p>No students meet the configured attention thresholds, or attendance/performance records are unavailable.</p></div>
  ) : (
    <div className="table-container">
      <table>
        <thead><tr>
          <SortableTh tableId={tableId} columnKey="name" label="Student" sorts={getSorts(tableId)} onSort={toggleSort} />
          <SortableTh tableId={tableId} columnKey="rollNumber" label="Student ID" sorts={getSorts(tableId)} onSort={toggleSort} />
          <SortableTh tableId={tableId} columnKey="attendancePercentage" label="Attendance" sorts={getSorts(tableId)} onSort={toggleSort} />
          <SortableTh tableId={tableId} columnKey="averagePerformance" label="Performance" sorts={getSorts(tableId)} onSort={toggleSort} />
          <SortableTh tableId={tableId} columnKey="reasons" label="Indicators" sorts={getSorts(tableId)} onSort={toggleSort} />
          <th></th>
        </tr></thead>
        <tbody>
          {getSortedRows(tableId, students, { reasons: (student) => student.reasons.join(', ') }).map((student) => (
            <tr key={student.studentId}>
              <td>{student.name}</td>
              <td>{student.rollNumber}</td>
              <td>{percent(student.attendancePercentage)}</td>
              <td>{marks(student.averagePerformance)}</td>
              <td>{student.reasons.join(', ')}</td>
              <td>
                <button className="btn btn-secondary btn-sm" type="button" onClick={() => navigate(`/departments/${departmentId}/students/${student.studentId}`)}>
                  View Student
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function HodProfile({ departmentId, hodId }) {
  const navigate = useNavigate();
  const { getSorts, getSortedRows, toggleSort } = useTableSorting();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    getAdminDepartmentHodProfile(departmentId, hodId)
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
  }, [departmentId, hodId]);

  const personal = profile?.personalInformation;
  const professional = profile?.professionalInformation;
  const department = profile?.departmentOverview;
  const students = profile?.studentOverview;
  const attendance = profile?.attendanceOverview;
  const assignments = profile?.assignmentOverview;
  const performance = profile?.academicPerformanceOverview;
  const hodPath = `/departments/${departmentId}/hod`;

  return (
    <Layout pageTitle="HOD Profile">
      <DepartmentSectionNav departmentId={departmentId} />
      <div className="admin-hod-profile-toolbar">
        <button className="btn btn-secondary btn-sm" type="button" onClick={() => navigate(hodPath)}>
          <ArrowLeft size={15} /> Back to HOD
        </button>
        <span>{professional?.departmentName || 'Department leadership'}</span>
      </div>
      {loading && <div className="loader"><div className="spinner" /></div>}
      {!loading && error && <div className="alert alert-error" role="alert">{error}</div>}
      {!loading && !error && profile && (
        <>
          <section className="card admin-hod-hero">
            {personal.profileImageUrl ? (
              <img src={personal.profileImageUrl} alt="" className="avatar lg" style={{ objectFit: 'cover' }} />
            ) : (
              <div className="avatar lg" aria-hidden="true">{initials(personal.fullName)}</div>
            )}
            <div style={{ flex: 1, minWidth: 180 }}>
              <h2 style={{ fontSize: '1.3rem', fontWeight: 800, marginBottom: 3 }}>{display(personal.fullName)}</h2>
              <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                {display(personal.hodId)} · {display(professional.departmentName)}
              </div>
              {personal.email && <a className="admin-hod-email" href={`mailto:${personal.email}`}><Mail size={15} />{personal.email}</a>}
            </div>
            <StatusBadge status={personal.accountStatus} />
          </section>

          <div className="admin-hod-overview-heading">
            <div>
              <h3>Department overview</h3>
              <p>Current academic and operational snapshot</p>
            </div>
          </div>
          <div className="stats-grid admin-hod-profile-stats">
            <SummaryCard label="Students" value={department.totalStudents} icon={<GraduationCap size={18} />} />
            <SummaryCard label="Faculty members" value={department.totalFacultyMembers} icon={<Users size={18} />} />
            <SummaryCard label="Subjects" value={department.totalSubjects} icon={<BookOpen size={18} />} />
            <SummaryCard label="Active assignments" value={department.totalActiveAssignments} icon={<ClipboardList size={18} />} />
            <SummaryCard label="Attendance average" value={percent(department.departmentAttendanceAverage)} icon={<CalendarCheck size={18} />} />
            <SummaryCard label="Academic performance" value={marks(department.overallAcademicPerformance)} icon={<Building2 size={18} />} />
          </div>

          <nav className="admin-hod-links" aria-label="Related department sections">
            <button className="btn btn-secondary btn-sm" type="button" onClick={() => navigate(`/departments/${departmentId}/faculty`)}><Users size={14} /> Faculty</button>
            <button className="btn btn-secondary btn-sm" type="button" onClick={() => navigate(`/departments/${departmentId}/students`)}><GraduationCap size={14} /> Students</button>
            <button className="btn btn-secondary btn-sm" type="button" onClick={() => navigate(`/departments/${departmentId}/subjects`)}><BookOpen size={14} /> Subjects</button>
            <button className="btn btn-secondary btn-sm" type="button" onClick={() => navigate(`/departments/${departmentId}/assignments`)}><ClipboardList size={14} /> Assignments</button>
            <button className="btn btn-secondary btn-sm" type="button" onClick={() => navigate(`/departments/${departmentId}/attendance`)}><CalendarCheck size={14} /> Attendance</button>
          </nav>

          <Section title="Personal Information" open>
            <div className="grid-3">
              <Metric label="Full name" value={personal.fullName} />
              <Metric label="HOD ID / Employee ID" value={personal.hodId} />
              <Metric label="Email" value={personal.email} />
              <Metric label="Phone" value={personal.phone} />
              <Metric label="Date of birth" value={personal.dateOfBirth} />
              <Metric label="Gender" value={personal.gender} />
              <Metric label="Residential address" value={personal.residentialAddress} />
              <Metric label="Joining date" value={personal.joiningDate} />
              <Metric label="HOD appointment date" value={personal.appointmentDate} />
              <Metric label="Employment status" value={personal.employmentStatus} />
              <Metric label="Account status" value={personal.accountStatus} />
              <Metric label="Account created" value={date(personal.accountCreatedAt)} />
            </div>
          </Section>

          <Section title="Professional Information">
            <div className="grid-3">
              <Metric label="Department" value={professional.departmentName} />
              <Metric label="Department code" value={professional.departmentCode} />
              <Metric label="Designation" value={professional.designation} />
              <Metric label="Highest qualification" value={professional.highestQualification} />
              <Metric label="Specialization" value={professional.specialization} />
              <Metric label="Professional experience" value={professional.professionalExperience == null ? null : `${professional.professionalExperience} years`} />
              <Metric label="Employment type" value={professional.employmentType} />
              <Metric label="Previous designations" value={professional.previousDesignations.length ? professional.previousDesignations.join(', ') : null} />
            </div>
          </Section>

          <Section title="Faculty Management Overview" subtitle={`${profile.facultyMembers.length} faculty members`}>
            {!profile.facultyMembers.length ? (
              <div className="empty-state"><p>No faculty members are associated with this department.</p></div>
            ) : (
              <div className="table-container">
                <table>
                  <thead><tr>
                    <SortableTh tableId="hod-faculty" columnKey="name" label="Faculty" sorts={getSorts('hod-faculty')} onSort={toggleSort} />
                    <SortableTh tableId="hod-faculty" columnKey="facultyId" label="Faculty ID" sorts={getSorts('hod-faculty')} onSort={toggleSort} />
                    <SortableTh tableId="hod-faculty" columnKey="designation" label="Designation" sorts={getSorts('hod-faculty')} onSort={toggleSort} />
                    <SortableTh tableId="hod-faculty" columnKey="assignedSubjects" label="Assigned subjects" sorts={getSorts('hod-faculty')} onSort={toggleSort} />
                    <SortableTh tableId="hod-faculty" columnKey="assignedYears" label="Years / semesters" sorts={getSorts('hod-faculty')} onSort={toggleSort} />
                    <SortableTh tableId="hod-faculty" columnKey="employmentStatus" label="Status" sorts={getSorts('hod-faculty')} onSort={toggleSort} />
                    <th></th>
                  </tr></thead>
                  <tbody>
                    {getSortedRows('hod-faculty', profile.facultyMembers, {
                      assignedSubjects: (faculty) => faculty.assignedSubjects.join(', '),
                      assignedYears: (faculty) => [...faculty.assignedYears, ...faculty.assignedSemesters].join(', '),
                    }).map((faculty) => (
                      <tr key={faculty.userId}>
                        <td>{faculty.name}</td>
                        <td>{faculty.facultyId}</td>
                        <td>{display(faculty.designation)}</td>
                        <td>{faculty.assignedSubjects.join(', ') || 'N/A'}</td>
                        <td>
                          {faculty.assignedYears.length || faculty.assignedSemesters.length
                            ? `${faculty.assignedYears.map((year) => `Year ${year}`).join(', ') || ''}${faculty.assignedYears.length && faculty.assignedSemesters.length ? ' · ' : ''}${faculty.assignedSemesters.map((semester) => `Semester ${semester}`).join(', ') || ''}`
                            : 'N/A'}
                        </td>
                        <td><StatusBadge status={faculty.employmentStatus} /></td>
                        <td><button className="btn btn-secondary btn-sm" type="button" onClick={() => navigate(`/departments/${departmentId}/faculty/${faculty.userId}`)}>View Profile</button></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Section>

          <Section
            title="Student Management Overview"
            subtitle={`${students.totalStudents} students`}
            actions={<button className="btn btn-secondary btn-sm" type="button" onClick={(event) => { event.preventDefault(); event.stopPropagation(); navigate(`/departments/${departmentId}/students`); }}>Open Students</button>}
          >
            <div className="stats-grid" style={{ marginBottom: 12 }}>
              <Metric label="Total students" value={students.totalStudents} />
              <Metric label="Overall attendance" value={percent(students.overallAttendance)} />
              <Metric label="Academic performance" value={marks(students.academicPerformance)} />
            </div>
            <div className="admin-hod-distribution-grid">
              <DistributionTable title="Year-wise Student Distribution" entries={students.yearDistribution} year />
              <DistributionTable title="Semester-wise Student Distribution" entries={students.semesterDistribution} />
            </div>
            <h4 className="admin-hod-subheading">Students Requiring Attention</h4>
            <StudentAttentionTable students={students.studentsRequiringAttention} departmentId={departmentId} navigate={navigate} />
          </Section>

          <Section title="Subject and Academic Management" subtitle={`${profile.subjects.length} subjects`}>
            {!profile.subjects.length ? (
              <div className="empty-state"><p>No subjects are recorded for this department.</p></div>
            ) : (
              <div className="table-container">
                <table>
                  <thead>
                    <tr>
                      <SortableTh tableId="hod-subjects" columnKey="name" label="Subject" sorts={getSorts('hod-subjects')} onSort={toggleSort} />
                      <SortableTh tableId="hod-subjects" columnKey="code" label="Code" sorts={getSorts('hod-subjects')} onSort={toggleSort} style={{ textAlign: 'center' }} />
                      <SortableTh tableId="hod-subjects" columnKey="assignedFaculty" label="Assigned faculty" sorts={getSorts('hod-subjects')} onSort={toggleSort} />
                      <SortableTh tableId="hod-subjects" columnKey="semester" label="Semester" sorts={getSorts('hod-subjects')} onSort={toggleSort} style={{ textAlign: 'center' }} />
                      <SortableTh tableId="hod-subjects" columnKey="enrolledStudents" label="Enrolled students" sorts={getSorts('hod-subjects')} onSort={toggleSort} style={{ textAlign: 'center' }} />
                      <SortableTh tableId="hod-subjects" columnKey="attendancePercentage" label="Attendance" sorts={getSorts('hod-subjects')} onSort={toggleSort} style={{ textAlign: 'center' }} />
                      <SortableTh tableId="hod-subjects" columnKey="classesConducted" label="Classes taken" sorts={getSorts('hod-subjects')} onSort={toggleSort} style={{ textAlign: 'center' }} />
                    </tr>
                  </thead>
                  <tbody>
                    {getSortedRows('hod-subjects', profile.subjects, {
                      assignedFaculty: (subject) => subject.assignedFaculty.join(', '),
                    }).map((subject) => (
                      <tr key={subject.subjectId}>
                        <td>{subject.name}</td>
                        <td style={{ textAlign: 'center' }}>{subject.code}</td>
                        <td>{subject.assignedFaculty.join(', ') || 'N/A'}</td>
                        <td style={{ textAlign: 'center' }}>{display(subject.semester)}</td>
                        <td style={{ textAlign: 'center' }}>{subject.enrolledStudents}</td>
                        <td style={{ textAlign: 'center' }}>{percent(subject.attendancePercentage)}</td>
                        <td style={{ textAlign: 'center' }}>{subject.classesConducted}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Section>

          <Section title="Attendance Overview">
            <div className="stats-grid" style={{ marginBottom: 12 }}>
              <Metric label="Department attendance" value={percent(attendance.departmentAttendancePercentage)} />
            </div>
            <h4 className="admin-hod-subheading">Faculty-wise Attendance</h4>
            {!attendance.facultyStatistics.length ? (
              <div className="empty-state"><p>No faculty attendance statistics are available.</p></div>
            ) : (
              <div className="table-container">
                <table>
                  <thead><tr>
                    <SortableTh tableId="hod-faculty-attendance" columnKey="facultyName" label="Faculty" sorts={getSorts('hod-faculty-attendance')} onSort={toggleSort} />
                    <SortableTh tableId="hod-faculty-attendance" columnKey="employeeId" label="Faculty ID" sorts={getSorts('hod-faculty-attendance')} onSort={toggleSort} />
                    <SortableTh tableId="hod-faculty-attendance" columnKey="attendanceRecords" label="Records" sorts={getSorts('hod-faculty-attendance')} onSort={toggleSort} />
                    <SortableTh tableId="hod-faculty-attendance" columnKey="attendancePercentage" label="Attendance" sorts={getSorts('hod-faculty-attendance')} onSort={toggleSort} />
                  </tr></thead>
                  <tbody>{getSortedRows('hod-faculty-attendance', attendance.facultyStatistics).map((faculty) => <tr key={faculty.facultyId}><td>{faculty.facultyName}</td><td>{faculty.employeeId}</td><td>{faculty.attendanceRecords}</td><td>{percent(faculty.attendancePercentage)}</td></tr>)}</tbody>
                </table>
              </div>
            )}
            <h4 className="admin-hod-subheading">Students with Low Attendance</h4>
            <StudentAttentionTable students={attendance.studentsWithLowAttendance} departmentId={departmentId} navigate={navigate} />
            <h4 className="admin-hod-subheading">Attendance Trends</h4>
            {!attendance.trends.length ? (
              <div className="empty-state"><p>No dated attendance history is available.</p></div>
            ) : (
              <div className="table-container">
                <table>
                  <thead><tr>
                    <SortableTh tableId="hod-attendance-trends" columnKey="month" label="Month" sorts={getSorts('hod-attendance-trends')} onSort={toggleSort} />
                    <SortableTh tableId="hod-attendance-trends" columnKey="attendanceRecords" label="Attendance records" sorts={getSorts('hod-attendance-trends')} onSort={toggleSort} />
                    <SortableTh tableId="hod-attendance-trends" columnKey="attendancePercentage" label="Attendance" sorts={getSorts('hod-attendance-trends')} onSort={toggleSort} />
                  </tr></thead>
                  <tbody>{getSortedRows('hod-attendance-trends', attendance.trends).map((trend) => <tr key={trend.month}><td>{trend.month}</td><td>{trend.attendanceRecords}</td><td>{percent(trend.attendancePercentage)}</td></tr>)}</tbody>
                </table>
              </div>
            )}
          </Section>

          <Section title="Assignment Overview">
            <div className="stats-grid" style={{ marginBottom: 12 }}>
              <Metric label="Total assignments" value={assignments.totalAssignments} />
              <Metric label="Active assignments" value={assignments.totalActiveAssignments} />
              <Metric label="Overdue assignments" value={assignments.overdueAssignments} />
              <Metric label="Pending submissions" value={assignments.pendingSubmissions} />
              <Metric label="Assignments awaiting grading" value={assignments.assignmentsAwaitingGrading} />
            </div>
            <h4 className="admin-hod-subheading">Faculty Assignment Distribution</h4>
            {!assignments.facultyDistribution.length ? (
              <div className="empty-state"><p>No faculty assignment distribution is available.</p></div>
            ) : (
              <div className="table-container">
                <table>
                  <thead><tr>
                    <SortableTh tableId="hod-faculty-assignments" columnKey="facultyName" label="Faculty" sorts={getSorts('hod-faculty-assignments')} onSort={toggleSort} />
                    <SortableTh tableId="hod-faculty-assignments" columnKey="employeeId" label="Faculty ID" sorts={getSorts('hod-faculty-assignments')} onSort={toggleSort} />
                    <SortableTh tableId="hod-faculty-assignments" columnKey="assignmentCount" label="Assignments" sorts={getSorts('hod-faculty-assignments')} onSort={toggleSort} />
                  </tr></thead>
                  <tbody>{getSortedRows('hod-faculty-assignments', assignments.facultyDistribution).map((faculty) => <tr key={faculty.facultyId}><td>{faculty.facultyName}</td><td>{faculty.employeeId}</td><td>{faculty.assignmentCount}</td></tr>)}</tbody>
                </table>
              </div>
            )}
            <h4 className="admin-hod-subheading">Department Assignments</h4>
            {!assignments.assignments.length ? (
              <div className="empty-state"><p>No assignments are recorded for this department.</p></div>
            ) : (
              <div className="table-container">
                <table>
                  <thead><tr>
                    <SortableTh tableId="hod-assignments" columnKey="title" label="Assignment" sorts={getSorts('hod-assignments')} onSort={toggleSort} />
                    <SortableTh tableId="hod-assignments" columnKey="subjectName" label="Subject" sorts={getSorts('hod-assignments')} onSort={toggleSort} />
                    <SortableTh tableId="hod-assignments" columnKey="facultyName" label="Faculty" sorts={getSorts('hod-assignments')} onSort={toggleSort} />
                    <SortableTh tableId="hod-assignments" columnKey="creationDate" label="Created" sorts={getSorts('hod-assignments')} onSort={toggleSort} />
                    <SortableTh tableId="hod-assignments" columnKey="dueDate" label="Due date" sorts={getSorts('hod-assignments')} onSort={toggleSort} />
                    <SortableTh tableId="hod-assignments" columnKey="totalSubmissions" label="Submissions" sorts={getSorts('hod-assignments')} onSort={toggleSort} />
                    <SortableTh tableId="hod-assignments" columnKey="pendingSubmissions" label="Pending" sorts={getSorts('hod-assignments')} onSort={toggleSort} />
                    <SortableTh tableId="hod-assignments" columnKey="gradingStatus" label="Grading status" sorts={getSorts('hod-assignments')} onSort={toggleSort} />
                  </tr></thead>
                  <tbody>
                    {getSortedRows('hod-assignments', assignments.assignments).map((assignment) => (
                      <tr key={assignment.assignmentId}>
                        <td>{assignment.title}</td>
                        <td>{assignment.subjectName} ({assignment.subjectCode})</td>
                        <td>{display(assignment.facultyName)}</td>
                        <td>{date(assignment.creationDate)}</td>
                        <td>{date(assignment.dueDate)}</td>
                        <td>{assignment.totalSubmissions}</td>
                        <td>{assignment.pendingSubmissions}</td>
                        <td>{assignment.gradingStatus}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <button className="btn btn-secondary btn-sm" style={{ marginTop: 10 }} type="button" onClick={() => navigate(`/departments/${departmentId}/assignments`)}>Open Assignment Management</button>
          </Section>

          <Section title="Academic Performance Overview">
            <div className="stats-grid" style={{ marginBottom: 12 }}>
              <Metric label="Department-wide performance" value={marks(performance.departmentAveragePerformance)} />
              <Metric label="Grade distribution" value={performance.gradeDistribution} />
            </div>
            <div className="admin-hod-distribution-grid">
              <PerformanceTable title="Year-wise Performance" values={performance.yearPerformance} year />
              <PerformanceTable title="Semester-wise Performance" values={performance.semesterPerformance} />
            </div>
            <h4 className="admin-hod-subheading">Students Requiring Academic Attention</h4>
            <StudentAttentionTable students={performance.studentsRequiringAttention} departmentId={departmentId} navigate={navigate} />
          </Section>

          <Section title="Administrative Information">
            <div className="grid-2">
              <Metric label="HOD appointment history" value={profile.administrativeInformation.appointmentHistory.length ? profile.administrativeInformation.appointmentHistory.join(', ') : null} />
              <Metric label="Departmental responsibilities" value={profile.administrativeInformation.responsibilities.length ? profile.administrativeInformation.responsibilities.join(', ') : null} />
              <Metric label="Administrative documents" value={profile.administrativeInformation.documents.length ? profile.administrativeInformation.documents.join(', ') : null} />
              <Metric label="Certifications" value={profile.administrativeInformation.certifications.length ? profile.administrativeInformation.certifications.join(', ') : null} />
            </div>
            <p className="admin-hod-note">Appointment history and administrative records are not stored by the current system.</p>
          </Section>
        </>
      )}
    </Layout>
  );
}

function PerformanceTable({ title, values, year = false }) {
  const { getSorts, getSortedRows, toggleSort } = useTableSorting();
  const tableId = `hod-performance-${title}`;
  return (
    <div className="admin-hod-distribution">
      <h4 className="admin-hod-subheading">{title}</h4>
      {!values.length ? (
        <div className="empty-state"><p>No performance marks are available.</p></div>
      ) : (
        <div className="table-container">
          <table>
            <thead><tr>
              <SortableTh tableId={tableId} columnKey="value" label={year ? 'Academic year' : 'Semester'} sorts={getSorts(tableId)} onSort={toggleSort} />
              <SortableTh tableId={tableId} columnKey="averageMarks" label="Class Performance" sorts={getSorts(tableId)} onSort={toggleSort} />
              <SortableTh tableId={tableId} columnKey="assessedRecords" label="Records" sorts={getSorts(tableId)} onSort={toggleSort} />
            </tr></thead>
            <tbody>{getSortedRows(tableId, values).map((entry) => <tr key={entry.value}><td>{year ? `Year ${entry.value}` : `Semester ${entry.value}`}</td><td>{marks(entry.averageMarks)}</td><td>{entry.assessedRecords}</td></tr>)}</tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function HodDirectory({ departmentId }) {
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    getAdminDepartmentHodDirectory(departmentId)
      .then((response) => {
        if (active) setData(response.data);
      })
      .catch((requestError) => {
        if (active) setError(errorMessage(requestError));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [departmentId]);

  const hod = data?.currentHod;
  return (
    <Layout pageTitle="HOD Management">
      <DepartmentSectionNav departmentId={departmentId} />
      <div className="page-header admin-hod-page-heading">
        <div>
          <span className="admin-hod-kicker">Department leadership</span>
          <h2 className="page-title">Head of Department</h2>
          <p className="page-subtitle">Current leadership and department overview.</p>
        </div>
      </div>

      {loading && <div className="loader"><div className="spinner" /></div>}
      {!loading && error && <div className="alert alert-error" role="alert">{error}</div>}
      {!loading && !error && (
        hod ? (
          <>
            <section className="card admin-hod-directory-hero">
              <div className="admin-hod-directory-identity">
                {hod.profileImageUrl ? (
                  <img src={hod.profileImageUrl} alt="" className="avatar admin-hod-directory-avatar" />
                ) : (
                  <div className="avatar admin-hod-directory-avatar" aria-hidden="true">{initials(hod.name)}</div>
                )}
                <div className="admin-hod-directory-details">
                  <div className="admin-hod-current-label"><span /> Current HOD</div>
                  <h3>{hod.name}</h3>
                  <p>{display(hod.hodId)} <span aria-hidden="true">·</span> {hod.departmentName} ({hod.departmentCode})</p>
                  {hod.email && <a className="admin-hod-email" href={`mailto:${hod.email}`}><Mail size={15} />{hod.email}</a>}
                </div>
              </div>
              <div className="admin-hod-directory-actions">
                <StatusBadge status={hod.status} />
                <button className="btn btn-primary" type="button" onClick={() => navigate(`/departments/${departmentId}/hod/${hod.userId}`)}>
                  View full profile <ArrowRight size={16} />
                </button>
              </div>
            </section>

            <div className="stats-grid admin-hod-directory-stats">
              <SummaryCard label="Faculty members" value={data.summary.totalFacultyMembers} icon={<Users size={18} />} detail="Assigned to this department" />
              <SummaryCard label="Students" value={data.summary.totalStudents} icon={<GraduationCap size={18} />} detail="Enrolled in this department" />
              <SummaryCard label="Subjects" value={data.summary.totalSubjects} icon={<BookOpen size={18} />} detail="Managed by this department" />
            </div>
          </>
        ) : (
          <section className="card admin-hod-empty">
            <div className="admin-hod-empty-icon"><Building2 size={26} /></div>
            <h3>No HOD assigned</h3>
            <p>This department does not have a current HOD. HOD history is not stored by the system.</p>
            <button className="btn btn-secondary" type="button" onClick={() => navigate('/departments')}>
              Return to Departments
            </button>
          </section>
        )
      )}
    </Layout>
  );
}

export default function AdminDepartmentHodPage({ departmentId, hodId }) {
  return hodId
    ? <HodProfile departmentId={departmentId} hodId={hodId} />
    : <HodDirectory departmentId={departmentId} />;
}
