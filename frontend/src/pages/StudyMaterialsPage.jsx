import { useCallback, useEffect, useMemo, useState } from 'react';
import { ArrowLeft, ChevronRight, Download, FileStack, Trash2, Upload } from 'lucide-react';
import toast from 'react-hot-toast';
import Layout from '../components/Layout';
import { useAuth } from '../context/AuthContext';
import { getSubjectsByFaculty, getSubjectsByStudent } from '../api/subjects';
import {
  downloadStudyMaterial,
  deleteStudyMaterial,
  getStudyMaterials,
  uploadStudyMaterials,
} from '../api/studyMaterials';

function formatFileSize(size) {
  if (!size) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const unitIndex = Math.min(Math.floor(Math.log(size) / Math.log(1024)), units.length - 1);
  return `${(size / (1024 ** unitIndex)).toFixed(unitIndex ? 1 : 0)} ${units[unitIndex]}`;
}

function getFileExtension(fileName) {
  const name = String(fileName || '').toLowerCase();
  const dot = name.lastIndexOf('.');
  return dot > 0 && dot < name.length - 1 ? name.slice(dot) : '';
}

export default function StudyMaterialsPage() {
  const { user, isStudent } = useAuth();
  const canUpload = user?.role === 'HOD' || user?.role === 'FACULTY';
  const [subjects, setSubjects] = useState([]);
  const [materials, setMaterials] = useState([]);
  const [subjectId, setSubjectId] = useState('');
  const [fileFormat, setFileFormat] = useState('ALL');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [files, setFiles] = useState([]);
  const [fileInputKey, setFileInputKey] = useState(0);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const [loadError, setLoadError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError('');
    try {
      const [subjectResponse, materialResponse] = await Promise.all([
        isStudent ? getSubjectsByStudent(user.id) : getSubjectsByFaculty(user.id),
        getStudyMaterials(),
      ]);
      if (!Array.isArray(subjectResponse.data) || !Array.isArray(materialResponse.data)) {
        throw new Error('The study materials service returned an invalid response.');
      }
      setSubjects(subjectResponse.data);
      setMaterials(materialResponse.data);
      setSubjectId((current) => (
        subjectResponse.data.some((subject) => String(subject.id) === String(current))
          ? current
          : ''
      ));
    } catch (error) {
      const message = error?.response?.data?.message || error?.message || 'Failed to load study materials.';
      setSubjects([]);
      setMaterials([]);
      setLoadError(message);
      toast.error(message);
    } finally {
      setLoading(false);
    }
  }, [isStudent, user?.id]);

  useEffect(() => {
    if (user?.id) load();
  }, [load, user?.id]);

  const handleUpload = async (event) => {
    event.preventDefault();
    if (!subjectId || files.length === 0) {
      toast.error('Select at least one file.');
      return;
    }

    setUploading(true);
    try {
      await uploadStudyMaterials({ subjectId, title, description, files });
      toast.success(`${files.length} study material file${files.length === 1 ? '' : 's'} uploaded.`);
      setTitle('');
      setDescription('');
      setFiles([]);
      setFileInputKey((key) => key + 1);
      setFileFormat('ALL');
      await load();
    } catch (error) {
      toast.error(error?.response?.data?.message || error?.message || 'Could not upload study material.');
    } finally {
      setUploading(false);
    }
  };

  const handleDownload = async (material) => {
    try {
      const response = await downloadStudyMaterial(material.id);
      const url = window.URL.createObjectURL(response.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = material.fileName;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => window.URL.revokeObjectURL(url), 0);
    } catch (error) {
      toast.error(error?.response?.data?.message || error?.message || 'Could not download this file.');
    }
  };

  const handleDelete = async (material) => {
    if (!window.confirm(`Delete study material "${material.title || material.fileName}"?`)) return;

    setDeletingId(material.id);
    try {
      await deleteStudyMaterial(material.id);
      toast.success('Study material deleted.');
      await load();
    } catch (error) {
      toast.error(error?.response?.data?.message || error?.message || 'Could not delete this study material.');
    } finally {
      setDeletingId(null);
    }
  };

  const openSubject = (id) => {
    setSubjectId(String(id));
    setFileFormat('ALL');
  };

  const backToSubjects = () => {
    setSubjectId('');
    setFileFormat('ALL');
    setTitle('');
    setDescription('');
    setFiles([]);
    setFileInputKey((key) => key + 1);
  };

  const activeSubject = subjects.find((subject) => String(subject.id) === String(subjectId));
  const subjectMaterials = useMemo(
    () => materials
      .filter((material) => String(material.subjectId) === String(subjectId))
      .sort((left, right) => new Date(right.uploadedAt || 0) - new Date(left.uploadedAt || 0)),
    [materials, subjectId],
  );
  const availableFormats = useMemo(
    () => [...new Set(subjectMaterials.map((material) => getFileExtension(material.fileName)))]
      .sort((left, right) => left.localeCompare(right)),
    [subjectMaterials],
  );
  const filteredMaterials = fileFormat === 'ALL'
    ? subjectMaterials
    : subjectMaterials.filter((material) => (
      fileFormat === 'NO_EXTENSION'
        ? !getFileExtension(material.fileName)
        : getFileExtension(material.fileName) === fileFormat
    ));

  return (
    <Layout pageTitle="Study Material">
      <div className="study-materials-page">
        {loadError && <div className="alert alert-error">{loadError}</div>}

        {loading ? (
          <div className="study-materials-loading" role="status" aria-label="Loading study materials">
            <div className="spinner" />
          </div>
        ) : !activeSubject ? (
          subjects.length === 0 ? (
            <div className="study-materials-empty empty-state">
              <FileStack />
              <h3>No subjects yet</h3>
              <p>No subjects are assigned to your account.</p>
            </div>
          ) : (
            <section className="study-subject-grid" aria-label="Your subjects">
              {[...subjects].sort((a, b) => a.name.localeCompare(b.name)).map((subject) => {
                const count = materials.filter((material) => String(material.subjectId) === String(subject.id)).length;
                return (
                  <button
                    type="button"
                    key={subject.id}
                    className="study-subject-card"
                    onClick={() => openSubject(subject.id)}
                    aria-label={`Open ${subject.name} study materials`}
                  >
                    <span className="study-subject-icon"><FileStack size={21} /></span>
                    <span className="study-subject-copy">
                      <span className="study-subject-name">{subject.name}</span>
                      <span className="study-subject-code">{subject.code || 'Subject'}</span>
                      <span className="study-subject-count">
                        {count} {count === 1 ? 'resource' : 'resources'}
                      </span>
                    </span>
                    <ChevronRight className="study-subject-arrow" size={19} />
                  </button>
                );
              })}
            </section>
          )
        ) : (
          <section className="study-subject-workspace" aria-labelledby="study-subject-title">
            <header className="study-subject-header">
              <button type="button" className="btn btn-secondary study-back-button" onClick={backToSubjects}>
                <ArrowLeft size={16} /> <span>All subjects</span>
              </button>
              <div className="study-subject-heading">
                <span className="study-subject-icon study-subject-icon-large"><FileStack size={23} /></span>
                <div className="study-subject-heading-copy">
                  <p className="study-subject-eyebrow">{canUpload ? 'TEACHING RESOURCES' : 'SUBJECT LIBRARY'}</p>
                  <h2 id="study-subject-title">{activeSubject.name}</h2>
                  <p className="study-subject-description">
                    {activeSubject.code || 'Subject'} <span aria-hidden="true">·</span>{' '}
                    {canUpload
                      ? 'Share resources with students in this subject.'
                      : 'Browse and download resources for this subject.'}
                  </p>
                </div>
              </div>
              <div className="study-resource-total">
                <strong>{subjectMaterials.length}</strong>
                <span>{subjectMaterials.length === 1 ? 'resource' : 'resources'}</span>
              </div>
            </header>

          {canUpload && (
            <form onSubmit={handleUpload} className="study-upload-panel">
              <div className="study-panel-heading">
                <div className="study-panel-icon"><Upload size={18} /></div>
                <div>
                  <h3>Upload study material</h3>
                  <p>Add files for students enrolled in this subject.</p>
                </div>
              </div>
              <div className="study-upload-fields">
                <div className="form-group">
                  <label className="form-label" htmlFor="study-material-title">Title <span>(optional)</span></label>
                  <input
                    id="study-material-title"
                    className="form-input"
                    value={title}
                    onChange={(event) => setTitle(event.target.value)}
                    maxLength={255}
                    placeholder="e.g. Lecture 3 slides"
                  />
                </div>
                <div className="form-group">
                  <label className="form-label" htmlFor="study-material-files">Files <span>(required)</span></label>
                  <input
                    key={fileInputKey}
                    id="study-material-files"
                    className="form-input study-file-input"
                    type="file"
                    multiple
                    onChange={(event) => setFiles(Array.from(event.target.files || []))}
                    required
                  />
                </div>
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="study-material-description">
                  Description <span>(optional)</span>
                </label>
                <textarea
                  id="study-material-description"
                  className="form-textarea"
                  value={description}
                  onChange={(event) => setDescription(event.target.value)}
                  placeholder="Add context or instructions for students."
                />
              </div>
              <button type="submit" className="btn btn-primary study-upload-submit" disabled={uploading || files.length === 0}>
                {uploading ? <span className="spinner study-button-spinner" /> : <Upload size={16} />}
                Upload {files.length ? `${files.length} File${files.length === 1 ? '' : 's'}` : 'Files'}
              </button>
            </form>
          )}

          <div className="study-library-toolbar">
            <div className="study-library-label">
              <div>
                <h3>Available resources</h3>
                <p>Sorted by most recently uploaded</p>
              </div>
            </div>
            <div className="study-filter">
              <label className="form-label" htmlFor="study-material-format">File format</label>
              <select
                id="study-material-format"
                className="form-select"
                value={fileFormat}
                onChange={(event) => setFileFormat(event.target.value)}
              >
                <option value="ALL">All Files</option>
                {availableFormats.map((format) => (
                  <option key={format || 'no-extension'} value={format || 'NO_EXTENSION'}>
                    {format ? `*${format}` : 'No extension'}
                  </option>
                ))}
              </select>
            </div>
            <span className="study-filter-count">
              {filteredMaterials.length} {filteredMaterials.length === 1 ? 'file' : 'files'}
            </span>
          </div>

          {subjectMaterials.length === 0 ? (
            <div className="study-resource-empty empty-state">
              <FileStack />
              <h3>No resources uploaded yet</h3>
              <p>
                {canUpload
                  ? 'Upload the first resource to make it available to students.'
                  : 'There are no study materials available for this subject yet.'}
              </p>
            </div>
          ) : filteredMaterials.length === 0 ? (
            <div className="study-resource-empty empty-state">
              <p>No files match this format.</p>
            </div>
          ) : (
            <div className="study-resource-list">
              {filteredMaterials.map((material) => (
                <article key={material.id} className="study-resource-card">
                  <span className="study-resource-icon"><FileStack size={19} /></span>
                  <div className="study-resource-content">
                    <h4>{material.title || material.fileName}</h4>
                    <p className="study-resource-file">
                      {material.fileName} <span aria-hidden="true">·</span> {formatFileSize(material.fileSize)}
                    </p>
                    {material.description && <p className="study-resource-description">{material.description}</p>}
                    <div className="study-resource-meta">
                      <span>Uploaded by {material.uploadedByName || 'Uploader unavailable'}</span>
                      <span aria-hidden="true">·</span>
                      <time dateTime={material.uploadedAt || undefined}>
                        {material.uploadedAt
                          ? new Date(material.uploadedAt).toLocaleString()
                          : 'Date unavailable'}
                      </time>
                    </div>
                  </div>
                  <div className="study-resource-actions">
                    <button
                      type="button"
                      className="btn btn-secondary study-download-button"
                      onClick={() => handleDownload(material)}
                      aria-label={`Download ${material.fileName}`}
                    >
                      <Download size={16} /> <span>Download</span>
                    </button>
                    {canUpload && String(material.uploadedById) === String(user?.id) && (
                      <button
                        type="button"
                        className="btn btn-danger btn-icon"
                        onClick={() => handleDelete(material)}
                        disabled={deletingId === material.id}
                        aria-label={`Delete ${material.fileName}`}
                        title="Delete study material"
                      >
                        {deletingId === material.id
                          ? <span className="spinner study-button-spinner" />
                          : <Trash2 size={16} />}
                      </button>
                    )}
                  </div>
                </article>
              ))}
            </div>
          )}
          </section>
        )}
      </div>
    </Layout>
  );
}
