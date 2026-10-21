import { useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  LayoutDashboard, Users, Building2, BookOpen,
  ClipboardList, CalendarCheck, User, LogOut, GraduationCap, Menu, X
} from 'lucide-react';

const ROLE_BADGE = {
  ADMIN:   'badge-admin',
  HOD:     'badge-hod',
  FACULTY: 'badge-faculty',
  STUDENT: 'badge-student',
};

export default function Layout({ children, pageTitle }) {
  const { user, logout, isAdminOrHod, isFaculty, isStudent } = useAuth();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const navItems = [
    {
      label: 'Main',
      items: [
        { to: '/dashboard', icon: <LayoutDashboard />, text: 'Dashboard', show: true },
        { to: '/profile',   icon: <User />,            text: 'My Profile', show: true },
      ],
    },
    {
      label: 'Management',
      items: [
        { to: '/users',       icon: <Users />,        text: 'User Verification', show: isAdminOrHod },
        { to: '/departments', icon: <Building2 />,    text: 'Departments',       show: isAdminOrHod },
        { to: '/subjects',    icon: <BookOpen />,     text: 'Subjects',          show: true },
        { to: '/assignments', icon: <ClipboardList />,text: 'Assignments',       show: true },
        { to: '/attendance',  icon: <CalendarCheck />,text: 'Attendance',        show: true },
      ],
    },
  ];


  const initials = user?.name
    ? user.name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2)
    : '?';

  return (
    <div className="app-shell">
      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', zIndex: 99 }}
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside className={`sidebar ${sidebarOpen ? 'open' : ''}`}>
        <div className="sidebar-logo">
          <div className="sidebar-logo-inner">
            <div className="sidebar-logo-icon">
              <GraduationCap size={20} color="#fff" />
            </div>
            <div>
              <div className="sidebar-logo-text">IMS</div>
              <div className="sidebar-logo-sub">Institute Management</div>
            </div>
          </div>
        </div>

        <nav className="sidebar-nav">
          {navItems.map((section) => {
            const visibleItems = section.items.filter((i) => i.show);
            if (!visibleItems.length) return null;
            return (
              <div key={section.label}>
                <div className="nav-section-label">{section.label}</div>
                {visibleItems.map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
                    onClick={() => setSidebarOpen(false)}
                  >
                    {item.icon}
                    <span>{item.text}</span>
                  </NavLink>
                ))}
              </div>
            );
          })}
        </nav>

        <div className="sidebar-footer">
          <div className="sidebar-user" onClick={handleLogout} title="Logout">
            <div className="avatar">{initials}</div>
            <div className="sidebar-user-info">
              <div className="sidebar-user-name truncate">{user?.name}</div>
              <div className="sidebar-user-role">{user?.role}</div>
            </div>
            <LogOut size={16} style={{ color: 'var(--text-muted)', flexShrink: 0 }} />
          </div>
        </div>
      </aside>

      {/* Main */}
      <div className="main-content">
        <header className="topbar">
          <button
            className="btn btn-ghost btn-icon"
            style={{ display: 'none' }}
            id="menu-toggle"
            onClick={() => setSidebarOpen(!sidebarOpen)}
          >
            {sidebarOpen ? <X size={20} /> : <Menu size={20} />}
          </button>
          <h1 className="topbar-title">{pageTitle}</h1>
          <span className={`topbar-badge ${ROLE_BADGE[user?.role] || ''}`}>
            {user?.role}
          </span>
          <div className="avatar sm">{initials}</div>
        </header>

        <main className="page-content fade-in-up">{children}</main>
      </div>

      <style>{`
        @media (max-width: 768px) {
          #menu-toggle { display: flex !important; }
        }
      `}</style>
    </div>
  );
}
