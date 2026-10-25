import { useEffect, useState, useCallback, useMemo, useRef } from 'react';
import Layout from '../components/Layout';
import { useAuth } from '../context/AuthContext';
import {
  getAttendanceBySubject,
  getAttendanceByStudent,
  createAttendance,
  recordDayBatch,
  deleteDayAttendance,
} from '../api/attendance';
import { getAllSubjects, getSubjectsByFaculty, getSubjectsByStudent } from '../api/subjects';
import { getAllDepartments } from '../api/departments';
import { getAllUsers } from '../api/users';
import {
  CalendarCheck, Plus, Search, TrendingUp, AlertTriangle,
  BookOpen, Trash2, X, Check, Calendar, ChevronRight,
} from 'lucide-react';
import toast from 'react-hot-toast';

export default function AttendancePage() {
  const { user, isFaculty, isStudent, isAdminOrHod } = useAuth();

  const [subjects, setSubjects] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [selectedDepartmentId, setSelectedDepartmentId] = useState('');
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

  // Load Subjects and Verified Students once
  useEffect(() => {
    const init = async () => {
      setLoading(true);
      try {
        const [subRes, userRes, departmentRes] = await Promise.all([
          isFaculty
            ? getSubjectsByFaculty(user.id)
            : (isStudent ? getSubjectsByStudent(user.id) : getAllSubjects()),
          getAllUsers(),
          user?.role === 'ADMIN' ? getAllDepartments() : Promise.resolve({ data: [] }),
        ]);

        const loadedSubjects = subRes.data || [];
        setSubjects(loadedSubjects);
        setDepartments(departmentRes.data || []);

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
  }, [user, isFaculty, isStudent, isAdminOrHod]);

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
    if (isAdminOrHod && selectedSubject) {
      list = list.filter((student) =>
        String(student.department?.id) === String(selectedSubject.department?.id)
        && Number(student.semester) === Number(selectedSubject.semester)
      );
    }
    return list;
  }, [students, isStudent, isAdminOrHod, selectedSubject, user]);

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
    if (isStudent) return; // Students cannot toggle

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
    if (isStudent) return;
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
      <div className="page-header" style={{ marginBottom: 16 }}>
        <div>
          <h2 className="page-title">Attendance Register</h2>
          <p className="page-subtitle">
            {isFaculty
              ? 'Multi-day spreadsheet attendance & performance grading'
              : (isStudent ? 'Your subject-wise attendance history' : 'Institute attendance matrix')}
          </p>
        </div>
        {(isFaculty || isAdminOrHod) && selectedSubject && (
          <button className="btn btn-primary" onClick={() => setAddDayModalOpen(true)}>
            <Plus size={18} /> Add Day / Class
          </button>
        )}
      </div>

      {/* ──────── 8. Subject Selection at Top ──────── */}
      <div className="card" style={{ padding: '16px 20px', marginBottom: 20 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 14 }}>
          <div className="attendance-filter-row">
            {user?.role === 'ADMIN' && (
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
            ) : (
              <>
                <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: 6 }}>
                  <BookOpen size={17} color="var(--accent)" /> Select Subject:
                </span>
                <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                  {subjects.map((s) => {
                    const isSelected = Number(selectedSubjectId) === s.id;
                    return (
                      <button
                        key={s.id}
                        onClick={() => setSelectedSubjectId(s.id)}
                        style={{
                          padding: '7px 14px',
                          borderRadius: 20,
                          fontSize: '0.82rem',
                          fontWeight: 700,
                          cursor: 'pointer',
                          transition: 'all 0.2s ease',
                          background: isSelected ? 'var(--accent)' : 'var(--bg-secondary)',
                          color: isSelected ? '#fff' : 'var(--text-secondary)',
                          border: `1px solid ${isSelected ? 'var(--accent)' : 'var(--border)'}`,
                          boxShadow: isSelected ? '0 0 14px var(--accent-glow)' : 'none',
                        }}
                      >
                        {s.name} ({s.code}) · {s.type === 'OPTIONAL' ? 'Optional' : 'Compulsory'}
                      </button>
                    );
                  })}
                </div>
              </>
            )}
          </div>

          {selectedSubject && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 16, fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              <span>Faculty: <strong>{selectedSubject.faculty?.name || '—'}</strong></span>
              <span>Classes Taken: <strong style={{ color: 'var(--accent)' }}>{distinctDays.length}</strong></span>
              <span>Class Avg: <strong style={{ color: overallClassAverage >= 75 ? 'var(--success)' : 'var(--danger)' }}>{overallClassAverage}%</strong></span>
            </div>
          )}
        </div>
      </div>

      {/* Student Low Attendance Alert */}
      {isStudent && user && (
        (() => {
          const s = studentStatsMap.get(user.id);
          if (s && s.totalDays > 0 && s.pct < 75) {
            return (
              <div className="alert" style={{
                background: 'rgba(239, 68, 68, 0.1)', color: 'var(--danger)',
                border: '1px solid var(--danger)', marginBottom: 20,
                display: 'flex', alignItems: 'center', gap: 12,
              }}>
                <AlertTriangle size={20} />
                <div>
                  <strong>Low Attendance Alert:</strong> Your attendance in {selectedSubject?.name} is {s.pct}% ({s.attended}/{s.totalDays} classes).
                  The minimum requirement is 75%.
                </div>
              </div>
            );
          }
          return null;
        })()
      )}

      {/* Toolbar: Search and Quick Legend */}
      <div className="toolbar" style={{ marginBottom: 14 }}>
        <div className="toolbar-left">
          <div className="search-bar" style={{ maxWidth: 320 }}>
            <Search size={16} />
            <input
              placeholder="Search student by name or roll number..."
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
      </div>

      {/* ──────── 1-7, 9. The Attendance Matrix Grid ──────── */}
      {loading || gridLoading ? (
        <div className="card" style={{ padding: 40, textAlign: 'center' }}>
          <div className="loader"><div className="spinner" /></div>
          <p style={{ marginTop: 12, color: 'var(--text-muted)', fontSize: '0.85rem' }}>Loading attendance grid...</p>
        </div>
      ) : !selectedSubject ? (
        <div className="empty-state">
          <BookOpen />
          <h3>{isAdminOrHod && ((user?.role === 'ADMIN' && !selectedDepartmentId) || !selectedSemester) ? 'Select attendance filters' : 'No subjects available'}</h3>
          <p>
            {isAdminOrHod && ((user?.role === 'ADMIN' && !selectedDepartmentId) || !selectedSemester)
              ? 'Choose the required department and semester, then select a subject to view attendance.'
              : (isAdminOrHod ? 'There are no subjects for the selected department and semester.' : 'You have not been assigned to any subjects yet.')}
          </p>
        </div>
      ) : (
        <div className="attendance-matrix-wrapper">
          <table className="attendance-matrix-table">
            <thead>
              <tr>
                {/* 1. Sticky Student Identifiers */}
                <th className="sticky-col-roll">Roll No</th>
                <th className="sticky-col-name">Student Name</th>
                <th className="sticky-col-pct" style={{ textAlign: 'center' }}>Overall %</th>

                {/* 2, 6. Day Columns (narrow, day number + short date, hover tooltip, highlighted when class taken) */}
                {distinctDays.map((d) => (
                  <th key={d.date} className="day-col-header conducted-day">
                    <div className="day-number-text">Day {d.dayNumber}</div>
                    <div className="day-date-text">{d.shortDate}</div>

                    {/* Tooltip on hover (Point 2) */}
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
                      {(isFaculty || isAdminOrHod) && (
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
                  </th>
                ))}

                {/* 3. Add Next Day column trigger */}
                {(isFaculty || isAdminOrHod) && (
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
                visibleStudents.map((stu) => {
                  const sStats = studentStatsMap.get(stu.id) || { attended: 0, totalDays: 0, pct: 0, isLow: false };
                  return (
                    <tr key={stu.id}>
                      {/* Sticky Roll No */}
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
                            {(isFaculty || isAdminOrHod) ? (
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

                      {(isFaculty || isAdminOrHod) && <td style={{ background: 'var(--bg-secondary)' }} />}
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

                {(isFaculty || isAdminOrHod) && <td style={{ background: 'var(--bg-hover)' }} />}
              </tr>
            </tfoot>
          </table>
        </div>
      )}

      {/* 3. Add Day Modal */}
      {addDayModalOpen && (
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
