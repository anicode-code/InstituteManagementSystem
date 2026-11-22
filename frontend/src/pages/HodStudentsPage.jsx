import { useEffect, useMemo, useRef, useState } from 'react';
import Layout from '../components/Layout';
import SortableTh from '../components/SortableTh';
import { getHodStudentDetails, getHodStudentRoster } from '../api/users';
import { sortRows } from '../utils/tableSorting';
import { GraduationCap, Search, X } from 'lucide-react';

const SORT_COLUMNS = [
  { key: 'rollNumber', label: 'Institute ID' },
  { key: 'name', label: 'Student' },
  { key: 'semester', label: 'Semester' },
  { key: 'attendancePercentage', label: 'Attendance' },
  { key: 'classPerformance', label: 'Class performance' },
  { key: 'pendingAssignments', label: 'Pending assignments' },
];

const YEAR_LABELS = { 1: '1st', 2: '2nd', 3: '3rd', 4: '4th' };
const SEMESTERS = [1, 2, 3, 4, 5, 6, 7, 8];

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

const rosterHeaderStyle = {
  ...tableHeaderStyle,
  padding: '14px 16px',
  letterSpacing: '0.04em',
};

const rosterCellStyle = {
  ...tableCellStyle,
  padding: '13px 16px',
  verticalAlign: 'middle',
};

const rosterCenteredCellStyle = {
  ...rosterCellStyle,
  textAlign: 'center',
  fontVariantNumeric: 'tabular-nums',
};

const centeredSortColumns = new Set([
  'semester',
  'attendancePercentage',
  'classPerformance',
  'pendingAssignments',
]);

export default function HodStudentsPage() {
  const [semester, setSemester] = useState('');
  const [search, setSearch] = useState('');
  const [sorts, setSorts] = useState([]);
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [details, setDetails] = useState(null);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [detailsError, setDetailsError] = useState('');
  const detailsRequest = useRef(0);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    getHodStudentRoster(semester)
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
  }, [semester]);

  const toggleSort = (_, key) => {
    setSorts((current) => {
      const selectedSort = current.find((sort) => sort.key === key);
      if (!selectedSort) return [...current, { key, direction: 'asc' }];
      if (selectedSort.direction === 'asc') {
        return current.map((sort) => sort.key === key ? { ...sort, direction: 'desc' } : sort);
      }
      return current.filter((sort) => sort.key !== key);
    });
  };
  const filteredStudents = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return students;
    return students.filter((student) => [
      student.name,
      student.rollNumber,
      student.email,
      student.semester ? `Semester ${student.semester}` : '',
      student.departmentName,
    ].filter(Boolean).some((value) => String(value).toLowerCase().includes(query)));
  }, [students, search]);
  const sortedStudents = useMemo(() => sortRows(filteredStudents, sorts), [filteredStudents, sorts]);

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

  return (
    <Layout pageTitle="Student List">
      <div style={{ marginBottom: 20 }}>
        <h2 style={{ fontSize: '1.45rem', fontWeight: 800, marginBottom: 4 }}>Department students</h2>
        <p style={{ color: 'var(--text-secondary)' }}>
          View students in your assigned department(s). Metrics are shown only when recorded data is available.
        </p>
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <div style={{ display: 'flex', alignItems: 'end', gap: 16, flexWrap: 'wrap' }}>
          <div className="form-group" style={{ marginBottom: 0, minWidth: 180 }}>
            <label className="form-label" htmlFor="roster-semester">Semester</label>
            <select id="roster-semester" className="form-input" value={semester} onChange={(event) => setSemester(event.target.value)}>
              <option value="">All Semesters</option>
              {SEMESTERS.map((value) => (
                <option key={value} value={value}>Semester {value}</option>
              ))}
            </select>
          </div>
          <div className="search-bar" style={{ maxWidth: 360, flex: '1 1 240px' }}>
            <Search />
            <input
              aria-label="Search students"
              placeholder="Search name or Institute ID..."
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
        </div>
      </div>

      {loading && <div className="loader"><div className="spinner" /></div>}
      {!loading && error && <div className="alert alert-error" role="alert">{error}</div>}
      {!loading && !error && students.length === 0 && (
        <div className="card text-center" style={{ padding: 36, color: 'var(--text-secondary)' }}>
          No students found{semester ? ` for Semester ${semester}` : ''} in your assigned department(s).
        </div>
      )}
      {!loading && !error && students.length > 0 && filteredStudents.length === 0 && (
        <div className="card text-center" style={{ padding: 36, color: 'var(--text-secondary)' }}>
          <Search style={{ display: 'block', margin: '0 auto 8px', color: 'var(--text-muted)' }} />
          No students match your search.
        </div>
      )}
      {!loading && !error && filteredStudents.length > 0 && (
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <div className="table-container hod-roster-table-container" style={{ border: 0, borderRadius: 0 }}>
            <table className="hod-roster-table" style={{ minWidth: 820 }}>
              <thead>
                <tr>
                  {SORT_COLUMNS.map(({ key, label }) => (
                    <SortableTh
                      key={key}
                      tableId="hod-students"
                      columnKey={key}
                      label={label}
                      sorts={sorts}
                      onSort={toggleSort}
                      style={{
                        ...rosterHeaderStyle,
                        textAlign: centeredSortColumns.has(key) ? 'center' : 'left',
                      }}
                    />
                  ))}
                </tr>
              </thead>
              <tbody>
                {sortedStudents.map((student) => (
                  <tr key={student.id}>
                    <td style={rosterCellStyle}>{student.rollNumber}</td>
                    <td style={rosterCellStyle}>
                      <button
                        type="button"
                        className="btn btn-ghost"
                        style={{ padding: 0, color: 'var(--accent)', fontWeight: 600 }}
                        onClick={() => openStudent(student)}
                      >
                        {student.name}
                      </button>
                    </td>
                    <td style={rosterCenteredCellStyle}>{student.semester ? `Semester ${student.semester}` : '—'}</td>
                    <td style={rosterCenteredCellStyle}>{student.attendancePercentage == null ? '—' : `${student.attendancePercentage}%`}</td>
                    <td style={rosterCenteredCellStyle}>{student.classPerformance == null ? '—' : `${student.classPerformance}/10`}</td>
                    <td style={rosterCenteredCellStyle}>{student.pendingAssignments}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div style={{ padding: '12px 16px', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
            {filteredStudents.length} of {students.length} student{students.length === 1 ? '' : 's'}
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
