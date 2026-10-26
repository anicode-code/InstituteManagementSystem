import { useCallback, useEffect, useMemo, useState } from 'react';
import Layout from '../components/Layout';
import {
  assignFacultyToSubject,
  decideSubjectRequest,
  getEligibleSubjectFaculty,
  getHodDepartmentsForSubjects,
  getHodManagedSubjects,
  getHodSubjectRequests,
  removeFacultyFromSubject,
} from '../api/subjectRequests';
import { Check, RefreshCw, UserMinus, UserPlus, X } from 'lucide-react';
import toast from 'react-hot-toast';

const statusStyles = {
  PENDING: { background: 'var(--warning-light)', color: 'var(--warning)' },
  APPROVED: { background: 'var(--success-light)', color: 'var(--success)' },
  REJECTED: { background: 'var(--danger-light)', color: 'var(--danger)' },
};

export default function HodSubjectAssignmentsPage() {
  const [departments, setDepartments] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [requests, setRequests] = useState([]);
  const [eligibleFaculty, setEligibleFaculty] = useState([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [selectedFacultyId, setSelectedFacultyId] = useState('');
  const [departmentFilter, setDepartmentFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('PENDING');
  const [loading, setLoading] = useState(true);
  const [workingKey, setWorkingKey] = useState('');

  const loadBase = useCallback(async () => {
    setLoading(true);
    try {
      const [departmentResponse, subjectResponse] = await Promise.all([
        getHodDepartmentsForSubjects(),
        getHodManagedSubjects(),
      ]);
      setDepartments(departmentResponse.data || []);
      setSubjects(subjectResponse.data || []);
      setSelectedSubjectId((current) => current || String(subjectResponse.data?.[0]?.id || ''));
    } catch (error) {
      toast.error(error?.response?.data?.message || 'Failed to load HOD subject management');
    } finally {
      setLoading(false);
    }
  }, []);

  const loadRequests = useCallback(async () => {
    try {
      const response = await getHodSubjectRequests({
        ...(departmentFilter ? { departmentId: departmentFilter } : {}),
        ...(statusFilter ? { status: statusFilter } : {}),
      });
      setRequests(response.data || []);
    } catch (error) {
      toast.error(error?.response?.data?.message || 'Failed to load faculty requests');
    }
  }, [departmentFilter, statusFilter]);

  const loadEligibleFaculty = useCallback(async () => {
    if (!selectedSubjectId) {
      setEligibleFaculty([]);
      return;
    }
    try {
      const response = await getEligibleSubjectFaculty(selectedSubjectId);
      setEligibleFaculty(response.data || []);
    } catch (error) {
      toast.error(error?.response?.data?.message || 'Failed to load eligible faculty');
    }
  }, [selectedSubjectId]);

  useEffect(() => { loadBase(); }, [loadBase]);
  useEffect(() => { loadRequests(); }, [loadRequests]);
  useEffect(() => { loadEligibleFaculty(); }, [loadEligibleFaculty]);

  const selectedSubject = useMemo(
    () => subjects.find((subject) => String(subject.id) === String(selectedSubjectId)),
    [subjects, selectedSubjectId],
  );

  const refresh = async () => {
    await Promise.all([loadBase(), loadRequests(), loadEligibleFaculty()]);
  };

  const decide = async (requestId, decision) => {
    const key = `${requestId}-${decision}`;
    setWorkingKey(key);
    try {
      await decideSubjectRequest(requestId, decision);
      toast.success(`Request ${decision.toLowerCase()}`);
      await Promise.all([loadRequests(), loadBase()]);
    } catch (error) {
      toast.error(error?.response?.data?.message || `Could not ${decision.toLowerCase()} request`);
    } finally {
      setWorkingKey('');
    }
  };

  const assign = async () => {
    if (!selectedSubjectId || !selectedFacultyId) return;
    setWorkingKey('assign');
    try {
      await assignFacultyToSubject(selectedSubjectId, selectedFacultyId);
      toast.success('Faculty assigned to subject');
      setSelectedFacultyId('');
      await Promise.all([loadBase(), loadEligibleFaculty()]);
    } catch (error) {
      toast.error(error?.response?.data?.message || 'Could not assign faculty');
    } finally {
      setWorkingKey('');
    }
  };

  const remove = async (faculty) => {
    if (!selectedSubject || !window.confirm(`Remove ${faculty.name} from ${selectedSubject.name}?`)) return;
    setWorkingKey(`remove-${faculty.id}`);
    try {
      await removeFacultyFromSubject(selectedSubject.id, faculty.id);
      toast.success('Faculty removed from subject');
      await Promise.all([loadBase(), loadEligibleFaculty()]);
    } catch (error) {
      toast.error(error?.response?.data?.message || 'Could not remove faculty');
    } finally {
      setWorkingKey('');
    }
  };

  return (
    <Layout pageTitle="Subject Assignments">
      <div className="page-header">
        <div>
          <h2 className="page-title">Faculty Requests & Assignments</h2>
          <p className="page-subtitle">Review requests and manage teaching teams in your appointed departments.</p>
        </div>
        <button className="btn btn-secondary" onClick={refresh} disabled={loading}><RefreshCw size={16} /> Refresh</button>
      </div>

      <section className="card" style={{ marginBottom: 24 }}>
        <div className="card-header">
          <div><h3 className="card-title">Faculty Subject Requests</h3><p className="card-subtitle">Only requests from departments assigned to your HOD account are shown.</p></div>
        </div>
        <div className="toolbar" style={{ marginBottom: 16 }}>
          <div className="toolbar-left">
            <select className="form-select" aria-label="Filter requests by department" value={departmentFilter} onChange={(event) => setDepartmentFilter(event.target.value)}>
              <option value="">All My Departments</option>
              {departments.map((department) => <option key={department.id} value={department.id}>{department.name}</option>)}
            </select>
            <select className="form-select" aria-label="Filter requests by status" value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
              <option value="">All Statuses</option>
              <option value="PENDING">Pending</option>
              <option value="APPROVED">Approved</option>
              <option value="REJECTED">Rejected</option>
            </select>
          </div>
        </div>
        {loading ? <div className="loader"><div className="spinner" /></div> : requests.length === 0 ? (
          <div className="empty-state"><p>No requests match these filters.</p></div>
        ) : (
          <div className="table-container">
            <table>
              <thead><tr><th>Faculty</th><th>Subject</th><th>Department</th><th>Semester</th><th>Requested</th><th>Status</th><th>Decision</th></tr></thead>
              <tbody>
                {requests.map((request) => (
                  <tr key={request.id}>
                    <td><strong>{request.facultyName}</strong><div className="text-muted">{request.facultyRollNumber}</div></td>
                    <td><strong>{request.subjectName}</strong><div className="text-muted">{request.subjectCode}</div></td>
                    <td>{request.departmentName}</td>
                    <td>{request.semester ? `Semester ${request.semester}` : '—'}</td>
                    <td>{request.createdAt ? new Date(request.createdAt).toLocaleDateString() : '—'}</td>
                    <td><span className="badge" style={statusStyles[request.status]}>{request.status}</span></td>
                    <td>
                      {request.status === 'PENDING' ? (
                        <div style={{ display: 'flex', gap: 8 }}>
                          <button className="btn btn-primary btn-sm" disabled={workingKey !== ''} onClick={() => decide(request.id, 'APPROVED')}>
                            <Check size={15} /> Approve
                          </button>
                          <button className="btn btn-danger btn-sm" disabled={workingKey !== ''} onClick={() => decide(request.id, 'REJECTED')}>
                            <X size={15} /> Reject
                          </button>
                        </div>
                      ) : <span className="text-muted">{request.reviewedByName ? `By ${request.reviewedByName}` : '—'}</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="card">
        <div className="card-header">
          <div><h3 className="card-title">Manage Teaching Team</h3><p className="card-subtitle">Directly assign or remove eligible faculty members.</p></div>
        </div>
        {loading ? <div className="loader"><div className="spinner" /></div> : subjects.length === 0 ? (
          <div className="empty-state"><p>No subjects are available in your appointed departments.</p></div>
        ) : (
          <>
            <div className="form-group" style={{ maxWidth: 600 }}>
              <label className="form-label" htmlFor="managed-subject">Subject</label>
              <select id="managed-subject" className="form-select" value={selectedSubjectId} onChange={(event) => setSelectedSubjectId(event.target.value)}>
                {subjects.map((subject) => (
                  <option key={subject.id} value={subject.id}>{subject.departmentName} · Sem {subject.semester ?? '—'} · {subject.name} ({subject.code})</option>
                ))}
              </select>
            </div>
            {selectedSubject && (
              <>
                <div style={{ margin: '18px 0' }}>
                  <h4 style={{ marginBottom: 12 }}>Currently Assigned</h4>
                  {selectedSubject.faculty?.length ? (
                    <div style={{ display: 'grid', gap: 10 }}>
                      {selectedSubject.faculty.map((faculty) => (
                        <div key={faculty.id} className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: 12 }}>
                          <div><strong>{faculty.name}</strong><div className="text-muted">{faculty.rollNumber} · {faculty.role}</div></div>
                          <button className="btn btn-danger btn-sm" disabled={workingKey !== ''} onClick={() => remove(faculty)}><UserMinus size={15} /> Remove</button>
                        </div>
                      ))}
                    </div>
                  ) : <p className="text-muted">No faculty are currently assigned.</p>}
                </div>
                <div className="grid-2" style={{ alignItems: 'end', maxWidth: 700 }}>
                  <div className="form-group">
                    <label className="form-label" htmlFor="eligible-faculty">Eligible Faculty</label>
                    <select id="eligible-faculty" className="form-select" value={selectedFacultyId} onChange={(event) => setSelectedFacultyId(event.target.value)}>
                      <option value="">Select a faculty member</option>
                      {eligibleFaculty.filter((faculty) => !selectedSubject.faculty?.some((assigned) => assigned.id === faculty.id)).map((faculty) => (
                        <option key={faculty.id} value={faculty.id}>{faculty.name} ({faculty.rollNumber}){faculty.role === 'HOD' ? ' · HOD' : ''}</option>
                      ))}
                    </select>
                  </div>
                  <button className="btn btn-primary" disabled={!selectedFacultyId || workingKey !== ''} onClick={assign}><UserPlus size={16} /> Assign Faculty</button>
                </div>
              </>
            )}
          </>
        )}
      </section>
    </Layout>
  );
}
