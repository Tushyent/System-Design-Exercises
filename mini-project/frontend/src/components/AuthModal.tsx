import React, { useState } from 'react';
import { api } from '../api/client';
import { User, Role } from '../types';
import { 
  X, 
  LogIn, 
  UserPlus, 
  Lock, 
  Mail, 
  User as UserIcon, 
  Eye, 
  EyeOff, 
  Library, 
  GraduationCap, 
  ShieldCheck, 
  KeyRound,
  AlertCircle
} from 'lucide-react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (user: User) => void;
}

const DEMO_ACCOUNTS = [
  { role: 'Student Member', email: 'member@ssn.edu.in', pass: 'member123', icon: GraduationCap },
  { role: 'Librarian', email: 'librarian@ssn.edu.in', pass: 'librarian123', icon: KeyRound },
  { role: 'System Admin', email: 'admin@ssn.edu.in', pass: 'admin123', icon: ShieldCheck },
];

export const AuthModal: React.FC<Props> = ({ isOpen, onClose, onSuccess }) => {
  const [isRegister, setIsRegister] = useState(false);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [role, setRole] = useState<Role>('MEMBER');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      if (isRegister) {
        const res = await api.auth.register(name, email, password, role);
        localStorage.setItem('token', res.token);
        localStorage.setItem('user', JSON.stringify(res));
        onSuccess(res);
      } else {
        const res = await api.auth.login(email, password);
        localStorage.setItem('token', res.token);
        localStorage.setItem('user', JSON.stringify(res));
        onSuccess(res);
      }
      onClose();
    } catch (err: any) {
      setError(err.message || 'Authentication failed. Please check credentials.');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickDemoFill = async (accountEmail: string, accountPass: string) => {
    setEmail(accountEmail);
    setPassword(accountPass);
    setIsRegister(false);
    setLoading(true);
    setError(null);

    try {
      const res = await api.auth.login(accountEmail, accountPass);
      localStorage.setItem('token', res.token);
      localStorage.setItem('user', JSON.stringify(res));
      onSuccess(res);
      onClose();
    } catch (err: any) {
      setError(err.message || 'Demo sign-in failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-xs p-4 overflow-y-auto"
      role="dialog"
      aria-modal="true"
      aria-labelledby="auth-modal-title"
    >
      <div className="bg-white rounded-dialog max-w-md w-full shadow-elevated border border-athena-border overflow-hidden relative my-6 animate-in fade-in zoom-in-95 duration-200">
        
        {/* Modal Header */}
        <div className="px-6 py-5 border-b border-athena-divider bg-athena-blue-surface flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-control-sm bg-athena-navy text-white flex items-center justify-center">
              <Library className="w-4 h-4 text-white" />
            </div>
            <div>
              <h2 id="auth-modal-title" className="font-display font-bold text-athena-navy text-sm">
                Athena LMS Account Portal
              </h2>
              <p className="text-[11px] text-athena-text-muted">
                {isRegister ? 'Register your university library account' : 'Sign in to borrow, return, and reserve titles'}
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 rounded-control-sm text-athena-text-muted hover:text-athena-text-main hover:bg-white transition-colors"
            aria-label="Close authentication dialog"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Tab Toggle: Sign In vs Register */}
        <div className="px-6 pt-4">
          <div className="grid grid-cols-2 p-1 bg-athena-blue-surface rounded-control border border-athena-border text-xs font-semibold">
            <button
              type="button"
              onClick={() => {
                setIsRegister(false);
                setError(null);
              }}
              className={`py-1.5 rounded-control-sm transition-all ${
                !isRegister ? 'bg-white text-athena-blue shadow-subtle font-bold' : 'text-athena-text-secondary hover:text-athena-text-main'
              }`}
            >
              Sign In
            </button>
            <button
              type="button"
              onClick={() => {
                setIsRegister(true);
                setError(null);
              }}
              className={`py-1.5 rounded-control-sm transition-all ${
                isRegister ? 'bg-white text-athena-blue shadow-subtle font-bold' : 'text-athena-text-secondary hover:text-athena-text-main'
              }`}
            >
              Register
            </button>
          </div>
        </div>

        {/* Demo Accounts Quick-Fill Section */}
        <div className="px-6 pt-4">
          <div className="bg-slate-50 border border-athena-border/80 rounded-control p-2.5">
            <span className="text-[10px] uppercase font-bold text-athena-text-muted tracking-wider block mb-1.5 font-mono">
              Academic Demo Quick-Fill:
            </span>
            <div className="grid grid-cols-3 gap-1.5">
              {DEMO_ACCOUNTS.map((acc, i) => {
                const Icon = acc.icon;
                return (
                  <button
                    key={i}
                    type="button"
                    onClick={() => handleQuickDemoFill(acc.email, acc.pass)}
                    className="p-2 bg-white hover:bg-[#E8F2FA] hover:border-[#1460A3] border border-slate-300 rounded-control-sm text-[11px] font-bold text-slate-800 transition-all flex flex-col items-center gap-1.5 shadow-xs cursor-pointer group"
                    title={`Sign in as ${acc.email}`}
                  >
                    <Icon className="w-4 h-4 text-[#1460A3] shrink-0 group-hover:scale-110 transition-transform" />
                    <span className="truncate w-full text-center text-[10px] font-bold text-slate-800">{acc.role}</span>
                  </button>
                );
              })}
            </div>
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="p-3 bg-athena-error-bg text-athena-error text-xs font-medium rounded-control border border-rose-200 flex items-start gap-2">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {isRegister && (
            <div>
              <label className="block text-xs font-semibold text-athena-text-main mb-1">
                Full Name
              </label>
              <div className="relative">
                <UserIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-athena-text-muted" />
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Alex Johnson"
                  className="w-full pl-9 pr-3 py-2 bg-athena-blue-surface hover:bg-white focus:bg-white border border-athena-border focus:border-athena-blue rounded-control text-xs text-athena-text-main focus:outline-none focus:ring-2 focus:ring-athena-blue-light transition-all shadow-subtle"
                />
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-athena-text-main mb-1">
              University Email Address
            </label>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-athena-text-muted" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="student@ssn.edu.in"
                className="w-full pl-9 pr-3 py-2 bg-athena-blue-surface hover:bg-white focus:bg-white border border-athena-border focus:border-athena-blue rounded-control text-xs text-athena-text-main focus:outline-none focus:ring-2 focus:ring-athena-blue-light transition-all shadow-subtle font-mono"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-athena-text-main mb-1">
              Password
            </label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-athena-text-muted" />
              <input
                type={showPassword ? 'text' : 'password'}
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full pl-9 pr-9 py-2 bg-athena-blue-surface hover:bg-white focus:bg-white border border-athena-border focus:border-athena-blue rounded-control text-xs text-athena-text-main focus:outline-none focus:ring-2 focus:ring-athena-blue-light transition-all shadow-subtle font-mono"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-athena-text-muted hover:text-athena-text-main p-0.5"
                title={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
              </button>
            </div>
          </div>

          {isRegister && (
            <div>
              <label className="block text-xs font-semibold text-athena-text-main mb-1">
                Account Role
              </label>
              <select
                value={role}
                onChange={(e) => setRole(e.target.value as Role)}
                className="w-full px-3 py-2 bg-athena-blue-surface border border-athena-border rounded-control text-xs text-athena-text-main focus:outline-none focus:ring-2 focus:ring-athena-blue-light"
              >
                <option value="MEMBER">Member (Student / Faculty)</option>
                <option value="LIBRARIAN">Librarian (Staff Desk)</option>
                <option value="ADMIN">System Administrator</option>
              </select>
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 px-4 bg-[#1460A3] hover:bg-[#0E4B82] text-white text-xs font-bold rounded-control transition-all shadow-subtle flex items-center justify-center gap-2 disabled:opacity-50 active:scale-98 cursor-pointer"
          >
            {loading ? (
              <span>Authenticating...</span>
            ) : isRegister ? (
              <>
                <UserPlus className="w-4 h-4" />
                <span>Create Library Account</span>
              </>
            ) : (
              <>
                <LogIn className="w-4 h-4" />
                <span>Sign In to Athena LMS</span>
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
};
