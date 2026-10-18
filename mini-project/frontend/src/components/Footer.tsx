import React from 'react';
import { Library, Cpu, ShieldCheck, Code2 } from 'lucide-react';

export const Footer: React.FC = () => {
  return (
    <footer className="bg-white border-t border-vidya-border py-8 mt-16 text-xs text-vidya-text-muted">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col md:flex-row items-center justify-between gap-4">
        
        {/* Left: Brand Identity */}
        <div className="flex items-center gap-2">
          <Library className="w-4 h-4 text-vidya-blue" />
          <span className="font-display font-bold text-vidya-navy text-sm">
            Vidya LMS
          </span>
          <span className="text-vidya-border">•</span>
          <span className="text-vidya-text-secondary">
            University Library Platform
          </span>
        </div>

        {/* Center: Architecture Badges */}
        <div className="flex items-center gap-4 text-[11px] font-mono text-vidya-text-secondary">
          <span className="flex items-center gap-1">
            <Cpu className="w-3.5 h-3.5 text-vidya-blue" />
            Snowflake 64-bit ID
          </span>
          <span className="text-vidya-border">•</span>
          <span className="flex items-center gap-1">
            <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
            Token Bucket Limiter
          </span>
          <span className="text-vidya-border">•</span>
          <span className="flex items-center gap-1">
            <Code2 className="w-3.5 h-3.5 text-sky-600" />
            Consistent Hash Ring
          </span>
        </div>

        {/* Right: Academic Lab Attribution */}
        <p className="text-vidya-text-muted text-center md:text-right">
          SSN College of Engineering | UCS3513 System Design Laboratory | Mini Project 3
        </p>
      </div>
    </footer>
  );
};
