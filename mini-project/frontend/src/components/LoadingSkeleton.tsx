import React from 'react';

export const BookCardSkeleton: React.FC = () => {
  return (
    <div className="bg-white rounded-card border border-vidya-border p-4 shadow-subtle flex flex-col justify-between animate-pulse">
      <div>
        <div className="w-full aspect-[2/3] bg-slate-200 rounded-lg mb-3" />
        <div className="w-16 h-4 bg-slate-200 rounded-full mb-2" />
        <div className="w-3/4 h-4 bg-slate-200 rounded mb-1.5" />
        <div className="w-1/2 h-3 bg-slate-200 rounded mb-2" />
        <div className="w-full h-8 bg-slate-100 rounded" />
      </div>
      <div className="pt-3 mt-3 border-t border-vidya-divider flex items-center justify-between">
        <div className="w-20 h-5 bg-slate-200 rounded-full" />
        <div className="w-16 h-4 bg-slate-200 rounded" />
      </div>
    </div>
  );
};

export const TableSkeleton: React.FC<{ rows?: number }> = ({ rows = 4 }) => {
  return (
    <div className="bg-white rounded-card border border-vidya-border overflow-hidden shadow-subtle animate-pulse">
      <div className="h-10 bg-slate-100 border-b border-vidya-border" />
      <div className="divide-y divide-vidya-divider">
        {Array.from({ length: rows }).map((_, i) => (
          <div key={i} className="p-4 flex items-center justify-between gap-4">
            <div className="w-1/3 h-4 bg-slate-200 rounded" />
            <div className="w-1/4 h-4 bg-slate-200 rounded" />
            <div className="w-1/6 h-4 bg-slate-200 rounded" />
            <div className="w-16 h-7 bg-slate-200 rounded-control" />
          </div>
        ))}
      </div>
    </div>
  );
};

export const SummaryCardSkeleton: React.FC = () => {
  return (
    <div className="bg-white rounded-card border border-vidya-border p-5 shadow-subtle animate-pulse">
      <div className="w-24 h-3 bg-slate-200 rounded mb-2" />
      <div className="w-16 h-7 bg-slate-200 rounded mb-1" />
      <div className="w-32 h-3 bg-slate-100 rounded" />
    </div>
  );
};
