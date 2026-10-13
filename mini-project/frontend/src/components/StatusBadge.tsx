import React from 'react';
import { CheckCircle2, AlertTriangle, Bookmark, Clock, AlertCircle, RefreshCw } from 'lucide-react';

export type StatusVariant = 
  | 'available' 
  | 'limited' 
  | 'unavailable' 
  | 'borrowed' 
  | 'overdue' 
  | 'reserved' 
  | 'paid' 
  | 'running'
  | 'completed'
  | 'failed';

interface Props {
  variant: StatusVariant;
  label?: string;
  count?: number;
  size?: 'sm' | 'md';
}

export const StatusBadge: React.FC<Props> = ({ variant, label, count, size = 'sm' }) => {
  const getBadgeConfig = () => {
    switch (variant) {
      case 'available':
        return {
          bg: 'bg-emerald-50 text-emerald-800 border-emerald-200',
          icon: <CheckCircle2 className="w-3 h-3 text-emerald-600" />,
          defaultText: count !== undefined ? `${count} Available` : 'Available',
        };
      case 'limited':
        return {
          bg: 'bg-amber-50 text-amber-800 border-amber-200',
          icon: <AlertTriangle className="w-3 h-3 text-amber-600" />,
          defaultText: count !== undefined ? `Only ${count} left` : 'Limited copies',
        };
      case 'unavailable':
      case 'reserved':
        return {
          bg: 'bg-blue-50 text-blue-800 border-blue-200',
          icon: <Bookmark className="w-3 h-3 text-blue-600" />,
          defaultText: 'Reservable (0 Available)',
        };
      case 'borrowed':
        return {
          bg: 'bg-slate-100 text-slate-700 border-slate-200',
          icon: <Clock className="w-3 h-3 text-slate-500" />,
          defaultText: 'Borrowed',
        };
      case 'overdue':
        return {
          bg: 'bg-rose-50 text-rose-800 border-rose-200',
          icon: <AlertCircle className="w-3 h-3 text-rose-600" />,
          defaultText: 'Overdue',
        };
      case 'paid':
        return {
          bg: 'bg-emerald-50 text-emerald-800 border-emerald-200',
          icon: <CheckCircle2 className="w-3 h-3 text-emerald-600" />,
          defaultText: 'Settled',
        };
      case 'running':
        return {
          bg: 'bg-sky-50 text-sky-800 border-sky-200',
          icon: <RefreshCw className="w-3 h-3 text-sky-600 animate-spin" />,
          defaultText: 'Processing',
        };
      case 'completed':
        return {
          bg: 'bg-emerald-50 text-emerald-800 border-emerald-200',
          icon: <CheckCircle2 className="w-3 h-3 text-emerald-600" />,
          defaultText: 'Completed',
        };
      case 'failed':
        return {
          bg: 'bg-rose-50 text-rose-800 border-rose-200',
          icon: <AlertCircle className="w-3 h-3 text-rose-600" />,
          defaultText: 'Failed',
        };
      default:
        return {
          bg: 'bg-slate-100 text-slate-700 border-slate-200',
          icon: null,
          defaultText: 'Unknown',
        };
    }
  };

  const config = getBadgeConfig();
  const text = label || config.defaultText;

  return (
    <span
      className={`inline-flex items-center gap-1.5 font-medium rounded-full border ${config.bg} ${
        size === 'sm' ? 'text-[11px] px-2.5 py-0.5' : 'text-xs px-3 py-1'
      }`}
    >
      {config.icon}
      <span>{text}</span>
    </span>
  );
};
