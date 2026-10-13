import React, { useState } from 'react';
import { BookOpen, Bookmark } from 'lucide-react';

interface Props {
  coverUrl?: string;
  title: string;
  author?: string;
  category?: string;
  className?: string;
  aspectRatio?: '2/3' | '3/4';
  size?: 'sm' | 'md' | 'lg';
}

const CATEGORY_THEMES: Record<string, { bg: string; accent: string; text: string }> = {
  'distributed systems': { bg: 'from-[#0C2340] to-[#123B5D]', accent: '#38BDF8', text: '#F0F9FF' },
  'system design': { bg: 'from-[#0F2D4A] to-[#1B4B72]', accent: '#38BDF8', text: '#F0F9FF' },
  'software engineering': { bg: 'from-[#1E293B] to-[#334155]', accent: '#818CF8', text: '#F8FAFC' },
  'algorithms': { bg: 'from-[#064E3B] to-[#047857]', accent: '#34D399', text: '#ECFDF5' },
  'operating systems': { bg: 'from-[#1E1B4B] to-[#312E81]', accent: '#A78BFA', text: '#F5F3FF' },
  'database design': { bg: 'from-[#431407] to-[#78350F]', accent: '#FBBF24', text: '#FEF3C7' },
  'networking': { bg: 'from-[#134E4A] to-[#0F766E]', accent: '#2DD4BF', text: '#F0FDFA' },
  'programming': { bg: 'from-[#172554] to-[#1E40AF]', accent: '#60A5FA', text: '#EFF6FF' },
  'computer science': { bg: 'from-[#0C2340] to-[#1460A3]', accent: '#38BDF8', text: '#F0F9FF' },
  'electrical engineering': { bg: 'from-[#0F172A] to-[#1E3A8A]', accent: '#38BDF8', text: '#F0F9FF' },
  'electronics': { bg: 'from-[#042F2E] to-[#115E59]', accent: '#2DD4BF', text: '#F0FDFA' },
  'mechanical engineering': { bg: 'from-[#1C1917] to-[#44403C]', accent: '#FB923C', text: '#FFF7ED' },
  'civil engineering': { bg: 'from-[#292524] to-[#57534E]', accent: '#CBD5E1', text: '#FAFAF9' },
  'mathematics': { bg: 'from-[#311042] to-[#581C87]', accent: '#C084FC', text: '#FAF5FF' },
  'data science': { bg: 'from-[#0C4A6E] to-[#0369A1]', accent: '#38BDF8', text: '#F0F9FF' },
  'humanities': { bg: 'from-[#701A75] to-[#86198F]', accent: '#F472B6', text: '#FDF2F8' },
  'general reading': { bg: 'from-[#1E293B] to-[#475569]', accent: '#94A3B8', text: '#F8FAFC' },
  'design': { bg: 'from-[#831843] to-[#9D174D]', accent: '#F472B6', text: '#FFF1F2' },
  'philosophy': { bg: 'from-[#2E1065] to-[#4C1D95]', accent: '#A78BFA', text: '#FAF5FF' },
};

export const BookCover: React.FC<Props> = ({
  coverUrl,
  title,
  author,
  category,
  className = '',
  aspectRatio = '2/3',
  size = 'md',
}) => {
  const [imageFailed, setImageFailed] = useState(false);

  // Normalize category for theme matching
  const catKey = (category || '').toLowerCase().trim();
  const theme = CATEGORY_THEMES[catKey] || {
    bg: 'from-[#102A43] to-[#1E3A8A]',
    accent: '#38BDF8',
    text: '#EAF4FC',
  };

  const hasValidUrl = coverUrl && coverUrl.trim().length > 0 && !imageFailed;

  return (
    <div
      className={`relative overflow-hidden rounded-lg shadow-sm border border-athena-border/60 bg-athena-blue-surface flex-shrink-0 select-none ${className}`}
      style={{
        aspectRatio: aspectRatio === '2/3' ? '2 / 3' : '3 / 4',
      }}
    >
      {hasValidUrl ? (
        <img
          src={coverUrl}
          alt={`Cover of ${title}`}
          onError={() => setImageFailed(true)}
          loading="lazy"
          className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
        />
      ) : (
        /* Curated Procedural Academic Book Cover */
        <div
          className={`w-full h-full bg-gradient-to-br ${theme.bg} p-3 flex flex-col justify-between text-left relative overflow-hidden transition-transform duration-300 group-hover:scale-105`}
        >
          {/* Subtle geometric academic background pattern */}
          <div className="absolute inset-0 opacity-10 pointer-events-none">
            <svg width="100%" height="100%" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <pattern id="grid" width="16" height="16" patternUnits="userSpaceOnUse">
                  <path d="M 16 0 L 0 0 0 16" fill="none" stroke="currentColor" strokeWidth="0.5" />
                </pattern>
              </defs>
              <rect width="100%" height="100%" fill="url(#grid)" />
            </svg>
          </div>

          {/* Book Spine Shadow Effect */}
          <div className="absolute top-0 bottom-0 left-0 w-2.5 bg-black/20 pointer-events-none shadow-inner" />

          {/* Top: Category Tag & Emblem */}
          <div className="relative z-10 flex items-start justify-between">
            <span
              className="text-[9px] uppercase font-bold tracking-wider px-1.5 py-0.5 rounded backdrop-blur-xs font-mono"
              style={{ backgroundColor: 'rgba(255, 255, 255, 0.15)', color: theme.text }}
            >
              {category || 'Athena LMS'}
            </span>
            <Bookmark className="w-3.5 h-3.5 opacity-60" style={{ color: theme.accent }} />
          </div>

          {/* Center: Title */}
          <div className="relative z-10 my-auto py-1">
            <h4
              className={`font-display font-bold leading-tight line-clamp-3 ${
                size === 'sm' ? 'text-[11px]' : size === 'lg' ? 'text-sm' : 'text-xs'
              }`}
              style={{ color: theme.text }}
            >
              {title}
            </h4>
          </div>

          {/* Bottom: Author & Library Seal */}
          <div className="relative z-10 pt-1 border-t border-white/10 flex items-center justify-between">
            <p className="text-[10px] opacity-80 truncate max-w-[80%]" style={{ color: theme.text }}>
              {author || 'Academic Press'}
            </p>
            <BookOpen className="w-3 h-3 opacity-50 shrink-0" style={{ color: theme.accent }} />
          </div>
        </div>
      )}
    </div>
  );
};
