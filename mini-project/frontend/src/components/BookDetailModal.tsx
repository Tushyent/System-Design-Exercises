import React, { useState, useEffect } from 'react';
import { api } from '../api/client';
import { BookDetailResponse } from '../types';
import { BookCover } from './BookCover';
import { StatusBadge } from './StatusBadge';
import { 
  X, 
  BookOpen, 
  Share2, 
  Check, 
  Copy, 
  Calendar, 
  Bookmark, 
  CheckCircle2, 
  AlertCircle,
  Building2,
  Barcode,
  Layers,
  Sparkles
} from 'lucide-react';

interface Props {
  bookId: string | null;
  onClose: () => void;
  onRefresh: () => void;
  isAuthenticated: boolean;
  onOpenAuth?: () => void;
}

export const BookDetailModal: React.FC<Props> = ({
  bookId,
  onClose,
  onRefresh,
  isAuthenticated,
  onOpenAuth,
}) => {
  const [data, setData] = useState<BookDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [copied, setCopied] = useState(false);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  useEffect(() => {
    if (!bookId) return;
    loadDetails();
  }, [bookId]);

  // Handle escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  const loadDetails = async () => {
    if (!bookId) return;
    setLoading(true);
    setFeedback(null);
    try {
      const res = await api.books.getDetails(bookId);
      setData(res);
    } catch (err: any) {
      setFeedback({ type: 'error', message: err.message || 'Failed to load details' });
    } finally {
      setLoading(false);
    }
  };

  if (!bookId) return null;

  const handleCopyShortUrl = () => {
    if (!data?.shareableShortUrl) return;
    const fullUrl = window.location.origin + data.shareableShortUrl;
    navigator.clipboard.writeText(fullUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleBorrow = async () => {
    if (!data) return;
    setActionLoading(true);
    setFeedback(null);
    try {
      const res = await api.circulation.borrow(data.book.id);
      setFeedback({
        type: 'success',
        message: `Book borrowed successfully! Loan ID: ${res.loanId || res.id}, Barcode: ${res.barcode}. Due date: ${new Date(res.dueDate).toLocaleDateString()}.`,
      });
      loadDetails();
      onRefresh();
    } catch (err: any) {
      setFeedback({ type: 'error', message: err.message || 'Borrowing failed' });
    } finally {
      setActionLoading(false);
    }
  };

  const handleReserve = async () => {
    if (!data) return;
    setActionLoading(true);
    setFeedback(null);
    try {
      const res = await api.reservations.create(data.book.id);
      setFeedback({
        type: 'success',
        message: `Reservation placed successfully! You are #${res.queuePosition} in the FIFO waitlist. The next returned copy will be held for you.`,
      });
      loadDetails();
      onRefresh();
    } catch (err: any) {
      setFeedback({ type: 'error', message: err.message || 'Reservation failed' });
    } finally {
      setActionLoading(false);
    }
  };

  const isAvailable = (data?.availableCount ?? 0) > 0;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-xs p-4 overflow-y-auto"
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-title"
    >
      <div className="bg-white rounded-dialog max-w-2xl w-full shadow-elevated border border-athena-border overflow-hidden relative my-6 animate-in fade-in zoom-in-95 duration-200">
        
        {/* Modal Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-athena-divider bg-athena-blue-surface">
          <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-athena-blue font-mono">
            <BookOpen className="w-4 h-4" />
            <span>Catalogue Item Details</span>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-control-sm text-athena-text-muted hover:text-athena-text-main hover:bg-white transition-colors"
            aria-label="Close dialog"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Content */}
        <div className="p-6 sm:p-8">
          {loading ? (
            <div className="py-16 text-center text-athena-text-muted text-sm animate-pulse">
              Loading book metadata and inventory availability...
            </div>
          ) : data ? (
            <div className="space-y-6">
              
              {/* Feedback Alert */}
              {feedback && (
                <div
                  className={`p-4 rounded-control text-xs font-medium flex items-start gap-3 border ${
                    feedback.type === 'success'
                      ? 'bg-athena-success-bg text-athena-success border-emerald-200'
                      : 'bg-athena-error-bg text-athena-error border-rose-200'
                  }`}
                >
                  {feedback.type === 'success' ? (
                    <CheckCircle2 className="w-4 h-4 text-athena-success shrink-0 mt-0.5" />
                  ) : (
                    <AlertCircle className="w-4 h-4 text-athena-error shrink-0 mt-0.5" />
                  )}
                  <span>{feedback.message}</span>
                </div>
              )}

              {/* 2-Column Book Presentation */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
                
                {/* Left: 2:3 Cover Artwork & Short URL */}
                <div className="sm:col-span-1 space-y-3">
                  <BookCover
                    coverUrl={data.book.coverUrl}
                    title={data.book.title}
                    author={data.book.authors?.join(', ')}
                    category={data.book.categories?.[0]}
                    aspectRatio="2/3"
                    size="lg"
                    className="w-full shadow-subtle"
                  />

                  {/* Shareable Base62 Short URL Box */}
                  {data.shareableShortUrl && (
                    <div className="bg-athena-blue-surface border border-athena-border rounded-control p-2.5 space-y-1.5">
                      <div className="flex items-center justify-between text-[10px] font-bold text-athena-text-muted uppercase tracking-wider font-mono">
                        <span>Shareable Link</span>
                        <span className="text-athena-blue">Base62</span>
                      </div>
                      <div className="flex items-center gap-1.5 bg-white px-2 py-1.5 rounded border border-athena-border">
                        <span className="text-[11px] font-mono text-athena-text-main truncate flex-1">
                          {data.shareableShortUrl}
                        </span>
                        <button
                          type="button"
                          onClick={handleCopyShortUrl}
                          className="p-1 text-athena-blue hover:text-athena-blue-hover transition-colors rounded"
                          title="Copy short link"
                        >
                          {copied ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
                        </button>
                      </div>
                      {copied && (
                        <p className="text-[10px] text-emerald-600 font-semibold text-center">
                          Copied to clipboard!
                        </p>
                      )}
                    </div>
                  )}
                </div>

                {/* Right: Book Details & Meta */}
                <div className="sm:col-span-2 space-y-4">
                  <div>
                    <div className="flex flex-wrap gap-1.5 mb-2">
                      {data.book.categories?.map((cat, i) => (
                        <span
                          key={i}
                          className="text-[10px] font-bold uppercase tracking-wider text-athena-blue bg-athena-blue-light px-2 py-0.5 rounded font-mono"
                        >
                          {cat}
                        </span>
                      ))}
                    </div>

                    <h2 id="modal-title" className="text-xl sm:text-2xl font-display font-extrabold text-athena-navy leading-snug">
                      {data.book.title}
                    </h2>

                    <p className="text-sm font-medium text-athena-text-secondary mt-1">
                      by {data.book.authors?.join(', ') || 'Unknown Author'}
                    </p>
                  </div>

                  {/* Metadata Attributes */}
                  <div className="grid grid-cols-2 gap-3 py-3 border-y border-athena-divider text-xs">
                    <div>
                      <span className="text-athena-text-muted block text-[11px]">ISBN-13</span>
                      <span className="font-mono font-medium text-athena-text-main">{data.book.isbn}</span>
                    </div>

                    <div>
                      <span className="text-athena-text-muted block text-[11px]">Availability</span>
                      <span className="font-semibold text-athena-navy">
                        {data.availableCount} of {data.book.totalCopies} copies available
                      </span>
                    </div>

                    {data.book.publisher && (
                      <div>
                        <span className="text-athena-text-muted block text-[11px]">Publisher</span>
                        <span className="font-medium text-athena-text-main">{data.book.publisher}</span>
                      </div>
                    )}

                    {Boolean(data.book.publicationYear && data.book.publicationYear > 1000) && (
                      <div>
                        <span className="text-athena-text-muted block text-[11px]">Year Published</span>
                        <span className="font-medium text-athena-text-main font-mono">{data.book.publicationYear}</span>
                      </div>
                    )}
                  </div>

                  {/* Book Description */}
                  {data.book.description && (
                    <div className="text-xs text-athena-text-secondary leading-relaxed space-y-1">
                      <span className="font-bold text-athena-navy text-[11px] uppercase tracking-wider block">
                        Overview
                      </span>
                      <p className="line-clamp-4">{data.book.description}</p>
                    </div>
                  )}

                  {/* Primary Action Button */}
                  <div className="pt-2">
                    {!isAuthenticated ? (
                      <button
                        onClick={onOpenAuth}
                        className="w-full py-2.5 px-4 bg-[#1460A3] hover:bg-[#0E4B82] text-white text-xs font-bold rounded-control transition-all shadow-subtle flex items-center justify-center gap-2 cursor-pointer"
                      >
                        <Bookmark className="w-4 h-4 text-white" />
                        <span>Sign In to Borrow or Reserve</span>
                      </button>
                    ) : isAvailable ? (
                      <button
                        onClick={handleBorrow}
                        disabled={actionLoading}
                        className="w-full py-2.5 px-4 bg-[#15803D] hover:bg-[#166534] text-white text-xs font-bold rounded-control transition-all shadow-subtle flex items-center justify-center gap-2 disabled:opacity-50 cursor-pointer"
                      >
                        <CheckCircle2 className="w-4 h-4 text-white" />
                        <span>{actionLoading ? 'Processing Borrow Request...' : 'Borrow Physical Copy Now'}</span>
                      </button>
                    ) : (
                      <button
                        onClick={handleReserve}
                        disabled={actionLoading}
                        className="w-full py-2.5 px-4 bg-[#1460A3] hover:bg-[#0E4B82] text-white text-xs font-bold rounded-control transition-all shadow-subtle flex items-center justify-center gap-2 disabled:opacity-50 cursor-pointer"
                      >
                        <Bookmark className="w-4 h-4 text-white" />
                        <span>{actionLoading ? 'Placing Reservation...' : 'Join FIFO Waitlist (Reserve)'}</span>
                      </button>
                    )}
                  </div>
                </div>
              </div>

              {/* Physical Copy Inventory Table */}
              {data.copies && data.copies.length > 0 && (
                <div className="pt-4 border-t border-athena-divider space-y-2">
                  <div className="flex items-center justify-between text-xs font-bold text-athena-navy">
                    <span className="flex items-center gap-1.5">
                      <Barcode className="w-3.5 h-3.5 text-athena-blue" />
                      <span>Physical Inventory Barcodes ({data.copies.length})</span>
                    </span>
                    <span className="text-[11px] font-mono text-athena-text-muted">
                      Atomic copy tracking
                    </span>
                  </div>

                  <div className="bg-athena-blue-surface rounded-control border border-athena-border overflow-hidden">
                    <div className="max-h-36 overflow-y-auto divide-y divide-athena-divider">
                      {data.copies.map((copy) => (
                        <div
                          key={copy.id}
                          className="px-3.5 py-2 text-xs flex items-center justify-between"
                        >
                          <span className="font-mono text-athena-text-main font-medium">
                            {copy.barcode}
                          </span>
                          <span
                            className={`text-[10px] font-semibold px-2 py-0.5 rounded-full font-mono ${
                              copy.status === 'AVAILABLE'
                                ? 'bg-emerald-100 text-emerald-800'
                                : 'bg-slate-200 text-slate-700'
                            }`}
                          >
                            {copy.status}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="py-12 text-center text-athena-text-muted text-sm">
              Book information unavailable.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
