import React, { useState } from 'react';
import { User } from '../types';
import { AutocompleteInput } from './AutocompleteInput';
import { 
  BookOpen, 
  Library, 
  Receipt, 
  Activity, 
  SlidersHorizontal, 
  LogIn, 
  LogOut, 
  User as UserIcon,
  Menu,
  X,
  GraduationCap
} from 'lucide-react';

interface Props {
  activeTab: string;
  setActiveTab: (tab: string) => void;
  user: User | null;
  onOpenAuth: () => void;
  onLogout: () => void;
  onSelectBook: (id: string) => void;
  onSearchSubmit: (q: string) => void;
}

export const Navbar: React.FC<Props> = ({
  activeTab,
  setActiveTab,
  user,
  onOpenAuth,
  onLogout,
  onSelectBook,
  onSearchSubmit,
}) => {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const navItems = [
    { id: 'catalogue', label: 'Catalogue', icon: BookOpen, requiresAuth: false },
    { id: 'loans', label: 'My Library', icon: Library, requiresAuth: true },
    { id: 'fines', label: 'Fines & Payments', icon: Receipt, requiresAuth: true },
    { id: 'system-design', label: 'System Design Lab', icon: Activity, requiresAuth: false },
    { id: 'admin', label: 'Library Operations', icon: SlidersHorizontal, requiresAuth: true, adminOnly: true },
  ];

  const visibleNavItems = navItems.filter((item) => {
    if (!item.requiresAuth) return true;
    if (!user) return false;
    if (item.adminOnly) return user.role === 'ADMIN' || user.role === 'LIBRARIAN';
    return true;
  });

  return (
    <header className="bg-white border-b border-athena-border sticky top-0 z-40 shadow-subtle">
      {/* Top Bar: Brand, Search, Account Controls */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16 gap-4 sm:gap-8">
          
          {/* Brand Mark & Title */}
          <div
            onClick={() => setActiveTab('catalogue')}
            className="flex items-center gap-3 cursor-pointer shrink-0 select-none group"
            role="button"
            tabIndex={0}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') setActiveTab('catalogue');
            }}
          >
            <div className="w-10 h-10 rounded-control bg-athena-navy text-white flex items-center justify-center shadow-subtle group-hover:bg-athena-blue transition-colors">
              <Library className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-1.5 font-display font-bold text-athena-navy text-lg tracking-tight">
                <span>Athena</span>
                <span className="text-athena-blue font-extrabold">LMS</span>
              </div>
              <p className="text-[11px] text-athena-text-muted font-medium flex items-center gap-1 -mt-0.5">
                <span>University Library</span>
              </p>
            </div>
          </div>

          {/* Primary Catalogue Search (Center Desktop) */}
          <div className="flex-1 max-w-xl hidden md:block">
            <AutocompleteInput onSelectBook={onSelectBook} onSearchSubmit={onSearchSubmit} />
          </div>

          {/* User Account Controls */}
          <div className="flex items-center gap-3">
            {user ? (
              <div className="flex items-center gap-3 bg-athena-blue-surface px-3 py-1.5 rounded-control border border-athena-border">
                <div className="w-7 h-7 rounded-full bg-athena-blue-light text-athena-blue flex items-center justify-center shrink-0">
                  <UserIcon className="w-4 h-4" />
                </div>
                <div className="text-left hidden sm:block">
                  <div className="text-xs font-bold text-athena-text-main leading-none truncate max-w-[120px]">
                    {user.name}
                  </div>
                  <div className="text-[10px] font-semibold text-athena-blue uppercase tracking-wider font-mono mt-0.5">
                    {user.role}
                  </div>
                </div>
                <button
                  onClick={onLogout}
                  className="p-1.5 text-athena-text-muted hover:text-athena-error hover:bg-athena-error-bg rounded transition-colors ml-1"
                  title="Sign Out"
                  aria-label="Sign Out"
                >
                  <LogOut className="w-4 h-4" />
                </button>
              </div>
            ) : (
              <button
                onClick={onOpenAuth}
                className="flex items-center gap-2 px-4 py-2 bg-[#1460A3] hover:bg-[#0E4B82] text-white rounded-control text-xs font-bold shadow-subtle transition-all active:scale-98 cursor-pointer"
              >
                <LogIn className="w-4 h-4 text-white" />
                <span>Sign In</span>
              </button>
            )}

            {/* Mobile Hamburger Toggle */}
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="p-2 text-athena-text-secondary hover:text-athena-text-main md:hidden rounded-control border border-athena-border"
              aria-label="Toggle navigation menu"
            >
              {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </div>

        {/* Search Bar on Mobile */}
        <div className="pb-3 md:hidden">
          <AutocompleteInput onSelectBook={onSelectBook} onSearchSubmit={onSearchSubmit} />
        </div>
      </div>

      {/* Secondary Row: Navigation Tabs */}
      <div className="border-t border-athena-divider bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <nav className="flex space-x-1 overflow-x-auto scrollbar-none py-1.5" aria-label="Main Navigation">
            {visibleNavItems.map((item) => {
              const Icon = item.icon;
              const isActive = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => {
                    setActiveTab(item.id);
                    setMobileMenuOpen(false);
                  }}
                  className={`px-3.5 py-2 text-xs font-semibold rounded-control-sm flex items-center gap-2 transition-all shrink-0 ${
                    isActive
                      ? 'bg-[#E8F2FA] text-[#1460A3] font-bold shadow-xs'
                      : 'text-slate-700 hover:text-slate-900 hover:bg-slate-100 font-medium'
                  }`}
                  aria-current={isActive ? 'page' : undefined}
                >
                  <Icon className={`w-3.5 h-3.5 ${isActive ? 'text-[#1460A3]' : 'text-slate-500'}`} />
                  <span>{item.label}</span>
                </button>
              );
            })}
          </nav>
        </div>
      </div>

      {/* Mobile Drawer Navigation */}
      {mobileMenuOpen && (
        <div className="md:hidden border-t border-athena-border bg-white p-4 space-y-2 shadow-elevated">
          {visibleNavItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => {
                  setActiveTab(item.id);
                  setMobileMenuOpen(false);
                }}
                className={`w-full px-4 py-2.5 text-xs font-semibold rounded-control flex items-center gap-3 transition-colors ${
                  isActive
                    ? 'bg-athena-blue-light text-athena-blue font-bold'
                    : 'text-athena-text-secondary hover:bg-athena-blue-surface'
                }`}
              >
                <Icon className="w-4 h-4" />
                <span>{item.label}</span>
              </button>
            );
          })}
        </div>
      )}
    </header>
  );
};
