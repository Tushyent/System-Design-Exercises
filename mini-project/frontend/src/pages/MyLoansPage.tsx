import React, { useState, useEffect } from 'react';
import { api } from '../api/client';
import { Loan, Reservation } from '../types';
import { BookCover } from '../components/BookCover';
import { StatusBadge } from '../components/StatusBadge';
import { EmptyState } from '../components/EmptyState';
import { SummaryCardSkeleton, TableSkeleton } from '../components/LoadingSkeleton';
import { 
  BookOpen, 
  RotateCcw, 
  Clock, 
  AlertTriangle, 
  CheckCircle2, 
  Bookmark, 
  Calendar, 
  XCircle,
  Barcode,
  Layers,
  Sparkles,
  ArrowRight
} from 'lucide-react';

export const MyLoansPage: React.FC<{ onNavigateToCatalogue?: () => void }> = ({ onNavigateToCatalogue }) => {
  const [loans, setLoans] = useState<Loan[]>([]);
  const [history, setHistory] = useState<Loan[]>([]);
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [loading, setLoading] = useState(true);
  const [returningId, setReturningId] = useState<string | null>(null);
  const [renewingId, setRenewingId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [activeSubTab, setActiveSubTab] = useState<'loans' | 'reservations' | 'history'>('loans');

  useEffect(() => {
    loadData();
  }, []);

  const handleRenew = async (loanId: string, bookTitle: string) => {
    setRenewingId(loanId);
    setFeedback(null);
    try {
      const res = await api.circulation.renew(loanId);
      const newDue = new Date(res.newDueDate).toLocaleDateString(undefined, {
        weekday: 'short',
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      });
      setFeedback({
        type: 'success',
        message: `Successfully renewed "${bookTitle}"! Extended until ${newDue} (Renewals used: ${res.renewalCount}/${res.maxRenewals}).`,
      });
      loadData();
    } catch (err: any) {
      setFeedback({ type: 'error', message: err.message || 'Renewal failed' });
    } finally {
      setRenewingId(null);
    }
  };


  const loadData = async () => {
    setLoading(true);
    try {
      const [loansRes, historyRes, resRes] = await Promise.all([
        api.circulation.myLoans(),
        api.circulation.history(),
        api.reservations.myReservations(),
      ]);
      setLoans(loansRes || []);
      setHistory(historyRes || []);
      setReservations(resRes || []);
    } catch (err: any) {
      setFeedback({ type: 'error', message: err.message || 'Failed to load library circulation records' });
    } finally {
      setLoading(false);
    }
  };

  const handleReturn = async (copyId: string, barcode: string) => {
    setReturningId(copyId);
    setFeedback(null);
    try {
      const res = await api.circulation.returnBook(copyId);
      const fineText = res.overdueDays > 0 ? ` Overdue fine: ₹${res.fineAmount.toFixed(2)} (${res.overdueDays} days overdue).` : '';
      const queueText = res.assignedToReservationId ? ` Copy immediately allocated to reservation queue position #${res.assignedToReservationId}.` : '';

      setFeedback({
        type: 'success',
        message: `Book (Barcode: ${barcode}) returned successfully!${fineText}${queueText}`,
      });
      loadData();
    } catch (err: any) {
      setFeedback({ type: 'error', message: err.message || 'Return failed' });
    } finally {
      setReturningId(null);
    }
  };

  const handleCancelReservation = async (resId: string) => {
    try {
      await api.reservations.cancel(resId);
      setFeedback({ type: 'success', message: 'Reservation cancelled successfully.' });
      loadData();
    } catch (err: any) {
      setFeedback({ type: 'error', message: err.message || 'Cancellation failed' });
    }
  };

  const isOverdue = (dueDateStr: string) => {
    return new Date(dueDateStr) < new Date();
  };

  const overdueCount = loans.filter((l) => isOverdue(l.dueDate)).length;
  const dueSoonCount = loans.filter((l) => {
    const due = new Date(l.dueDate);
    const now = new Date();
    const diffDays = (due.getTime() - now.getTime()) / (1000 * 3600 * 24);
    return diffDays >= 0 && diffDays <= 3;
  }).length;

  return (
    <div className="space-y-6">
      {/* 1. Page Header */}
      <div className="bg-white rounded-card border border-athena-border p-6 sm:p-8 shadow-subtle">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <span className="text-xs uppercase font-bold text-athena-blue tracking-wider font-mono">
              Member Circulation Desk
            </span>
            <h1 className="text-2xl sm:text-3xl font-display font-extrabold text-athena-navy tracking-tight mt-1">
              My Library
            </h1>
            <p className="text-sm text-athena-text-secondary mt-1">
              Manage borrowed books, due dates, and reservation queues.
            </p>
          </div>
        </div>

        {/* 2. Summary Metric Cards Row */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mt-6 pt-6 border-t border-athena-divider">
          <div className="bg-athena-blue-surface rounded-control p-4 border border-athena-border">
            <span className="text-[11px] font-bold text-athena-text-muted uppercase tracking-wider block font-mono">
              Active Loans
            </span>
            <div className="text-2xl font-display font-extrabold text-athena-navy mt-1 tabular-nums">
              {loading ? '-' : loans.length}
            </div>
            <span className="text-[11px] text-athena-text-secondary mt-0.5 block">
              Copies in possession
            </span>
          </div>

          <div className="bg-athena-blue-surface rounded-control p-4 border border-athena-border">
            <span className="text-[11px] font-bold text-amber-800 uppercase tracking-wider block font-mono">
              Due Soon (≤ 3 Days)
            </span>
            <div className="text-2xl font-display font-extrabold text-amber-700 mt-1 tabular-nums">
              {loading ? '-' : dueSoonCount}
            </div>
            <span className="text-[11px] text-athena-text-secondary mt-0.5 block">
              Return before due date
            </span>
          </div>

          <div className="bg-athena-blue-surface rounded-control p-4 border border-athena-border">
            <span className="text-[11px] font-bold text-rose-800 uppercase tracking-wider block font-mono">
              Overdue Loans
            </span>
            <div className="text-2xl font-display font-extrabold text-rose-700 mt-1 tabular-nums">
              {loading ? '-' : overdueCount}
            </div>
            <span className="text-[11px] text-athena-text-secondary mt-0.5 block">
              Accrues ₹1.50/day fine
            </span>
          </div>

          <div className="bg-athena-blue-surface rounded-control p-4 border border-athena-border">
            <span className="text-[11px] font-bold text-athena-blue uppercase tracking-wider block font-mono">
              Reservations
            </span>
            <div className="text-2xl font-display font-extrabold text-athena-blue mt-1 tabular-nums">
              {loading ? '-' : reservations.length}
            </div>
            <span className="text-[11px] text-athena-text-secondary mt-0.5 block">
              FIFO waitlist queues
            </span>
          </div>
        </div>
      </div>

      {/* Feedback Banner */}
      {feedback && (
        <div
          className={`p-4 rounded-control text-xs font-medium flex items-start gap-3 border shadow-subtle ${
            feedback.type === 'success'
              ? 'bg-athena-success-bg text-athena-success border-emerald-200'
              : 'bg-athena-error-bg text-athena-error border-rose-200'
          }`}
        >
          {feedback.type === 'success' ? (
            <CheckCircle2 className="w-4 h-4 text-athena-success shrink-0 mt-0.5" />
          ) : (
            <AlertTriangle className="w-4 h-4 text-athena-error shrink-0 mt-0.5" />
          )}
          <span>{feedback.message}</span>
        </div>
      )}

      {/* 3. Navigation Sub-Tabs */}
      <div className="flex items-center gap-2 border-b border-athena-divider pb-2">
        <button
          onClick={() => setActiveSubTab('loans')}
          className={`px-4 py-2 text-xs font-semibold rounded-control transition-all ${
            activeSubTab === 'loans'
              ? 'bg-white text-athena-blue border border-athena-border shadow-subtle font-bold'
              : 'text-athena-text-secondary hover:text-athena-text-main hover:bg-white/60'
          }`}
        >
          Active Loans ({loans.length})
        </button>
        <button
          onClick={() => setActiveSubTab('reservations')}
          className={`px-4 py-2 text-xs font-semibold rounded-control transition-all ${
            activeSubTab === 'reservations'
              ? 'bg-white text-athena-blue border border-athena-border shadow-subtle font-bold'
              : 'text-athena-text-secondary hover:text-athena-text-main hover:bg-white/60'
          }`}
        >
          Reservation Queues ({reservations.length})
        </button>
        <button
          onClick={() => setActiveSubTab('history')}
          className={`px-4 py-2 text-xs font-semibold rounded-control transition-all ${
            activeSubTab === 'history'
              ? 'bg-white text-athena-blue border border-athena-border shadow-subtle font-bold'
              : 'text-athena-text-secondary hover:text-athena-text-main hover:bg-white/60'
          }`}
        >
          Borrowing History ({history.length})
        </button>
      </div>

      {/* 4. Tab Panels */}
      {loading ? (
        <TableSkeleton rows={4} />
      ) : activeSubTab === 'loans' ? (
        /* Active Loans List */
        loans.length === 0 ? (
          <EmptyState
            icon={BookOpen}
            title="No active borrowed books"
            description="You currently have no borrowed titles. Discover books in the catalogue to borrow a physical copy."
            actionLabel="Explore Catalogue"
            onAction={onNavigateToCatalogue}
          />
        ) : (
          <div className="space-y-3">
            {loans.map((loan) => {
              const overdue = isOverdue(loan.dueDate);
              const isReturning = returningId === loan.copyId;
              const isRenewing = renewingId === loan.loanId;
              const renewalCount = loan.renewalCount || 0;
              const maxRenewalsReached = renewalCount >= 2;

              return (
                <div
                  key={loan.id}
                  className="bg-white rounded-card border border-athena-border p-4 sm:p-5 shadow-subtle flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 transition-all"
                >
                  <div className="flex items-center gap-4 min-w-0">
                    <BookCover
                      title={loan.bookTitle}
                      category="System Design"
                      aspectRatio="2/3"
                      size="sm"
                      className="w-14 shadow-subtle shrink-0"
                    />

                    <div className="space-y-1 min-w-0">
                      <h3 className="font-display font-bold text-athena-navy text-sm truncate">
                        {loan.bookTitle}
                      </h3>
                      
                      <div className="flex flex-wrap items-center gap-2 text-xs text-athena-text-secondary">
                        <span className="font-mono bg-athena-blue-surface px-2 py-0.5 rounded border border-athena-border text-[11px] text-athena-text-main">
                          Barcode: {loan.barcode}
                        </span>
                        <span>•</span>
                        <span className="font-mono text-[11px] text-athena-text-muted">
                          ID: {loan.loanId}
                        </span>
                        <span>•</span>
                        <span className={`text-[11px] font-semibold px-2 py-0.5 rounded ${
                          maxRenewalsReached 
                            ? 'bg-amber-100 text-amber-900 border border-amber-300' 
                            : 'bg-emerald-100 text-emerald-900 border border-emerald-300'
                        }`}>
                          Renewals: {renewalCount}/2
                        </span>
                      </div>

                      <div className="flex items-center gap-2 pt-1 text-xs">
                        <Calendar className="w-3.5 h-3.5 text-athena-text-muted" />
                        <span className="text-athena-text-muted">Due date:</span>
                        <span
                          className={`font-semibold tabular-nums ${
                            overdue ? 'text-athena-error font-bold' : 'text-athena-text-main'
                          }`}
                        >
                          {new Date(loan.dueDate).toLocaleDateString(undefined, {
                            weekday: 'short',
                            year: 'numeric',
                            month: 'short',
                            day: 'numeric',
                          })}
                        </span>
                        {overdue && (
                          <StatusBadge variant="overdue" label="Overdue" size="sm" />
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Actions (Renew & Return) */}
                  <div className="w-full sm:w-auto flex flex-col sm:flex-row items-stretch sm:items-center justify-end gap-2 shrink-0 pt-2 sm:pt-0 border-t sm:border-t-0 border-athena-divider">
                    <button
                      onClick={() => handleRenew(loan.loanId, loan.bookTitle)}
                      disabled={isRenewing || maxRenewalsReached}
                      title={maxRenewalsReached ? "Maximum 2 renewals limit reached" : "Extend loan by 14 days"}
                      className="px-3.5 py-2 bg-white hover:bg-slate-50 text-[#1460A3] border border-[#1460A3]/40 hover:border-[#1460A3] text-xs font-bold rounded-control transition-all shadow-subtle flex items-center justify-center gap-1.5 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
                    >
                      <Clock className={`w-3.5 h-3.5 ${isRenewing ? 'animate-spin' : ''}`} />
                      <span>{isRenewing ? 'Renewing...' : maxRenewalsReached ? 'Max Renewed (2/2)' : 'Renew (+14d)'}</span>
                    </button>
                    <button
                      onClick={() => handleReturn(loan.copyId, loan.barcode)}
                      disabled={isReturning}
                      className="px-4 py-2 bg-[#1460A3] hover:bg-[#0E4B82] text-white text-xs font-bold rounded-control transition-all shadow-subtle flex items-center justify-center gap-2 disabled:opacity-50 cursor-pointer"
                    >
                      <RotateCcw className={`w-3.5 h-3.5 ${isReturning ? 'animate-spin' : ''}`} />
                      <span>{isReturning ? 'Returning...' : 'Return Copy'}</span>
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )
      ) : activeSubTab === 'reservations' ? (
        /* Reservations List */
        reservations.length === 0 ? (
          <EmptyState
            icon={Bookmark}
            title="No active reservations"
            description="When all copies of a title are currently borrowed, you can place a reservation to enter the FIFO waitlist."
          />
        ) : (
          <div className="space-y-3">
            {reservations.map((res) => (
              <div
                key={res.id}
                className="bg-white rounded-card border border-athena-border p-4 sm:p-5 shadow-subtle flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4"
              >
                <div className="space-y-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-[10px] font-bold uppercase tracking-wider bg-athena-blue-light text-athena-blue px-2 py-0.5 rounded">
                      Position #{res.queuePosition}
                    </span>
                    <span className="font-mono text-xs text-athena-text-muted">
                      {res.reservationId}
                    </span>
                  </div>

                  <h3 className="font-display font-bold text-athena-navy text-sm">
                    {res.bookTitle}
                  </h3>

                  <p className="text-xs text-athena-text-secondary">
                    Status: <span className="font-semibold text-athena-text-main">{res.status}</span> • Placed on{' '}
                    {new Date(res.createdAt).toLocaleDateString()}
                  </p>
                </div>

                <button
                  onClick={() => handleCancelReservation(res.id)}
                  className="px-3 py-1.5 bg-rose-50 hover:bg-rose-100 text-athena-error text-xs font-semibold rounded-control transition-colors flex items-center gap-1.5"
                >
                  <XCircle className="w-3.5 h-3.5" />
                  <span>Cancel Reservation</span>
                </button>
              </div>
            ))}
          </div>
        )
      ) : (
        /* Borrowing History Table */
        history.length === 0 ? (
          <EmptyState
            icon={Clock}
            title="No borrowing history"
            description="Completed and returned loans will appear here."
          />
        ) : (
          <div className="bg-white rounded-card border border-athena-border overflow-hidden shadow-subtle">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-athena-blue-surface text-athena-text-muted uppercase text-[10px] font-bold font-mono tracking-wider border-b border-athena-border">
                  <tr>
                    <th className="p-3.5">Title</th>
                    <th className="p-3.5">Barcode</th>
                    <th className="p-3.5">Borrowed</th>
                    <th className="p-3.5">Returned</th>
                    <th className="p-3.5">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-athena-divider font-medium text-athena-text-main">
                  {history.map((h) => (
                    <tr key={h.id} className="hover:bg-athena-blue-surface/40 transition-colors">
                      <td className="p-3.5 font-bold text-athena-navy">{h.bookTitle}</td>
                      <td className="p-3.5 font-mono text-athena-text-secondary">{h.barcode}</td>
                      <td className="p-3.5 tabular-nums text-athena-text-muted">
                        {new Date(h.borrowedAt).toLocaleDateString()}
                      </td>
                      <td className="p-3.5 tabular-nums text-athena-text-muted">
                        {h.returnedAt ? new Date(h.returnedAt).toLocaleDateString() : '—'}
                      </td>
                      <td className="p-3.5">
                        <StatusBadge variant="paid" label={h.status} size="sm" />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )
      )}
    </div>
  );
};
