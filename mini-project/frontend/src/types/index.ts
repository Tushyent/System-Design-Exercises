export type Role = 'MEMBER' | 'LIBRARIAN' | 'ADMIN';
export type CopyStatus = 'AVAILABLE' | 'BORROWED' | 'RESERVED_FOR_PICKUP' | 'MAINTENANCE' | 'LOST';
export type LoanStatus = 'ACTIVE' | 'RETURNED' | 'OVERDUE';
export type ReservationStatus = 'PENDING' | 'READY_FOR_PICKUP' | 'FULFILLED' | 'CANCELLED' | 'EXPIRED';
export type FineStatus = 'UNPAID' | 'PAID';

export interface User {
  id: string;
  memberId: string;
  name: string;
  email: string;
  role: Role;
  token?: string;
}

export interface Book {
  id: string;
  isbn: string;
  title: string;
  authors: string[];
  categories: string[];
  publisher?: string;
  publicationYear?: number;
  description?: string;
  coverUrl?: string;
  externalRating?: number;
  externalSource?: string;
  totalCopies: number;
  availableCopies: number;
  createdAt: string;
}

export interface BookCopy {
  id: string;
  bookId: string;
  barcode: string;
  status: CopyStatus;
  currentLoanId?: string;
  currentHolderId?: string;
  pickupDeadline?: string;
}

export interface BookDetailResponse {
  book: Book;
  copies: BookCopy[];
  availableCount: number;
  borrowedCount: number;
  reservedCount: number;
  shareableShortUrl: string;
}

export interface Loan {
  id: string;
  loanId: string;
  memberId: string;
  bookId: string;
  bookTitle: string;
  copyId: string;
  barcode: string;
  borrowedAt: string;
  dueDate: string;
  returnedAt?: string;
  status: LoanStatus;
  renewalCount?: number;
  lastRenewedAt?: string;
}

export interface RenewResponse {
  loanId: string;
  bookTitle: string;
  newDueDate: string;
  renewalCount: number;
  maxRenewals: number;
  status: string;
  message: string;
}


export interface Reservation {
  id: string;
  reservationId: string;
  bookId: string;
  bookTitle: string;
  memberId: string;
  memberEmail: string;
  queuePosition: number;
  status: ReservationStatus;
  allocatedCopyId?: string;
  readyAt?: string;
  expiresAt?: string;
  createdAt: string;
}

export interface Fine {
  id: string;
  fineId: string;
  loanId: string;
  memberId: string;
  overdueDays: number;
  ratePerDay: number;
  amount: number;
  status: FineStatus;
  createdAt: string;
}

export interface Payment {
  id: string;
  paymentId: string;
  fineId: string;
  memberId: string;
  amount: number;
  idempotencyKey: string;
  status: string;
  transactionRef: string;
  createdAt: string;
}

export interface AutocompleteSuggestion {
  id: string;
  text: string;
  type: 'TITLE' | 'AUTHOR' | 'CATEGORY';
  score: number;
}

export interface CrawlerJob {
  id: string;
  jobId: string;
  sourceUrl: string;
  status: string;
  recordsFetched: number;
  recordsParsed: number;
  recordsImported: number;
  recordsDeduplicated: number;
  recordsFailed: number;
  durationMs: number;
  throughputBooksPerSec: number;
  errorMessage?: string;
  startedAt: string;
  finishedAt?: string;
}

export interface SystemMetrics {
  rateLimiter: {
    totalRequests: number;
    allowedRequests: number;
    rejectedRequests: number;
    rejectionRatePercent: number;
    avgOverheadMs: number;
  };
  keyValueStore: {
    cacheHits: number;
    cacheMisses: number;
    totalCacheRequests: number;
    hitRatioPercent: number;
  };
  idGenerator: {
    totalIdsGenerated: number;
    collisionsObserved: number;
    algorithm: string;
  };
  consistentHashing: {
    nodes: string[];
    nodeCount: number;
    virtualNodesCount: number;
  };
  urlShortener: {
    totalResolutions: number;
    averageLatencyMs: number;
  };
  autocomplete: {
    totalQueries: number;
    averageLatencyMs: number;
  };
  crawler: {
    totalJobsExecuted: number;
  };
  libraryStats: {
    totalMembers: number;
    totalTitles: number;
    totalCopies: number;
    activeLoans: number;
    pendingReservations: number;
    unpaidFinesCount: number;
    totalUnpaidFinesAmount: number;
  };
}
