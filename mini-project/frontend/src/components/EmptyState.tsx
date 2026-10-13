import React from 'react';
import { LucideIcon } from 'lucide-react';

interface Props {
  icon: LucideIcon;
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
  className?: string;
}

export const EmptyState: React.FC<Props> = ({
  icon: Icon,
  title,
  description,
  actionLabel,
  onAction,
  className = '',
}) => {
  return (
    <div
      className={`py-14 px-6 text-center bg-white rounded-card border border-athena-border shadow-subtle flex flex-col items-center justify-center max-w-lg mx-auto ${className}`}
    >
      <div className="w-12 h-12 rounded-full bg-athena-blue-light flex items-center justify-center text-athena-blue mb-3">
        <Icon className="w-6 h-6" />
      </div>
      <h3 className="font-display font-bold text-athena-text-main text-base">{title}</h3>
      <p className="text-xs text-athena-text-secondary mt-1 max-w-sm leading-relaxed">{description}</p>
      {actionLabel && onAction && (
        <button
          onClick={onAction}
          className="mt-4 px-4 py-2 bg-[#1460A3] hover:bg-[#0E4B82] text-white text-xs font-bold rounded-control transition-colors shadow-subtle cursor-pointer"
        >
          {actionLabel}
        </button>
      )}
    </div>
  );
};
