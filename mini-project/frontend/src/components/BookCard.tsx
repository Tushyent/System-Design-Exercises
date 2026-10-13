import React from 'react';
import { Book } from '../types';
import { BookCover } from './BookCover';
import { StatusBadge, StatusVariant } from './StatusBadge';
import { Star, ChevronRight, Calendar } from 'lucide-react';

interface Props {
  book: Book;
  onSelectBook: (id: string) => void;
}

export const BookCard: React.FC<Props> = ({ book, onSelectBook }) => {
  const getAvailabilityVariant = (): { variant: StatusVariant; label?: string } => {
    if (book.availableCopies > 1) {
      return { variant: 'available', label: `${book.availableCopies} Available` };
    }
    if (book.availableCopies === 1) {
      return { variant: 'limited', label: '1 Copy Left' };
    }
    return { variant: 'unavailable', label: 'Reservable (0 Available)' };
  };

  const status = getAvailabilityVariant();
  const primaryCategory = book.categories?.[0] || 'Computer Science';
  const authorDisplay = book.authors?.join(', ') || 'Unknown Author';

  return (
    <div
      onClick={() => onSelectBook(book.id)}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          onSelectBook(book.id);
        }
      }}
      className="group bg-white rounded-card border border-slate-300 hover:border-[#1460A3] shadow-subtle hover:shadow-card-hover transition-all duration-200 p-4 flex flex-col justify-between cursor-pointer text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-[#1460A3]"
    >
      <div>
        {/* Book Cover (2:3 aspect ratio) */}
        <div className="relative mb-3.5">
          <BookCover
            coverUrl={book.coverUrl}
            title={book.title}
            author={authorDisplay}
            category={primaryCategory}
            className="w-full shadow-subtle"
            aspectRatio="2/3"
            size="md"
          />

          {/* Rating pill overlay if available */}
          {book.externalRating && (
            <div className="absolute top-2 right-2 bg-white/95 backdrop-blur-xs px-2 py-0.5 rounded-full shadow-xs border border-athena-border/60 flex items-center gap-1 text-[11px] font-bold text-amber-700 tabular-nums">
              <Star className="w-3 h-3 fill-amber-400 text-amber-400" />
              <span>{book.externalRating.toFixed(1)}</span>
            </div>
          )}
        </div>

        {/* Category Pill */}
        <div className="flex items-center gap-1.5 mb-1.5">
          <span className="text-[10px] font-bold uppercase tracking-wider text-athena-blue bg-athena-blue-light px-2 py-0.5 rounded font-mono truncate max-w-full">
            {primaryCategory}
          </span>
          {Boolean(book.publicationYear && book.publicationYear > 1000) && (
            <span className="text-[11px] text-athena-text-muted flex items-center gap-1 font-mono">
              <span>•</span>
              <span>{book.publicationYear}</span>
            </span>
          )}
        </div>

        {/* Title */}
        <h3 className="font-display font-bold text-athena-navy text-sm leading-snug line-clamp-2 group-hover:text-athena-blue transition-colors">
          {book.title}
        </h3>

        {/* Author */}
        <p className="text-xs text-athena-text-secondary mt-1 line-clamp-1">
          {authorDisplay}
        </p>
      </div>

      {/* Card Footer: Status & Action */}
      <div className="pt-3 mt-3 border-t border-athena-divider flex items-center justify-between gap-2">
        <StatusBadge variant={status.variant} label={status.label} size="sm" />
        <span className="text-xs font-semibold text-athena-blue group-hover:translate-x-0.5 transition-transform flex items-center gap-0.5 shrink-0">
          <span>Details</span>
          <ChevronRight className="w-3.5 h-3.5" />
        </span>
      </div>
    </div>
  );
};
