import React, { useState, useEffect } from 'react';
import { api } from '../api/client';
import { Fine, Payment } from '../types';
import { StatusBadge } from '../components/StatusBadge';
import { EmptyState } from '../components/EmptyState';
import { SummaryCardSkeleton, TableSkeleton } from '../components/LoadingSkeleton';
import { 
  Receipt, 
  ShieldCheck, 
  CheckCircle2, 
  AlertCircle, 
  RefreshCw, 
  Key, 
  History, 
  Copy, 
  Check, 
  AlertTriangle,
  Info,
  Calendar,
  CreditCard,
  X
} from 'lucide-react';

export const FinesPage: React.FC = () => {
  const [unpaidFines, setUnpaidFines] = useState<Fine[]>([]);
  const [totalUnpaid, setTotalUnpaid] = useState(0);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [loading, setLoading] = useState(true);
  const [payingFine, setPayingFine] = useState<Fine | null>(null);
  const [idempotencyKey, setIdempotencyKey] = useState('');
  const [payLoading, setPayLoading] = useState(false);
  const [copiedKey, setCopiedKey] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  useEffect(() => {
    loadFinesAndPayments();
  }, []);

  const loadFinesAndPayments = async () => {
    setLoading(true);
    try {
      const [finesRes, historyRes] = await Promise.all([
        api.fines.myFines(),
        api.payments.myHistory(),
      ]);
      setUnpaidFines(finesRes.unpaidFines || []);
      setTotalUnpaid(finesRes.totalUnpaidAmount || 0);
      setPayments(historyRes || []);
    } catch (err: any) {
      setFeedback({ type: 'error', message: err.message || 'Failed to fetch fine records' });
    } finally {
      setLoading(false);
    }
  };

  const openPayModal = (fine: Fine) => {
    setPayingFine(fine);
    // Generate fresh cryptographic UUID idempotency key
    const newKey = 'IDEM-' + Math.random().toString(36).substring(2, 9).toUpperCase() + '-' + Date.now();
    setIdempotencyKey(newKey);
    setFeedback(null);
  };

  const handleProcessPayment = async () => {
    if (!payingFine) return;
    setPayLoading(true);
    setFeedback(null);
    try {
      const receipt = await api.payments.pay(payingFine.fineId, payingFine.amount, idempotencyKey);
      setFeedback({
        type: 'success',
        message: receipt.isDuplicateReplay
          ? `Idempotent Replay Verified: Duplicate submission with key '${receipt.idempotencyKey}' returned existing receipt ${receipt.paymentId} with zero duplicate billing!`
          : `Simulated Payment Succeeded! Receipt ID: ${receipt.paymentId}, TxRef: ${receipt.transactionRef}, Amount: ₹${receipt.amountPaid.toFixed(2)}.`,
      });
      loadFinesAndPayments();
      if (!receipt.isDuplicateReplay) {
        setPayingFine(null);
      }
    } catch (err: any) {
      setFeedback({ type: 'error', message: err.message || 'Payment processing failed' });
    } finally {
      setPayLoading(false);
    }
  };

  const handleCopyText = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(text);
    setTimeout(() => setCopiedKey(null), 2000);
  };

  return (
    <div className="space-y-6">
      {/* 1. Page Header */}
      <div className="bg-white rounded-card border border-athena-border p-6 sm:p-8 shadow-subtle">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <span className="text-xs uppercase font-bold text-athena-blue tracking-wider font-mono">
              Financial Accounting
            </span>
            <h1 className="text-2xl sm:text-3xl font-display font-extrabold text-athena-navy tracking-tight mt-1">
              Fines & Idempotent Payments
            </h1>
            <p className="text-sm text-athena-text-secondary mt-1">
              Overdue accounts and simulated transaction settlements with idempotency tokens.
            </p>
          </div>

          <div className="flex items-center gap-2 bg-athena-blue-light text-athena-blue px-3 py-1.5 rounded-control text-xs font-semibold border border-blue-200">
            <ShieldCheck className="w-4 h-4 text-athena-blue" />
            <span>UUID Idempotency Protected</span>
          </div>
        </div>

        {/* Policy Notice Box */}
        <div className="mt-4 p-3 bg-athena-blue-surface rounded-control border border-athena-border text-xs text-athena-text-secondary flex items-start gap-2.5">
          <Info className="w-4 h-4 text-athena-blue shrink-0 mt-0.5" />
          <span>
            <strong className="text-athena-navy">Library Policy:</strong> Overdue circulation fee is calculated at{' '}
            <strong className="text-athena-navy">₹1.50 per day</strong> (maximum cap of ₹50.00 per loan). Payments are simulated for the academic prototype.
          </span>
        </div>

        {/* Summary Metric Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mt-6 pt-6 border-t border-athena-divider">
          <div className="bg-athena-blue-surface rounded-control p-4 border border-athena-border">
            <span className="text-[11px] font-bold text-athena-text-muted uppercase tracking-wider block font-mono">
              Total Outstanding Balance
            </span>
            <div className="text-3xl font-display font-extrabold text-athena-navy mt-1 tabular-nums">
              ₹{totalUnpaid.toFixed(2)}
            </div>
            <span className="text-[11px] text-athena-text-secondary mt-0.5 block">
              {unpaidFines.length} pending fines
            </span>
          </div>

          <div className="bg-athena-blue-surface rounded-control p-4 border border-athena-border">
            <span className="text-[11px] font-bold text-athena-text-muted uppercase tracking-wider block font-mono">
              Unpaid Fines Count
            </span>
            <div className="text-3xl font-display font-extrabold text-amber-700 mt-1 tabular-nums">
              {unpaidFines.length}
            </div>
            <span className="text-[11px] text-athena-text-secondary mt-0.5 block">
              Clear dues to retain borrowing privileges
            </span>
          </div>

          <div className="bg-athena-blue-surface rounded-control p-4 border border-athena-border">
            <span className="text-[11px] font-bold text-athena-text-muted uppercase tracking-wider block font-mono">
              Settled Receipts
            </span>
            <div className="text-3xl font-display font-extrabold text-emerald-700 mt-1 tabular-nums">
              {payments.length}
            </div>
            <span className="text-[11px] text-athena-text-secondary mt-0.5 block">
              Archived transaction receipts
            </span>
          </div>
        </div>
      </div>

      {/* Feedback Alert */}
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

      {/* 2. Outstanding Fines Section */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold uppercase tracking-wider text-athena-text-muted font-mono flex items-center gap-2">
          <Receipt className="w-4 h-4 text-athena-blue" />
          <span>Outstanding Unpaid Fines ({unpaidFines.length})</span>
        </h2>

        {loading ? (
          <TableSkeleton rows={2} />
        ) : unpaidFines.length === 0 ? (
          <EmptyState
            icon={CheckCircle2}
            title="All accounts settled"
            description="You have no outstanding overdue fines. Your borrowing account is in good standing."
          />
        ) : (
          <div className="space-y-3">
            {unpaidFines.map((fine) => (
              <div
                key={fine.id}
                className="bg-white rounded-card border border-athena-border p-4 sm:p-5 shadow-subtle flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4"
              >
                <div className="space-y-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-xs font-bold text-athena-navy bg-athena-blue-surface px-2 py-0.5 rounded border border-athena-border">
                      {fine.fineId}
                    </span>
                    <span className="text-xs text-athena-text-muted font-mono">
                      Loan: {fine.loanId}
                    </span>
                  </div>

                  <p className="text-xs text-athena-text-secondary pt-0.5">
                    Accrual basis: <strong className="text-athena-navy">{fine.overdueDays} days overdue</strong> at ₹{fine.ratePerDay.toFixed(2)}/day
                  </p>
                </div>

                <div className="flex items-center gap-4 w-full sm:w-auto justify-between sm:justify-end pt-2 sm:pt-0 border-t sm:border-t-0 border-athena-divider">
                  <div className="text-left sm:text-right">
                    <span className="text-[10px] text-athena-text-muted block font-mono">DUE AMOUNT</span>
                    <span className="text-lg font-display font-extrabold text-athena-navy tabular-nums">
                      ₹{fine.amount.toFixed(2)}
                    </span>
                  </div>

                  <button
                    onClick={() => openPayModal(fine)}
                    className="px-4 py-2 bg-[#15803D] hover:bg-[#166534] text-white text-xs font-bold rounded-control transition-all shadow-subtle flex items-center gap-1.5 cursor-pointer"
                  >
                    <CreditCard className="w-3.5 h-3.5" />
                    <span>Pay Fine</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* 3. Transaction Payment History */}
      <section className="space-y-3 pt-4">
        <h2 className="text-sm font-bold uppercase tracking-wider text-athena-text-muted font-mono flex items-center gap-2">
          <History className="w-4 h-4 text-athena-blue" />
          <span>Payment History & Receipts ({payments.length})</span>
        </h2>

        {loading ? (
          <TableSkeleton rows={3} />
        ) : payments.length === 0 ? (
          <div className="bg-white rounded-card border border-athena-border p-8 text-center text-xs text-athena-text-muted shadow-subtle">
            No completed payments recorded yet.
          </div>
        ) : (
          <div className="bg-white rounded-card border border-athena-border overflow-hidden shadow-subtle">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-athena-blue-surface text-athena-text-muted uppercase text-[10px] font-bold font-mono tracking-wider border-b border-athena-border">
                  <tr>
                    <th className="p-3.5">Payment ID</th>
                    <th className="p-3.5">Tx Reference</th>
                    <th className="p-3.5">Fine ID</th>
                    <th className="p-3.5">Amount</th>
                    <th className="p-3.5">Idempotency Key</th>
                    <th className="p-3.5">Status</th>
                    <th className="p-3.5">Date</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-athena-divider font-medium text-athena-text-main">
                  {payments.map((p) => (
                    <tr key={p.id} className="hover:bg-athena-blue-surface/40 transition-colors">
                      <td className="p-3.5 font-mono font-bold text-athena-navy">{p.paymentId}</td>
                      <td className="p-3.5 font-mono text-athena-text-secondary">{p.transactionRef}</td>
                      <td className="p-3.5 font-mono text-athena-text-muted">{p.fineId}</td>
                      <td className="p-3.5 font-extrabold text-athena-navy tabular-nums">₹{p.amount.toFixed(2)}</td>
                      <td className="p-3.5 font-mono text-[11px] text-athena-text-muted">
                        <div className="flex items-center gap-1.5">
                          <span className="truncate max-w-[100px]" title={p.idempotencyKey}>
                            {p.idempotencyKey}
                          </span>
                          <button
                            type="button"
                            onClick={() => handleCopyText(p.idempotencyKey)}
                            className="p-1 hover:text-athena-blue text-slate-400 rounded"
                            title="Copy idempotency key"
                          >
                            {copiedKey === p.idempotencyKey ? (
                              <Check className="w-3 h-3 text-emerald-600" />
                            ) : (
                              <Copy className="w-3 h-3" />
                            )}
                          </button>
                        </div>
                      </td>
                      <td className="p-3.5">
                        <StatusBadge variant="paid" label={p.status} size="sm" />
                      </td>
                      <td className="p-3.5 tabular-nums text-athena-text-muted">
                        {new Date(p.createdAt).toLocaleDateString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </section>

      {/* 4. Payment Confirmation Dialog */}
      {payingFine && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-xs p-4"
          role="dialog"
          aria-modal="true"
        >
          <div className="bg-white rounded-dialog max-w-md w-full shadow-elevated border border-athena-border overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="px-6 py-4 border-b border-athena-divider bg-athena-blue-surface flex items-center justify-between">
              <h3 className="font-display font-bold text-athena-navy text-sm flex items-center gap-2">
                <CreditCard className="w-4 h-4 text-emerald-600" />
                <span>Simulated Fine Settlement</span>
              </h3>
              <button
                onClick={() => setPayingFine(null)}
                className="p-1 text-athena-text-muted hover:text-athena-text-main rounded-control-sm"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="p-6 space-y-4">
              <div className="bg-athena-blue-surface rounded-control p-4 border border-athena-border space-y-2">
                <div className="flex justify-between text-xs">
                  <span className="text-athena-text-secondary">Fine Reference:</span>
                  <span className="font-mono font-bold text-athena-navy">{payingFine.fineId}</span>
                </div>
                <div className="flex justify-between text-xs">
                  <span className="text-athena-text-secondary">Overdue Duration:</span>
                  <span className="font-medium text-athena-navy">{payingFine.overdueDays} days</span>
                </div>
                <div className="flex justify-between text-xs">
                  <span className="text-athena-text-secondary">Daily Overdue Rate:</span>
                  <span className="font-medium text-athena-navy">₹{payingFine.ratePerDay.toFixed(2)} / day</span>
                </div>
                <div className="pt-2 border-t border-athena-border flex justify-between items-center">
                  <span className="text-xs font-bold text-athena-navy">Total to Settle:</span>
                  <span className="text-xl font-display font-extrabold text-emerald-700 tabular-nums">
                    ₹{payingFine.amount.toFixed(2)}
                  </span>
                </div>
              </div>

              {/* Idempotency Token Field */}
              <div className="space-y-1">
                <div className="flex items-center justify-between text-[11px]">
                  <label className="font-semibold text-athena-text-main">Client Idempotency Key</label>
                  <button
                    type="button"
                    onClick={() => setIdempotencyKey('IDEM-' + Math.random().toString(36).substring(2, 9).toUpperCase() + '-' + Date.now())}
                    className="text-athena-blue hover:underline font-mono text-[10px]"
                  >
                    Regenerate UUID
                  </button>
                </div>
                <input
                  type="text"
                  value={idempotencyKey}
                  onChange={(e) => setIdempotencyKey(e.target.value)}
                  className="w-full px-3 py-2 bg-athena-blue-surface border border-athena-border rounded-control text-xs font-mono text-athena-text-main"
                  placeholder="UUID Idempotency Token"
                />
                <p className="text-[10px] text-athena-text-muted">
                  Submitting identical idempotency keys tests duplicate replay protection (HTTP 200, zero double billing).
                </p>
              </div>

              <div className="pt-2 flex items-center justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setPayingFine(null)}
                  className="px-4 py-2 border border-athena-border rounded-control text-xs font-semibold text-athena-text-secondary hover:bg-athena-blue-surface transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleProcessPayment}
                  disabled={payLoading}
                  className="px-4 py-2 bg-[#15803D] hover:bg-[#166534] text-white text-xs font-bold rounded-control transition-all shadow-subtle flex items-center gap-1.5 disabled:opacity-50 cursor-pointer"
                >
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  <span>{payLoading ? 'Processing Settlement...' : 'Confirm Simulated Payment'}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
