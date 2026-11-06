import { useEffect, useState } from 'react';
import { NavLink } from 'react-router-dom';
import { getDepartmentById } from '../api/departments';
import { BookOpen, CalendarCheck, ClipboardList } from 'lucide-react';

const sections = [
  { key: 'subjects', label: 'Subjects', icon: <BookOpen size={16} /> },
  { key: 'assignments', label: 'Assignments', icon: <ClipboardList size={16} /> },
  { key: 'attendance', label: 'Attendance', icon: <CalendarCheck size={16} /> },
];

export default function DepartmentSectionNav({ departmentId }) {
  const [department, setDepartment] = useState(null);

  useEffect(() => {
    let active = true;
    getDepartmentById(departmentId)
      .then((response) => {
        if (active) setDepartment(response.data);
      })
      .catch(() => {
        if (active) setDepartment(null);
      });
    return () => { active = false; };
  }, [departmentId]);

  return (
    <section className="card" style={{ marginBottom: 20 }}>
      <div className="card-header">
        <div>
          <h2 className="card-title">{department?.name || 'Department'}{department?.code ? ` (${department.code})` : ''}</h2>
          <p className="card-subtitle">Viewing information for this department only.</p>
        </div>
      </div>
      <nav aria-label="Department sections" style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
        {sections.map((section) => (
          <NavLink
            key={section.key}
            to={`/departments/${departmentId}/${section.key}`}
            className={({ isActive }) => `btn ${isActive ? 'btn-primary' : 'btn-secondary'}`}
          >
            {section.icon}{section.label}
          </NavLink>
        ))}
      </nav>
    </section>
  );
}
