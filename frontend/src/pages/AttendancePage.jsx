import { useEffect, useState, useCallback, useMemo, useRef } from 'react';
import Layout from '../components/Layout';
import { useAuth } from '../context/AuthContext';
import SortableTh from '../components/SortableTh';
import { useTableSorting } from '../utils/tableSorting';
import {
  getAttendanceBySubject,
  getAttendanceByStudent,
  createAttendance,
  recordDayBatch,
  deleteDayAttendance,
} from '../api/attendance';
import { getAllSubjects, getAllSubjectsForAttendance, getSubjectsByFaculty, getSubjectsByStudent } from '../api/subjects';
import { getAllDepartments } from '../api/departments';
import { getAllUsers } from '../api/users';
import {
  CalendarCheck, Plus, Search, TrendingUp, AlertTriangle,
  BookOpen, Trash2, X, Check, Calendar, ChevronRight,
} from 'lucide-react';
import toast from 'react-hot-toast';
import DepartmentSectionNav from '../components/DepartmentSectionNav';

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

function isStudentEnrolledInSubject(student, subject) {
  const explicitlySelected = student.selectedSubjects?.some(
    (selected) => Number(selected.id) === Number(subject.id)
  ) ?? false;
  const hasMatchingDepartment = student.department?.id != null
    && subject.department?.id != null
    && Number(student.department.id) === Number(subject.department.id);
  const hasMatchingSemester = student.semester != null
    && subject.semester != null
    && Number(student.semester) === Number(subject.semester);
  const isCompulsoryForStudent = subject.type === 'COMPULSORY'
    && hasMatchingDepartment
    && hasMatchingSemester;

  return explicitlySelected || isCompulsoryForStudent;
}

export default function AttendancePage({ departmentId }) {
  const { user, isAdmin, isFaculty, isStudent, isAdminOrHod } = useAuth();
  const { getSorts, getSortedRows, toggleSort } = useTableSorting();
  const attendanceSorts = getSorts('attendance-matrix');

  const [subjects, setSubjects] = useState([]);
  const [editableSubjectIds, setEditableSubjectIds] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [selectedDepartmentId, setSelectedDepartmentId] = useState(departmentId || '');
  const [selectedSemester, setSelectedSemester] = useState('');
  const [selectedSubjectId, setSelectedSubjectId] = useState(null);
  const [students, setStudents] = useState([]);
  const [records, setRecords] = useState([]);
  const attendanceRequestId = useRef(0);
  const [loading, setLoading] = useState(true);
  const [gridLoading, setGridLoading] = useState(false);
  const [search, setSearch] = useState('');

  // Add Day modal
  const [addDayModalOpen, setAddDayModalOpen] = useState(false);
  const [newDayDate, setNewDayDate] = useState(new Date().toISOString().split('T')[0]);
  const [defaultPresent, setDefaultPresent] = useState(true);
  const [submittingDay, setSubmittingDay] = useState(false);

  useEffect(() => {
    if (!departmentId) return;
    setSelectedDepartmentId(String(departmentId));
    setSelectedSemester('');
    setSelectedSubjectId(null);
    setRecords([]);
  }, [departmentId]);

  // Load Subjects and Verified Students once
  useEffect(() => {
    const init = async () => {
      setLoading(true);
      try {
        const [subRes, userRes, departmentRes, assignedSubjectRes] = await Promise.all([
          isFaculty
            ? getSubjectsByFaculty(user.id)
            : (isStudent
              ? getSubjectsByStudent(user.id)
              : (user?.role === 'HOD' ? getAllSubjectsForAttendance() : getAllSubjects())),
          isStudent ? Promise.resolve({ data: [] }) : getAllUsers(),
          user?.role === 'ADMIN' ? getAllDepartments() : Promise.resolve({ data: [] }),
          user?.role === 'HOD' ? getSubjectsByFaculty(user.id) : Promise.resolve({ data: [] }),
        ]);

        const loadedSubjects = subRes.data || [];
        setSubjects(loadedSubjects);
        setEditableSubjectIds((assignedSubjectRes.data || []).map((subject) => Number(subject.id)));
        setDepartments(departmentRes.data || []);
        if (departmentId) setSelectedDepartmentId(String(departmentId));

        // Filter all verified students
        const allVerifiedStudents = (userRes.data || []).filter(
          (u) => u.role === 'STUDENT' && u.status === 'VERIFIED'
        );
        setStudents(allVerifiedStudents);

        if (!isAdminOrHod && loadedSubjects.length > 0) {
          setSelectedSubjectId(loadedSubjects[0].id);
        }
      } catch (err) {
        toast.error('Failed to load subjects or students');
      } finally {
        setLoading(false);
      }
    };
    init();
  }, [user, isFaculty, isStudent, isAdminOrHod, departmentId]);

  const availableSemesters = useMemo(() => {
    const departmentSubjects = subjects.filter((subject) =>
      !selectedDepartmentId || String(subject.department?.id) === selectedDepartmentId
    );
    return [...new Set(departmentSubjects.map((subject) => subject.semester).filter(Boolean))]
      .sort((a, b) => a - b);
  }, [subjects, selectedDepartmentId]);

  const availableSubjects = useMemo(() => subjects.filter((subject) =>
    (!isAdminOrHod || !selectedSemester || String(subject.semester) === selectedSemester)
    && (user?.role !== 'ADMIN' || !selectedDepartmentId || String(subject.department?.id) === selectedDepartmentId)
  ), [subjects, isAdminOrHod, user, selectedSemester, selectedDepartmentId]);

  const selectedSubject = useMemo(
    () => {
      if (isAdminOrHod && !selectedSemester) return undefined;
      if (user?.role === 'ADMIN' && !selectedDepartmentId) return undefined;
      return availableSubjects.find((s) => s.id === Number(selectedSubjectId));
    },
    [availableSubjects, isAdminOrHod, selectedSubjectId, selectedSemester, selectedDepartmentId, user]
  );
  const canEditAttendance = isFaculty || isAdmin
    || (user?.role === 'HOD' && editableSubjectIds.includes(Number(selectedSubject?.id)));

  // Load attendance records whenever selectedSubjectId changes
  const loadSubjectAttendance = useCallback(async () => {
    if (!selectedSubject) {
      attendanceRequestId.current += 1;
      setRecords([]);
      setGridLoading(false);
      return;
    }
    const requestId = ++attendanceRequestId.current;
    setGridLoading(true);
    try {
      if (isStudent) {
        const res = await getAttendanceByStudent(user.id);
        // Filter student's attendance records to this subject
        const subjectRecords = (res.data || []).filter(
          (r) => r.subject?.id === Number(selectedSubjectId)
        );
        if (attendanceRequestId.current === requestId) setRecords(subjectRecords);
      } else {
        const res = await getAttendanceBySubject(selectedSubjectId);
        if (attendanceRequestId.current === requestId) setRecords(res.data || []);
      }
    } catch (err) {
      if (attendanceRequestId.current === requestId) toast.error('Failed to load attendance records');
    } finally {
      if (attendanceRequestId.current === requestId) setGridLoading(false);
    }
  }, [selectedSubject, selectedSubjectId, isStudent, user]);

  useEffect(() => {
    loadSubjectAttendance();
  }, [loadSubjectAttendance]);

  // Extract sorted distinct dates (Day 1, Day 2, ...) for this subject
  const distinctDays = useMemo(() => {
    const dates = [...new Set(records.map((r) => r.date))].sort();
    return dates.map((dateStr, idx) => {
      const parts = dateStr.split('-');
      const shortDate = `${parts[2]}/${parts[1]}`;
      const d = new Date(dateStr);
      const fullDate = d.toLocaleDateString('en-US', {
        weekday: 'short',
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      });

      // Stats for this day
      const dayRecords = records.filter((r) => r.date === dateStr);
      const presentCount = dayRecords.filter(
        (r) => r.status === 'PRESENT'
      ).length;
      const absentCount = dayRecords.filter((r) => r.status === 'ABSENT').length;
      const totalMarked = dayRecords.length;
      const dayPct = totalMarked > 0 ? Math.round((presentCount / totalMarked) * 100) : 0;

      // Avg performance marks
      const marksRecords = dayRecords.filter(
        (r) => r.performanceMarks !== null && r.performanceMarks !== undefined
      );
      const avgMarks = marksRecords.length
        ? (marksRecords.reduce((sum, r) => sum + r.performanceMarks, 0) / marksRecords.length).toFixed(1)
        : null;

      return {
        dayNumber: idx + 1,
        date: dateStr,
        shortDate,
        fullDate,
        presentCount,
        absentCount,
        dayPct,
        avgMarks,
        isConducted: true, // Class was taken
      };
    });
  }, [records]);

  const personalRecordsByDate = useMemo(
    () => new Map(records.map((record) => [record.date, record])),
    [records],
  );

  const personalAttendanceStats = useMemo(() => {
    const statuses = distinctDays.map((day) => personalRecordsByDate.get(day.date)?.status);
    const attended = statuses.filter((status) => status === 'PRESENT').length;
    const missed = statuses.filter((status) => status === 'ABSENT').length;
    const totalClasses = distinctDays.length;
    return {
      totalClasses,
      attended,
      missed,
      percentage: totalClasses ? Math.round((attended / totalClasses) * 100) : 0,
    };
  }, [distinctDays, personalRecordsByDate]);

  // Fast cell record lookup: `${studentId}_${date}`
  const cellMap = useMemo(() => {
    const map = new Map();
    for (const r of records) {
      if (r.student?.id && r.date) {
        map.set(`${r.student.id}_${r.date}`, r);
      }
    }
    return map;
  }, [records]);

  const eligibleStudents = useMemo(() => {
    let list = students;
    if (isStudent) {
      list = list.filter((s) => s.id === user.id);
    }
    if (user?.role === 'ADMIN' && selectedSubject) {
      list = list.filter((student) =>
        String(student.department?.id) === String(selectedSubject.department?.id)
        && Number(student.semester) === Number(selectedSubject.semester)
      );
    }
    if ((isFaculty || user?.role === 'HOD') && selectedSubject) {
      list = list.filter((student) => isStudentEnrolledInSubject(student, selectedSubject));
    }
    return list;
  }, [students, isStudent, isFaculty, selectedSubject, user]);

  // Filter the selected subject's roster by search
  const visibleStudents = useMemo(() => {
    let list = eligibleStudents;
    if (!search.trim()) return list;
    const q = search.toLowerCase();
    return list.filter(
      (s) =>
        s.name?.toLowerCase().includes(q) ||
        s.rollNumber?.toLowerCase().includes(q) ||
        s.email?.toLowerCase().includes(q)
    );
  }, [eligibleStudents, search]);
  // Student subject-level statistics
  const studentStatsMap = useMemo(() => {
    const totalDays = distinctDays.length;
    const stats = new Map();

    for (const stu of students) {
      let attended = 0;
      let absent = 0;
      let totalMarks = 0;
      let marksCount = 0;

      for (const day of distinctDays) {
        const cell = cellMap.get(`${stu.id}_${day.date}`);
        if (cell) {
          if (cell.status === 'PRESENT') attended++;
          else if (cell.status === 'ABSENT') absent++;

          if (cell.performanceMarks !== null && cell.performanceMarks !== undefined) {
            totalMarks += cell.performanceMarks;
            marksCount++;
          }
        }
      }

      const pct = totalDays > 0 ? Math.round((attended / totalDays) * 100) : 0;
      const avgScore = marksCount > 0 ? (totalMarks / marksCount).toFixed(1) : null;

      stats.set(stu.id, {
        attended,
        absent,
        totalDays,
        pct,
        avgScore,
        isLow: totalDays > 0 && pct < 75,
      });
    }

    return stats;
  }, [students, distinctDays, cellMap]);

  const sortedVisibleStudents = isAdmin
    ? getSortedRows('attendance-matrix', visibleStudents, {
      overallAttendance: (student) => studentStatsMap.get(student.id)?.pct,
      ...Object.fromEntries(distinctDays.map((day) => [
        `day-${day.date}`,
        (student) => {
          const status = cellMap.get(`${student.id}_${day.date}`)?.status;
          return status === 'PRESENT' ? 2 : status === 'ABSENT' ? 1 : 0;
        },
      ])),
    })
    : visibleStudents;

  // Overall Class Average across all students
  const overallClassAverage = useMemo(() => {
    if (distinctDays.length === 0 || visibleStudents.length === 0) return 0;
    const totalPcts = visibleStudents.reduce((acc, stu) => {
      const s = studentStatsMap.get(stu.id);
      return acc + (s?.pct || 0);
    }, 0);
    return Math.round(totalPcts / visibleStudents.length);
  }, [distinctDays, visibleStudents, studentStatsMap]);

  // Toggle cell attendance (cycles: P <-> A)
  const handleToggleStatus = async (studentId, date) => {
    if (!canEditAttendance) return;

    const key = `${studentId}_${date}`;
    const current = cellMap.get(key);
    // If currently PRESENT, toggle to ABSENT; otherwise toggle to PRESENT
    const nextStatus = current?.status === 'PRESENT' ? 'ABSENT' : 'PRESENT';

    // Optimistic UI update
    const updatedRecord = {
      ...(current || {}),
      id: current?.id || Date.now(),
      student: { id: studentId },
      subject: { id: Number(selectedSubjectId) },
      date,
      status: nextStatus,
      performanceMarks: current?.performanceMarks || null,
    };

    setRecords((prev) => {
      const existingIdx = prev.findIndex((r) => r.student?.id === studentId && r.date === date);
      if (existingIdx >= 0) {
        const copy = [...prev];
        copy[existingIdx] = updatedRecord;
        return copy;
      }
      return [...prev, updatedRecord];
    });

    try {
      const res = await createAttendance({
        studentId,
        subjectId: selectedSubjectId,
        date,
        status: nextStatus,
        performanceMarks: current?.performanceMarks,
      });
      // Replace with real backend record
      setRecords((prev) =>
        prev.map((r) =>
          r.student?.id === studentId && r.date === date ? res.data : r
        )
      );
    } catch (err) {
      toast.error('Failed to update attendance');
      loadSubjectAttendance();
    }
  };

  // Update cell performance marks
  const handlePerformanceMarksChange = async (studentId, date, value) => {
    if (!canEditAttendance) return;
    const key = `${studentId}_${date}`;
    const current = cellMap.get(key);
    const marksNum = value.trim() === '' ? null : Number(value);

    // Optimistic local update
    setRecords((prev) =>
      prev.map((r) =>
        r.student?.id === studentId && r.date === date
          ? { ...r, performanceMarks: marksNum }
          : r
      )
    );

    try {
      await createAttendance({
        studentId,
        subjectId: selectedSubjectId,
        date,
        status: current?.status || 'PRESENT',
        performanceMarks: marksNum,
      });
    } catch (err) {
      toast.error('Failed to save performance mark');
    }
  };

  // Add Day / Session handler
  const handleAddDay = async (e) => {
    e.preventDefault();
    if (!canEditAttendance) return;
    if (!newDayDate) {
      toast.error('Please select a valid date');
      return;
    }

    if (distinctDays.some((d) => d.date === newDayDate)) {
      toast.error(`A session for date ${newDayDate} already exists!`);
      return;
    }

    setSubmittingDay(true);
    try {
      const initialStatus = defaultPresent ? 'PRESENT' : 'ABSENT';
      const entries = eligibleStudents.map((s) => ({
        studentId: s.id,
        status: initialStatus,
        performanceMarks: null,
      }));

      await recordDayBatch(selectedSubjectId, newDayDate, entries);
      toast.success(`Day ${distinctDays.length + 1} (${newDayDate}) created!`);
      setAddDayModalOpen(false);
      await loadSubjectAttendance();
    } catch (err) {
      toast.error(err?.response?.data?.message || err?.message || 'Failed to add day session');
    } finally {
      setSubmittingDay(false);
    }
  };

  // Delete a day column
  const handleDeleteDay = async (dateStr, dayNum) => {
    if (!canEditAttendance) return;
    if (!window.confirm(`Delete all attendance for Day ${dayNum} (${dateStr})?`)) return;
    try {
      await deleteDayAttendance(selectedSubjectId, dateStr);
      toast.success(`Day ${dayNum} attendance deleted`);
      loadSubjectAttendance();
    } catch (err) {
      toast.error('Failed to delete day session');
    }
  };

  return (
    <Layout pageTitle="Attendance Register">
      {departmentId && <DepartmentSectionNav departmentId={departmentId} />}
      <div className="page-header" style={{ marginBottom: 16 }}>
        <div>
          <h2 className="page-title">Attendance Register</h2>
          <p className="page-subtitle">
            {isFaculty
              ? 'Multi-day spreadsheet attendance & performance grading'
              : (isStudent ? 'Your subject-wise attendance history' : 'Institute attendance matrix')}
          </p>
        </div>
        {canEditAttendance && selectedSubject && (
          <button className="btn btn-primary" onClick={() => setAddDayModalOpen(true)}>
            <Plus size={18} /> Add Day / Class
          </button>
        )}
      </div>

      {/* ──────── 8. Subject Selection at Top ──────── */}
      <div className="card" style={{ padding: '16px 20px', marginBottom: 20 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 14 }}>
          <div className="attendance-filter-row">
            {user?.role === 'ADMIN' && !departmentId && (
              <label className="attendance-filter">
                <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-secondary)' }}>Department:</span>
                <select className="form-select" value={selectedDepartmentId} onChange={(e) => {
                  setSelectedDepartmentId(e.target.value);
                  setSelectedSemester('');
                  setSelectedSubjectId(null);
                  setRecords([]);
                }}>
                  <option value="">Select department</option>
                  {departments.map((department) => <option key={department.id} value={department.id}>{department.name}</option>)}
                </select>
              </label>
            )}
            {isAdminOrHod ? (
              <>
                <label className="attendance-filter">
                  <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-secondary)' }}>Semester:</span>
                  <select
                    className="form-select"
                    value={selectedSemester}
                    disabled={user?.role === 'ADMIN' && !selectedDepartmentId}
                    onChange={(e) => {
                      setSelectedSemester(e.target.value);
                      setSelectedSubjectId(null);
                      setRecords([]);
                    }}
                  >
                    <option value="">Select semester</option>
                    {availableSemesters.map((semester) => <option key={semester} value={semester}>Semester {semester}</option>)}
                  </select>
                </label>
                <label className="attendance-filter">
                  <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-secondary)' }}>
                    <BookOpen size={17} color="var(--accent)" /> Subject:
                  </span>
                  <select
                    className="form-select"
                    value={selectedSubjectId || ''}
                    disabled={!selectedSemester || (user?.role === 'ADMIN' && !selectedDepartmentId)}
                    onChange={(e) => {
                      setSelectedSubjectId(e.target.value ? Number(e.target.value) : null);
                      setRecords([]);
                    }}
                  >
                    <option value="">Select subject</option>
                    {availableSubjects.map((subject) => (
                      <option key={subject.id} value={subject.id}>
                        {subject.name} ({subject.code}) · {subject.type === 'OPTIONAL' ? 'Optional' : 'Compulsory'}
                      </option>
                    ))}
                  </select>
                </label>
              </>
            ) : isStudent ? (
              <label className="attendance-filter">
                <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: 6 }}>
                  <BookOpen size={17} color="var(--accent)" /> Select Subject:
                </span>
                <select
                  className="form-select"
                  value={selectedSubjectId || ''}
                  disabled={loading || subjects.length === 0}
                  onChange={(e) => {
                    setSelectedSubjectId(e.target.value ? Number(e.target.value) : null);
                    setRecords([]);
                  }}
                >
                  <option value="">Select subject</option>
                  {subjects.map((subject) => (
                    <option key={subject.id} value={subject.id}>
                      {subject.name} ({subject.code}) · {subject.type === 'OPTIONAL' ? 'Optional' : 'Compulsory'}
                    </option>
                  ))}
                </select>
              </label>
            ) : isFaculty ? (
              <label className="attendance-filter">
                <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: 6 }}>
                  <BookOpen size={17} color="var(--accent)" /> Select Subject:
                </span>
                <select
                  className="form-select"
                  value={selectedSubjectId || ''}
                  onChange={(e) => {
                    setSelectedSubjectId(e.target.value ? Number(e.target.value) : null);
                    setRecords([]);
                  }}
                >
                  <option value="">Select subject</option>
                  {subjects.map((subject) => (
                    <option key={subject.id} value={subject.id}>
                      {subject.name} ({subject.code}) · {subject.type === 'OPTIONAL' ? 'Optional' : 'Compulsory'}
                    </option>
                  ))}
                </select>
              </label>
            ) : (
              <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                Select a subject to view your attendance history.
              </div>
            )}
          </div>

          {selectedSubject && !isStudent && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 16, fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              <span>Faculty: <strong>{
                selectedSubject.facultyMembers?.length
                  ? selectedSubject.facultyMembers.map((faculty) => faculty.name).join(', ')
                  : (selectedSubject.faculty?.name || '—')
              }</strong></span>
              <span>Classes Taken: <strong style={{ color: 'var(--accent)' }}>{distinctDays.length}</strong></span>
              <span>Class Avg: <strong style={{ color: overallClassAverage >= 75 ? 'var(--success)' : 'var(--danger)' }}>{overallClassAverage}%</strong></span>
            </div>
          )}
        </div>
      </div>

      {/* Student Low Attendance Alert */}
      {isStudent && selectedSubject && personalAttendanceStats.totalClasses > 0
        && personalAttendanceStats.percentage < 75 && (
        <div className="alert" style={{
          background: 'rgba(239, 68, 68, 0.1)', color: 'var(--danger)',
          border: '1px solid var(--danger)', marginBottom: 20,
          display: 'flex', alignItems: 'center', gap: 12,
        }}>
          <AlertTriangle size={20} />
          <div>
            <strong>Low Attendance Alert:</strong> Your attendance in {selectedSubject.name} is {personalAttendanceStats.percentage}% ({personalAttendanceStats.attended}/{personalAttendanceStats.totalClasses} classes).
            The minimum requirement is 75%.
          </div>
        </div>
      )}

      {/* Toolbar: Search and Quick Legend */}
      {!isStudent && <div className="toolbar" style={{ marginBottom: 14 }}>
        <div className="toolbar-left">
          <div className="search-bar" style={{ maxWidth: 320 }}>
            <Search size={16} />
            <input
              placeholder="Search student by name or Institute ID..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, fontSize: '0.78rem', color: 'var(--text-muted)' }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              <span style={{ width: 10, height: 10, borderRadius: 2, background: 'var(--success)' }} />
              P = Present
            </span>
            <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              <span style={{ width: 10, height: 10, borderRadius: 2, background: 'var(--danger)' }} />
              A = Absent
            </span>
            <span>★ = Performance (0-10)</span>
          </div>
        </div>
        <div className="toolbar-right">
          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            {visibleStudents.length} Students · {distinctDays.length} Days Conducted
          </span>
        </div>
      </div>}

      {/* ──────── 1-7, 9. The Attendance Matrix Grid ──────── */}
      {loading || gridLoading ? (
        <div className="card" style={{ padding: 40, textAlign: 'center' }}>
          <div className="loader"><div className="spinner" /></div>
          <p style={{ marginTop: 12, color: 'var(--text-muted)', fontSize: '0.85rem' }}>Loading attendance grid...</p>
        </div>
      ) : !selectedSubject ? (
        <div className="empty-state">
          <BookOpen />
          <h3>
            {isAdminOrHod && ((user?.role === 'ADMIN' && !selectedDepartmentId) || !selectedSemester)
              ? 'Select attendance filters'
              : (isStudent && subjects.length ? 'Select a subject' : 'No subjects available')}
          </h3>
          <p>
            {isAdminOrHod && ((user?.role === 'ADMIN' && !selectedDepartmentId) || !selectedSemester)
              ? 'Choose the required department and semester, then select a subject to view attendance.'
              : (isAdminOrHod
                ? 'There are no subjects for the selected department and semester.'
                : (isStudent && subjects.length
                  ? 'Choose a subject from the dropdown above to view your attendance.'
                  : 'You have not been assigned to any subjects yet.'))}
          </p>
        </div>
      ) : isStudent ? (
        <div>
          <div className="stats-grid" style={{ marginBottom: 16 }}>
            <SummaryCard
              label="Total classes conducted"
              value={personalAttendanceStats.totalClasses}
              icon={<CalendarCheck size={18} />}
              detail={`For ${selectedSubject.name}`}
            />
            <SummaryCard
              label="Classes attended"
              value={personalAttendanceStats.attended}
              icon={<Check size={18} />}
              detail="Marked present"
            />
            <SummaryCard
              label="Classes missed"
              value={personalAttendanceStats.missed}
              icon={<X size={18} />}
              detail="Marked absent"
            />
            <SummaryCard
              label="Attendance percentage"
              value={`${personalAttendanceStats.percentage}%`}
              icon={<TrendingUp size={18} />}
              detail={personalAttendanceStats.totalClasses ? 'Your attendance' : 'No classes recorded'}
            />
          </div>

          <section className="card" style={{ marginBottom: 16 }}>
            <div className="card-header">
              <div>
                <h3 className="card-title">Day-wise Attendance</h3>
                <p className="card-subtitle">
                  Your attendance history for {selectedSubject.name} ({selectedSubject.code}).
                </p>
              </div>
            </div>
            {!distinctDays.length ? (
              <div className="empty-state">
                <CalendarCheck />
                <p>No attendance records have been recorded for this subject yet.</p>
              </div>
            ) : (
              <div className="table-container">
                <table>
                  <thead>
                    <tr>
                      <th>Class</th>
                      <th>Date</th>
                      <th>Attendance</th>
                    </tr>
                  </thead>
                  <tbody>
                    {distinctDays.map((day) => {
                      const status = personalRecordsByDate.get(day.date)?.status;
                      const isPresent = status === 'PRESENT';
                      const isAbsent = status === 'ABSENT';
                      const label = isPresent ? 'Present' : isAbsent ? 'Absent' : 'Not recorded';
                      return (
                        <tr key={day.date}>
                          <td style={{ fontWeight: 700 }}>Day {day.dayNumber}</td>
                          <td>{day.fullDate}</td>
                          <td>
                            <span className="badge" style={{
                              background: isPresent
                                ? 'var(--success-light)'
                                : isAbsent ? 'var(--danger-light)' : 'var(--bg-secondary)',
                              color: isPresent
                                ? 'var(--success)'
                                : isAbsent ? 'var(--danger)' : 'var(--text-muted)',
                            }}>
                              {label}
                            </span>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </div>
      ) : (
        <div className="attendance-matrix-wrapper">
          <table className="attendance-matrix-table">
            <thead>
              <tr>
                {/* 1. Sticky Student Identifiers */}
                {isAdmin ? (
                  <>
                    <SortableTh tableId="attendance-matrix" columnKey="rollNumber" label="Institute ID" sorts={attendanceSorts} onSort={toggleSort} className="sticky-col-roll" />
                    <SortableTh tableId="attendance-matrix" columnKey="name" label="Student Name" sorts={attendanceSorts} onSort={toggleSort} className="sticky-col-name" />
                    <SortableTh tableId="attendance-matrix" columnKey="overallAttendance" label="Overall %" sorts={attendanceSorts} onSort={toggleSort} className="sticky-col-pct" style={{ textAlign: 'center' }} />
                  </>
                ) : (
                  <>
                    <th className="sticky-col-roll">Institute ID</th>
                    <th className="sticky-col-name">Student Name</th>
                    <th className="sticky-col-pct" style={{ textAlign: 'center' }}>Overall %</th>
                  </>
                )}

                {/* 2, 6. Day Columns (narrow, day number + short date, hover tooltip, highlighted when class taken) */}
                {distinctDays.map((d) => (
                  isAdmin ? (
                    <SortableTh
                      key={d.date}
                      tableId="attendance-matrix"
                      columnKey={`day-${d.date}`}
                      ariaLabel={`Day ${d.dayNumber}, ${d.fullDate}`}
                      label={<><span className="day-number-text">Day {d.dayNumber}</span><span className="day-date-text">{d.shortDate}</span></>}
                      sorts={attendanceSorts}
                      onSort={toggleSort}
                      className="day-col-header conducted-day"
                      headerContent={(
                        <div className="day-tooltip">
                      <div style={{ fontWeight: 800, color: 'var(--accent)', marginBottom: 4, display: 'flex', alignItems: 'center', gap: 4 }}>
                        <Calendar size={13} /> Day {d.dayNumber} ({d.date})
                      </div>
                      <div style={{ color: 'var(--text-muted)', fontSize: '0.7rem', marginBottom: 6 }}>
                        {d.fullDate}
                      </div>
                      <div style={{ color: 'var(--success)', fontWeight: 700 }}>
                        ✓ Present: {d.presentCount}
                      </div>
                      <div style={{ color: 'var(--danger)', fontWeight: 700 }}>
                        ✗ Absent: {d.absentCount}
                      </div>
                      <div style={{ marginTop: 4, fontWeight: 700, color: '#fff' }}>
                        Attendance: {d.dayPct}%
                      </div>
                      {d.avgMarks && (
                        <div style={{ color: '#f59e0b', fontSize: '0.72rem' }}>
                          Avg Performance: {d.avgMarks}/10
                        </div>
                      )}
                      {canEditAttendance && (
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleDeleteDay(d.date, d.dayNumber);
                          }}
                          style={{
                            marginTop: 8,
                            background: 'transparent',
                            border: '1px solid var(--danger)',
                            color: 'var(--danger)',
                            borderRadius: 4,
                            padding: '3px 6px',
                            fontSize: '0.7rem',
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: 4,
                            width: '100%',
                            justifyContent: 'center',
                          }}
                        >
                          <Trash2 size={11} /> Delete Day
                        </button>
                      )}
                        </div>
                      )}
                    />
                  ) : (
                    <th key={d.date} className="day-col-header conducted-day">
                      <div className="day-number-text">Day {d.dayNumber}</div>
                      <div className="day-date-text">{d.shortDate}</div>
                      <div className="day-tooltip">
                        <div style={{ fontWeight: 800, color: 'var(--accent)', marginBottom: 4, display: 'flex', alignItems: 'center', gap: 4 }}>
                          <Calendar size={13} /> Day {d.dayNumber} ({d.date})
                        </div>
                        <div style={{ color: 'var(--text-muted)', fontSize: '0.7rem', marginBottom: 6 }}>{d.fullDate}</div>
                        <div style={{ color: 'var(--success)', fontWeight: 700 }}>✓ Present: {d.presentCount}</div>
                        <div style={{ color: 'var(--danger)', fontWeight: 700 }}>✗ Absent: {d.absentCount}</div>
                        <div style={{ marginTop: 4, fontWeight: 700, color: '#fff' }}>Attendance: {d.dayPct}%</div>
                        {d.avgMarks && <div style={{ color: '#f59e0b', fontSize: '0.72rem' }}>Avg Performance: {d.avgMarks}/10</div>}
                        {canEditAttendance && (
                          <button
                            type="button"
                            onClick={(e) => { e.stopPropagation(); handleDeleteDay(d.date, d.dayNumber); }}
                            style={{ marginTop: 8, background: 'transparent', border: '1px solid var(--danger)', color: 'var(--danger)', borderRadius: 4, padding: '3px 6px', fontSize: '0.7rem', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4, width: '100%', justifyContent: 'center' }}
                          >
                            <Trash2 size={11} /> Delete Day
                          </button>
                        )}
                      </div>
                    </th>
                  )
                ))}

                {/* 3. Add Next Day column trigger */}
                {canEditAttendance && (
                  <th
                    style={{ width: 80, minWidth: 80, textAlign: 'center', verticalAlign: 'middle', cursor: 'pointer', background: 'var(--bg-secondary)' }}
                    onClick={() => setAddDayModalOpen(true)}
                    title="Add Next Day / Session"
                  >
                    <button
                      type="button"
                      className="btn btn-ghost btn-sm"
                      style={{ fontSize: '0.75rem', gap: 4, padding: '6px 8px', color: 'var(--accent)', fontWeight: 700 }}
                    >
                      <Plus size={14} /> Day {distinctDays.length + 1}
                    </button>
                  </th>
                )}
              </tr>
            </thead>
            <tbody>
              {visibleStudents.length === 0 ? (
                <tr>
                  <td colSpan={3 + distinctDays.length + 1} style={{ textAlign: 'center', padding: 30, color: 'var(--text-muted)' }}>
                    No verified students found for this subject.
                  </td>
                </tr>
              ) : (
                sortedVisibleStudents.map((stu) => {
                  const sStats = studentStatsMap.get(stu.id) || { attended: 0, totalDays: 0, pct: 0, isLow: false };
                  return (
                    <tr key={stu.id}>
                      {/* Sticky Institute ID */}
                      <td className="sticky-col-roll" style={{ fontFamily: 'monospace', fontWeight: 700, color: 'var(--accent)' }}>
                        {stu.rollNumber}
                      </td>

                      {/* Sticky Student Name */}
                      <td className="sticky-col-name" style={{ fontWeight: 600 }}>
                        <div style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={stu.name}>
                          {stu.name}
                        </div>
                      </td>

                      {/* 7. Attendance Percentage for this subject */}
                      <td className="sticky-col-pct" style={{ textAlign: 'center' }}>
                        <span style={{
                          display: 'inline-block',
                          padding: '2px 8px',
                          borderRadius: 12,
                          fontSize: '0.75rem',
                          fontWeight: 800,
                          background: sStats.isLow ? 'var(--danger-light)' : 'var(--success-light)',
                          color: sStats.isLow ? 'var(--danger)' : 'var(--success)',
                        }}>
                          {sStats.pct}%
                        </span>
                        <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)', marginTop: 2 }}>
                          {sStats.attended}/{sStats.totalDays}
                        </div>
                      </td>

                      {/* 4, 5. Cells corresponding to each Day column (status + optional performance marks) */}
                      {distinctDays.map((d) => {
                        const cell = cellMap.get(`${stu.id}_${d.date}`);
                        const status = cell?.status || 'UNMARKED';
                        const marksVal = cell?.performanceMarks !== null && cell?.performanceMarks !== undefined
                          ? cell.performanceMarks
                          : '';

                        let statusCls = 'unmarked';
                        let label = '—';
                        if (status === 'PRESENT') { statusCls = 'present'; label = 'P'; }
                        else if (status === 'ABSENT') { statusCls = 'absent'; label = 'A'; }

                        return (
                          <td key={d.date} className="day-col-cell">
                            <button
                              type="button"
                              className={`status-toggle-btn ${statusCls}`}
                              disabled={isStudent}
                              title={isStudent ? `${status}` : `Click to mark ${status === 'PRESENT' ? 'Absent' : 'Present'}`}
                              onClick={() => handleToggleStatus(stu.id, d.date)}
                            >
                              {label}
                            </button>

                            {/* Optional class performance mark */}
                            {canEditAttendance ? (
                              <div>
                                <input
                                  type="number"
                                  min="0"
                                  max="10"
                                  className="perf-marks-input"
                                  defaultValue={marksVal}
                                  placeholder="★"
                                  title="Class performance mark (0-10)"
                                  onBlur={(e) => handlePerformanceMarksChange(stu.id, d.date, e.target.value)}
                                  onKeyDown={(e) => {
                                    if (e.key === 'Enter') {
                                      e.currentTarget.blur();
                                    }
                                  }}
                                />
                              </div>
                            ) : (
                              marksVal !== '' && (
                                <div style={{ fontSize: '0.68rem', fontWeight: 700, color: '#f59e0b', marginTop: 2 }}>
                                  ★ {marksVal}
                                </div>
                              )
                            )}
                          </td>
                        );
                      })}

                      {canEditAttendance && <td style={{ background: 'var(--bg-secondary)' }} />}
                    </tr>
                  );
                })
              )}
            </tbody>

            {/* 9. Last row containing total present/absent and column percentage */}
            <tfoot>
              <tr className="matrix-footer-row">
                <td className="sticky-col-roll" style={{ background: 'var(--bg-hover)' }}>Summary</td>
                <td className="sticky-col-name" style={{ background: 'var(--bg-hover)' }}>Total Class Stats</td>
                <td className="sticky-col-pct" style={{ textAlign: 'center', background: 'var(--bg-hover)' }}>
                  <div style={{ fontSize: '0.8rem', fontWeight: 800, color: overallClassAverage >= 75 ? 'var(--success)' : 'var(--danger)' }}>
                    {overallClassAverage}%
                  </div>
                  <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>Class Avg</div>
                </td>

                {distinctDays.map((d) => (
                  <td key={d.date} className="day-col-cell" style={{ background: 'var(--bg-hover)' }}>
                    <div style={{ fontSize: '0.72rem', color: 'var(--success)', fontWeight: 800 }}>
                      P:{d.presentCount}
                    </div>
                    <div style={{ fontSize: '0.72rem', color: 'var(--danger)', fontWeight: 800 }}>
                      A:{d.absentCount}
                    </div>
                    <div style={{
                      fontSize: '0.7rem',
                      fontWeight: 800,
                      marginTop: 2,
                      padding: '1px 4px',
                      borderRadius: 4,
                      background: d.dayPct >= 75 ? 'var(--success-light)' : 'var(--danger-light)',
                      color: d.dayPct >= 75 ? 'var(--success)' : 'var(--danger)',
                    }}>
                      {d.dayPct}%
                    </div>
                  </td>
                ))}

                {canEditAttendance && <td style={{ background: 'var(--bg-hover)' }} />}
              </tr>
            </tfoot>
          </table>
        </div>
      )}

      {/* 3. Add Day Modal */}
      {addDayModalOpen && canEditAttendance && (
        <div className="modal-overlay" onClick={() => setAddDayModalOpen(false)}>
          <div className="modal" style={{ maxWidth: 420 }} onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">
                Add Session (Day {distinctDays.length + 1})
              </h3>
              <button className="btn btn-ghost btn-icon" onClick={() => setAddDayModalOpen(false)}>
                <X size={18} />
              </button>
            </div>
            <form onSubmit={handleAddDay}>
              <div className="modal-body">
                <div style={{ marginBottom: 14 }}>
                  <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Subject: </span>
                  <strong style={{ color: 'var(--accent)' }}>{selectedSubject?.name} ({selectedSubject?.code})</strong>
                </div>

                <div className="form-group">
                  <label className="form-label">Date of Class *</label>
                  <input
                    className="form-input"
                    type="date"
                    value={newDayDate}
                    onChange={(e) => setNewDayDate(e.target.value)}
                    required
                  />
                </div>

                <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 12 }}>
                  <input
                    type="checkbox"
                    id="defPresent"
                    checked={defaultPresent}
                    onChange={(e) => setDefaultPresent(e.target.checked)}
                    style={{ width: 18, height: 18, cursor: 'pointer' }}
                  />
                  <label htmlFor="defPresent" style={{ fontSize: '0.85rem', cursor: 'pointer', fontWeight: 600 }}>
                    Mark all students as Present initially
                  </label>
                </div>
                <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: 4 }}>
                  You can quickly toggle any individual student between Present and Absent directly on the spreadsheet grid cells.
                </p>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setAddDayModalOpen(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={submittingDay}>
                  {submittingDay ? (
                    <span className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} />
                  ) : (
                    <>
                      <Check size={16} /> Create Day {distinctDays.length + 1}
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </Layout>
  );
}
