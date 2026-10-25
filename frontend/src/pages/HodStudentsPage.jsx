import { useEffect, useRef, useState } from 'react';
import Layout from '../components/Layout';
import { getHodStudentDetails, getHodStudentRoster } from '../api/users';
import { ArrowDown, ArrowUp, GraduationCap, X } from 'lucide-react';

const SORT_OPTIONS = [
  { value: 'rollNumber', label: 'Roll number' },
  { value: 'name', label: 'Name' },
  { value: 'department', label: 'Department' },
  { value: 'attendancePercentage', label: 'Attendance percentage' },
  { value: 'classPerformance', label: 'Class performance' },
  { value: 'pendingAssignments', label: 'Pending assignments' },
];

const YEAR_LABELS = { 1: '1st', 2: '2nd', 3: '3rd', 4: '4th' };

function Metric({ label, value }) {
  return (
    <div style={{ padding: 14, background: 'var(--bg-secondary)', borderRadius: 8 }}>
      <div style={{ color: 'var(--text-muted)', fontSize: '0.72rem', textTransform: 'uppercase', marginBottom: 4 }}>{label}</div>
      <div style={{ fontWeight: 700 }}>{value ?? 'Not recorded'}</div>
    </div>
  );
}

function StudentDetailsModal({ student, details, loading, error, onClose }) {
  if (!student) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <section
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="student-details-title"
        style={{ maxWidth: 820, maxHeight: '90vh', overflowY: 'auto' }}
        onClick={(event) => event.stopPropagation()}
      >
        <div className="modal-header">
          <div>
            <h2 id="student-details-title" className="modal-title">{student.name}</h2>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>{student.rollNumber}</div>
          </div>
          <button className="btn btn-ghost btn-icon" onClick={onClose} aria-label="Close student details">
            <X size={18} />
          </button>
        </div>
        <div className="modal-body">
          {loading && <div className="loader"><div className="spinner" /></div>}
          {!loading && error && <div className="alert alert-error" role="alert">{error}</div>}
          {!loading && details && (
            <>
              <div className="stats-grid" style={{ marginBottom: 20 }}>
                <Metric label="Email" value={details.student.email} />
                <Metric label="Department" value={details.student.department?.name} />
                <Metric label="Academic year" value={details.student.year ? `${YEAR_LABELS[details.student.year]} year` : null} />
                <Metric label="Semester" value={details.student.semester} />
                <Metric label="Account status" value={details.student.status} />
                <Metric label="Account created" value={details.student.createdAt ? new Date(details.student.createdAt).toLocaleDateString() : null} />
                <Metric label="Attendance" value={details.student.attendancePercentage == null ? null : `${details.student.attendancePercentage}%`} />
                <Metric label="Class performance" value={details.student.classPerformance == null ? null : `${details.student.classPerformance}/10`} />
                <Metric label="Pending assignments" value={details.student.pendingAssignments} />
              </div>

              <section style={{ marginBottom: 22 }}>
                <h3 style={{ fontWeight: 700, marginBottom: 10 }}>Enrolled subjects</h3>
                {details.subjects.length === 0
                  ? <p style={{ color: 'var(--text-muted)' }}>No enrolled subjects recorded.</p>
                  : <div style={{ display: 'grid', gap: 8 }}>
                    {details.subjects.map((subject) => (
                      <div key={subject.id} style={{ display: 'flex', justifyContent: 'space-between', gap: 12, padding: 10, background: 'var(--bg-secondary)', borderRadius: 8 }}>
                        <span>{subject.name} <span style={{ color: 'var(--text-muted)' }}>({subject.code})</span></span>
                        <span style={{ color: 'var(--text-secondary)', whiteSpace: 'nowrap' }}>
                          {subject.academicYear ? `${YEAR_LABELS[subject.academicYear]} year` : 'Year not set'}
                          {subject.semester ? ` · Semester ${subject.semester}` : ''}
                        </span>
                      </div>
                    ))}
                  </div>}
              </section>

              <section style={{ marginBottom: 22 }}>
                <h3 style={{ fontWeight: 700, marginBottom: 10 }}>Attendance records</h3>
                {details.attendance.length === 0
                  ? <p style={{ color: 'var(--text-muted)' }}>No attendance records recorded.</p>
                  : <div style={{ overflowX: 'auto' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: 480 }}>
                      <thead><tr>{['Date', 'Subject', 'Status', 'Performance marks'].map((label) => <th key={label} style={tableHeaderStyle}>{label}</th>)}</tr></thead>
                      <tbody>{details.attendance.map((record) => (
                        <tr key={record.id}>
                          <td style={tableCellStyle}>{record.date}</td>
                          <td style={tableCellStyle}>{record.subjectName} ({record.subjectCode})</td>
                          <td style={tableCellStyle}>{record.status}</td>
                          <td style={tableCellStyle}>{record.performanceMarks ?? '—'}</td>
                        </tr>
                      ))}</tbody>
                    </table>
                  </div>}
              </section>

              <section>
                <h3 style={{ fontWeight: 700, marginBottom: 10 }}>Assignments</h3>
                {details.assignments.length === 0
                  ? <p style={{ color: 'var(--text-muted)' }}>No assignments recorded for enrolled subjects.</p>
                  : <div style={{ display: 'grid', gap: 10 }}>
                    {details.assignments.map((assignment) => (
                      <article key={assignment.id} style={{ padding: 12, background: 'var(--bg-secondary)', borderRadius: 8 }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
                          <strong>{assignment.title}</strong>
                          <span style={{ color: assignment.submitted ? 'var(--success)' : 'var(--warning)' }}>
                            {assignment.submitted ? assignment.submissionStatus : 'Not submitted'}
                          </span>
                        </div>
                        <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginTop: 4 }}>
                          {assignment.subjectName} ({assignment.subjectCode})
                          {assignment.dueDate ? ` · Due ${assignment.dueDate}` : ''}
                        </div>
                        {assignment.description && <p style={{ marginTop: 8 }}>{assignment.description}</p>}
                        {assignment.submitted && (
                          <div style={{ marginTop: 8, fontSize: '0.85rem' }}>
                            {assignment.submissionText && <p>{assignment.submissionText}</p>}
                            <div style={{ color: 'var(--text-secondary)' }}>
                              Submitted {assignment.submittedAt || '—'}
                              {assignment.marks != null ? ` · Marks: ${assignment.marks}/${assignment.maxMarks}` : ''}
                            </div>
                            {assignment.feedback && <p style={{ marginTop: 4 }}>Feedback: {assignment.feedback}</p>}
                          </div>
                        )}
                      </article>
                    ))}
                  </div>}
              </section>
            </>
          )}
        </div>
      </section>
    </div>
  );
}

const tableHeaderStyle = {
  textAlign: 'left',
  padding: '10px 12px',
  color: 'var(--text-muted)',
  fontSize: '0.72rem',
  textTransform: 'uppercase',
  borderBottom: '1px solid var(--border)',
};

const tableCellStyle = {
  padding: '10px 12px',
  borderBottom: '1px solid var(--border)',
  verticalAlign: 'top',
};

export default function HodStudentsPage() {
  const [year, setYear] = useState('');
  const [sortBy, setSortBy] = useState('rollNumber');
  const [direction, setDirection] = useState('asc');
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [details, setDetails] = useState(null);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [detailsError, setDetailsError] = useState('');
  const detailsRequest = useRef(0);

  useEffect(() => {
    if (!year) {
      setStudents([]);
      setError('');
      return undefined;
    }

    let active = true;
    setLoading(true);
    setError('');
    getHodStudentRoster(Number(year), sortBy, direction)
      .then((response) => {
        if (active) setStudents(response.data || []);
      })
      .catch((requestError) => {
        if (active) {
          setStudents([]);
          setError(requestError.response?.data?.message || 'Failed to load students. Please try again.');
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => { active = false; };
  }, [year, sortBy, direction]);

  const openStudent = async (student) => {
    const requestId = ++detailsRequest.current;
    setSelectedStudent(student);
    setDetails(null);
    setDetailsError('');
    setDetailsLoading(true);
    try {
      const response = await getHodStudentDetails(student.id);
      if (requestId === detailsRequest.current) setDetails(response.data);
    } catch (requestError) {
      if (requestId === detailsRequest.current) {
        setDetailsError(requestError.response?.data?.message || 'Failed to load student details. Please try again.');
      }
    } finally {
      if (requestId === detailsRequest.current) setDetailsLoading(false);
    }
  };

  const closeDetails = () => {
    detailsRequest.current += 1;
    setSelectedStudent(null);
    setDetails(null);
    setDetailsError('');
  };

  const sortLabel = SORT_OPTIONS.find((option) => option.value === sortBy)?.label;

  return (
    <Layout pageTitle="Student Roster">
      <div style={{ marginBottom: 20 }}>
        <h2 style={{ fontSize: '1.45rem', fontWeight: 800, marginBottom: 4 }}>Department students</h2>
        <p style={{ color: 'var(--text-secondary)' }}>
          Choose an academic year to view students in your assigned department(s). Metrics are shown only when recorded data is available.
        </p>
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <div style={{ display: 'flex', alignItems: 'end', gap: 16, flexWrap: 'wrap' }}>
          <div className="form-group" style={{ marginBottom: 0, minWidth: 180 }}>
            <label className="form-label" htmlFor="roster-year">Academic year</label>
            <select id="roster-year" className="form-input" value={year} onChange={(event) => setYear(event.target.value)}>
              <option value="">Select a year</option>
              {[1, 2, 3, 4].map((value) => (
                <option key={value} value={value}>{YEAR_LABELS[value]} year</option>
              ))}
            </select>
          </div>
          <div className="form-group" style={{ marginBottom: 0, minWidth: 220 }}>
            <label className="form-label" htmlFor="roster-sort">Sort students by</label>
            <select id="roster-sort" className="form-input" value={sortBy} onChange={(event) => setSortBy(event.target.value)}>
              {SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </div>
          <button
            className="btn btn-secondary"
            type="button"
            aria-label={`Sort ${sortLabel} ${direction === 'asc' ? 'descending' : 'ascending'}`}
            onClick={() => setDirection((current) => current === 'asc' ? 'desc' : 'asc')}
          >
            {direction === 'asc' ? <ArrowUp size={16} /> : <ArrowDown size={16} />}
            {direction === 'asc' ? 'Ascending' : 'Descending'}
          </button>
        </div>
      </div>

      {!year && (
        <div className="card text-center" style={{ padding: 44, color: 'var(--text-secondary)' }}>
          <GraduationCap size={32} style={{ color: 'var(--accent)', marginBottom: 10 }} />
          <p>Select an academic year to load the student roster.</p>
        </div>
      )}
      {year && loading && <div className="loader"><div className="spinner" /></div>}
      {year && !loading && error && <div className="alert alert-error" role="alert">{error}</div>}
      {year && !loading && !error && students.length === 0 && (
        <div className="card text-center" style={{ padding: 36, color: 'var(--text-secondary)' }}>
          No students found for the {YEAR_LABELS[year]} year in your assigned department(s).
        </div>
      )}
      {year && !loading && !error && students.length > 0 && (
        <div className="card" style={{ overflowX: 'auto', padding: 0 }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: 760 }}>
            <thead>
              <tr>
                {['Roll number', 'Student', 'Department', 'Attendance', 'Class performance', 'Pending assignments'].map((label) => (
                  <th key={label} style={tableHeaderStyle}>{label}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {students.map((student) => (
                <tr key={student.id}>
                  <td style={tableCellStyle}>{student.rollNumber}</td>
                  <td style={tableCellStyle}>
                    <button
                      type="button"
                      className="btn btn-ghost"
                      style={{ padding: 0, color: 'var(--accent)', fontWeight: 600 }}
                      onClick={() => openStudent(student)}
                    >
                      {student.name}
                    </button>
                  </td>
                  <td style={tableCellStyle}>{student.department?.code || student.department?.name || '—'}</td>
                  <td style={tableCellStyle}>{student.attendancePercentage == null ? '—' : `${student.attendancePercentage}%`}</td>
                  <td style={tableCellStyle}>{student.classPerformance == null ? '—' : `${student.classPerformance}/10`}</td>
                  <td style={tableCellStyle}>{student.pendingAssignments}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div style={{ padding: '12px 16px', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
            {students.length} student{students.length === 1 ? '' : 's'} · sorted by {sortLabel.toLowerCase()} ({direction})
          </div>
        </div>
      )}

      <StudentDetailsModal
        student={selectedStudent}
        details={details}
        loading={detailsLoading}
        error={detailsError}
        onClose={closeDetails}
      />
    </Layout>
  );
}
