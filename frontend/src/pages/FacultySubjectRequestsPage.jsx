import { useCallback, useEffect, useMemo, useState } from 'react';
import Layout from '../components/Layout';
import { useAuth } from '../context/AuthContext';
import {
  createSubjectRequest,
  getEligibleRequestSubjects,
  getMySubjectRequests,
} from '../api/subjectRequests';
import { BookOpen, Send } from 'lucide-react';
import toast from 'react-hot-toast';

const statusStyles = {
  PENDING: { background: 'var(--warning-light)', color: 'var(--warning)' },
  APPROVED: { background: 'var(--success-light)', color: 'var(--success)' },
  REJECTED: { background: 'var(--danger-light)', color: 'var(--danger)' },
};

export default function FacultySubjectRequestsPage() {
  const { user } = useAuth();
  const [subjects, setSubjects] = useState([]);
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submittingId, setSubmittingId] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [subjectResponse, requestResponse] = await Promise.all([
        getEligibleRequestSubjects(),
        getMySubjectRequests(),
      ]);
      setSubjects(subjectResponse.data || []);
      setRequests(requestResponse.data || []);
    } catch (error) {
      toast.error(error?.response?.data?.message || 'Failed to load subject requests');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const requestedSubjectIds = useMemo(
    () => new Set(requests.filter((item) => item.status === 'PENDING').map((item) => item.subjectId)),
    [requests],
  );

  const requestSubject = async (subjectId) => {
    setSubmittingId(subjectId);
    try {
      await createSubjectRequest(subjectId);
      toast.success('Subject request submitted');
      await load();
    } catch (error) {
      toast.error(error?.response?.data?.message || 'Could not submit subject request');
    } finally {
      setSubmittingId(null);
    }
  };

  return (
    <Layout pageTitle="Subject Requests">
      <div className="page-header">
        <div>
          <h2 className="page-title">Request to Teach</h2>
          <p className="page-subtitle">Request subjects in your registered departments. HOD approval is required before assignment.</p>
        </div>
      </div>

      <section className="card" style={{ marginBottom: 24 }}>
        <div className="card-header">
          <div>
            <h3 className="card-title">Eligible Subjects</h3>
            <p className="card-subtitle">Subjects are limited to your registered department associations.</p>
          </div>
        </div>
        {loading ? <div className="loader"><div className="spinner" /></div> : subjects.length === 0 ? (
          <div className="empty-state">
            <BookOpen />
            <h3>No eligible subjects</h3>
            <p>No subjects were found in your registered departments.</p>
          </div>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr><th>Subject</th><th>Department</th><th>Semester</th><th>Current Faculty</th><th>Action</th></tr>
              </thead>
              <tbody>
                {subjects.map((subject) => {
                  const alreadyAssigned = subject.faculty?.some((member) => member.id === user?.id);
                  const pending = requestedSubjectIds.has(subject.id);
                  return (
                    <tr key={subject.id}>
                      <td><strong>{subject.name}</strong><div className="text-muted">{subject.code}</div></td>
                      <td>{subject.departmentName}</td>
                      <td>{subject.semester ? `Semester ${subject.semester}` : '—'}</td>
                      <td>{subject.faculty?.length ? subject.faculty.map((member) => member.name).join(', ') : 'Unassigned'}</td>
                      <td>
                        {alreadyAssigned ? (
                          <span className="badge" style={statusStyles.APPROVED}>Assigned</span>
                        ) : (
                          <button
                            className="btn btn-primary btn-sm"
                            disabled={pending || submittingId === subject.id}
                            onClick={() => requestSubject(subject.id)}
                          >
                            <Send size={15} /> {pending ? 'Pending' : submittingId === subject.id ? 'Submitting…' : 'Request'}
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="card">
        <div className="card-header">
          <div>
            <h3 className="card-title">My Request History</h3>
            <p className="card-subtitle">Request outcomes are retained for tracking.</p>
          </div>
        </div>
        {loading ? <div className="loader"><div className="spinner" /></div> : requests.length === 0 ? (
          <div className="empty-state"><p>You have not submitted any subject requests.</p></div>
        ) : (
          <div className="table-container">
            <table>
              <thead><tr><th>Subject</th><th>Department</th><th>Semester</th><th>Requested</th><th>Status</th><th>Reviewed By</th></tr></thead>
              <tbody>
                {requests.map((request) => (
                  <tr key={request.id}>
                    <td><strong>{request.subjectName}</strong><div className="text-muted">{request.subjectCode}</div></td>
                    <td>{request.departmentName}</td>
                    <td>{request.semester ? `Semester ${request.semester}` : '—'}</td>
                    <td>{request.createdAt ? new Date(request.createdAt).toLocaleDateString() : '—'}</td>
                    <td><span className="badge" style={statusStyles[request.status]}>{request.status}</span></td>
                    <td>{request.reviewedByName || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </Layout>
  );
}
