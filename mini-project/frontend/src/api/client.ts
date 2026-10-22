import {
  Book,
  BookDetailResponse,
  BookCopy,
  CopyDetail,
  Loan,
  RenewResponse,
  Reservation,
  Fine,
  Payment,
  SystemMetrics,
  AutocompleteSuggestion,
  CrawlerJob,
  Member,
  MemberProfile360,
  MemberEligibility,
  AuditLog,
  SystemConfig,
  Role,
  CopyStatus,
  LoanStatus,
  ReservationStatus,
  FineStatus
} from '../types';

const API_BASE = '/api';

function getAuthHeaders(): HeadersInit {
  const token = localStorage.getItem('token');
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (res.status === 429) {
    const retryAfter = res.headers.get('Retry-After') || '60';
    throw new Error(`Rate limit exceeded! Please retry after ${retryAfter} seconds.`);
  }

  const json = await res.json();
  if (!res.ok) {
    const message = json.message || json.error || `HTTP error ${res.status}`;
    throw new Error(message);
  }
  return json.data !== undefined ? json.data : json;
}

export const api = {
  auth: {
    login: async (email: string, password: string) => {
      const res = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      });
      return handleResponse<any>(res);
    },
    register: async (name: string, email: string, password: string, role = 'MEMBER') => {
      const res = await fetch(`${API_BASE}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, email, password, role }),
      });
      return handleResponse<any>(res);
    },
    me: async () => {
      const res = await fetch(`${API_BASE}/auth/me`, { headers: getAuthHeaders() });
      return handleResponse<any>(res);
    },
  },

  members: {
    getAll: async (search?: string, status?: string, role?: string) => {
      const params = new URLSearchParams();
      if (search) params.append('search', search);
      if (status) params.append('status', status);
      if (role) params.append('role', role);
      const res = await fetch(`${API_BASE}/members?${params.toString()}`, { headers: getAuthHeaders() });
      return handleResponse<Member[]>(res);
    },
    getById: async (id: string) => {
      const res = await fetch(`${API_BASE}/members/${id}`, { headers: getAuthHeaders() });
      return handleResponse<Member>(res);
    },
    getProfile360: async (id: string) => {
      const res = await fetch(`${API_BASE}/members/${id}/summary`, { headers: getAuthHeaders() });
      return handleResponse<MemberProfile360>(res);
    },
    checkEligibility: async (id: string) => {
      const res = await fetch(`${API_BASE}/members/${id}/eligibility`, { headers: getAuthHeaders() });
      return handleResponse<MemberEligibility>(res);
    },
    register: async (data: { name: string; email: string; password: string; role?: Role; department?: string; phone?: string; maxBorrowLimit?: number }) => {
      const res = await fetch(`${API_BASE}/members`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
      });
      return handleResponse<Member>(res);
    },
    update: async (id: string, data: any) => {
      const res = await fetch(`${API_BASE}/members/${id}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
      });
      return handleResponse<Member>(res);
    },
    updateStatus: async (id: string, status: string, reason?: string, suspendedUntil?: string) => {
      const res = await fetch(`${API_BASE}/members/${id}/status`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify({ status, reason, suspendedUntil }),
      });
      return handleResponse<Member>(res);
    },
    resetPassword: async (id: string, newPassword: string) => {
      const res = await fetch(`${API_BASE}/members/${id}/reset-password`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ newPassword }),
      });
      return handleResponse<void>(res);
    },
  },

  books: {
    getAll: async (page = 0, size = 12) => {
      const res = await fetch(`${API_BASE}/books?page=${page}&size=${size}`);
      return handleResponse<{ content: Book[]; totalElements: number; totalPages: number }>(res);
    },
    search: async (q: string, mode = 'optimized', page = 0, size = 12) => {
      const res = await fetch(`${API_BASE}/books/search?q=${encodeURIComponent(q)}&mode=${mode}&page=${page}&size=${size}`);
      return handleResponse<{ content: Book[]; totalElements: number; totalPages: number }>(res);
    },
    getDetails: async (id: string) => {
      const res = await fetch(`${API_BASE}/books/${id}`);
      return handleResponse<BookDetailResponse>(res);
    },
    create: async (data: any) => {
      const res = await fetch(`${API_BASE}/books`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
      });
      return handleResponse<Book>(res);
    },
    update: async (id: string, data: any) => {
      const res = await fetch(`${API_BASE}/books/${id}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
      });
      return handleResponse<Book>(res);
    },
    delete: async (id: string, hardDelete = false) => {
      const res = await fetch(`${API_BASE}/books/${id}?hardDelete=${hardDelete}`, {
        method: 'DELETE',
        headers: getAuthHeaders(),
      });
      return handleResponse<void>(res);
    },
    addCopies: async (bookId: string, count: number) => {
      const res = await fetch(`${API_BASE}/books/${bookId}/copies?count=${count}`, {
        method: 'POST',
        headers: getAuthHeaders(),
      });
      return handleResponse<Book>(res);
    },
    getCopies: async (bookId: string) => {
      const res = await fetch(`${API_BASE}/books/${bookId}/copies`, { headers: getAuthHeaders() });
      return handleResponse<BookCopy[]>(res);
    },
    getAllCopies: async (bookId?: string, status?: string, barcode?: string) => {
      const params = new URLSearchParams();
      if (bookId) params.append('bookId', bookId);
      if (status) params.append('status', status);
      if (barcode) params.append('barcode', barcode);
      const res = await fetch(`${API_BASE}/books/copies/all?${params.toString()}`, { headers: getAuthHeaders() });
      return handleResponse<CopyDetail[]>(res);
    },
    updateCopy: async (copyId: string, data: { status?: CopyStatus; barcode?: string; location?: string; conditionNotes?: string }) => {
      const res = await fetch(`${API_BASE}/books/copies/${copyId}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
      });
      return handleResponse<BookCopy>(res);
    },
    deleteCopy: async (copyId: string) => {
      const res = await fetch(`${API_BASE}/books/copies/${copyId}`, {
        method: 'DELETE',
        headers: getAuthHeaders(),
      });
      return handleResponse<void>(res);
    },
  },

  circulation: {
    borrow: async (bookId: string) => {
      const res = await fetch(`${API_BASE}/circulation/borrow`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ bookId }),
      });
      return handleResponse<any>(res);
    },
    staffCheckout: async (data: { memberIdentifier: string; barcode: string; loanDays?: number; overrideLimit?: boolean; overrideReason?: string }) => {
      const res = await fetch(`${API_BASE}/circulation/staff-checkout`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
      });
      return handleResponse<any>(res);
    },
    returnBook: async (copyIdOrBarcode: string) => {
      const res = await fetch(`${API_BASE}/circulation/return`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ copyIdOrBarcode }),
      });
      return handleResponse<any>(res);
    },
    staffReturn: async (data: { barcode: string; condition?: string; damageFee?: number; staffNotes?: string }) => {
      const res = await fetch(`${API_BASE}/circulation/staff-return`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
      });
      return handleResponse<any>(res);
    },
    renew: async (loanId: string) => {
      const res = await fetch(`${API_BASE}/circulation/renew`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ loanId }),
      });
      return handleResponse<RenewResponse>(res);
    },
    myLoans: async () => {
      const res = await fetch(`${API_BASE}/circulation/my-loans`, { headers: getAuthHeaders() });
      return handleResponse<Loan[]>(res);
    },
    history: async () => {
      const res = await fetch(`${API_BASE}/circulation/my-history`, { headers: getAuthHeaders() });
      return handleResponse<Loan[]>(res);
    },
    allLoans: async (status?: string, search?: string) => {
      const params = new URLSearchParams();
      if (status) params.append('status', status);
      if (search) params.append('search', search);
      const res = await fetch(`${API_BASE}/circulation/all-loans?${params.toString()}`, { headers: getAuthHeaders() });
      return handleResponse<Loan[]>(res);
    },
  },

  reservations: {
    create: async (bookId: string) => {
      const res = await fetch(`${API_BASE}/reservations`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ bookId }),
      });
      return handleResponse<Reservation>(res);
    },
    myReservations: async () => {
      const res = await fetch(`${API_BASE}/reservations/my-reservations`, { headers: getAuthHeaders() });
      return handleResponse<Reservation[]>(res);
    },
    cancel: async (reservationId: string) => {
      const res = await fetch(`${API_BASE}/reservations/${reservationId}`, {
        method: 'DELETE',
        headers: getAuthHeaders(),
      });
      return handleResponse<Reservation>(res);
    },
    all: async (status?: string) => {
      const params = new URLSearchParams();
      if (status) params.append('status', status);
      const res = await fetch(`${API_BASE}/reservations/all?${params.toString()}`, { headers: getAuthHeaders() });
      return handleResponse<Reservation[]>(res);
    },
    fulfill: async (reservationId: string) => {
      const res = await fetch(`${API_BASE}/reservations/${reservationId}/fulfill`, {
        method: 'POST',
        headers: getAuthHeaders(),
      });
      return handleResponse<Reservation>(res);
    },
    cancelByStaff: async (reservationId: string, reason: string) => {
      const res = await fetch(`${API_BASE}/reservations/${reservationId}/cancel-by-staff?reason=${encodeURIComponent(reason)}`, {
        method: 'POST',
        headers: getAuthHeaders(),
      });
      return handleResponse<Reservation>(res);
    },
    expire: async (reservationId: string) => {
      const res = await fetch(`${API_BASE}/reservations/${reservationId}/expire`, {
        method: 'POST',
        headers: getAuthHeaders(),
      });
      return handleResponse<Reservation>(res);
    },
  },

  fines: {
    myFines: async () => {
      const res = await fetch(`${API_BASE}/fines/my-fines`, { headers: getAuthHeaders() });
      return handleResponse<{ unpaidFines: Fine[]; totalUnpaidAmount: number; unpaidCount: number }>(res);
    },
    all: async (status?: string) => {
      const params = new URLSearchParams();
      if (status) params.append('status', status);
      const res = await fetch(`${API_BASE}/fines/all?${params.toString()}`, { headers: getAuthHeaders() });
      return handleResponse<Fine[]>(res);
    },
    waive: async (fineId: string, reason: string) => {
      const res = await fetch(`${API_BASE}/fines/${fineId}/waive`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ reason }),
      });
      return handleResponse<Fine>(res);
    },
    adjust: async (fineId: string, newAmount: number, reason: string) => {
      const res = await fetch(`${API_BASE}/fines/${fineId}/adjust`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ newAmount, reason }),
      });
      return handleResponse<Fine>(res);
    },
    cashPayment: async (fineId: string, notes?: string) => {
      const res = await fetch(`${API_BASE}/fines/${fineId}/cash-payment`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ notes }),
      });
      return handleResponse<Fine>(res);
    },
  },

  payments: {
    pay: async (fineId: string, amount: number, idempotencyKey: string) => {
      const res = await fetch(`${API_BASE}/payments/pay`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ fineId, amount, idempotencyKey }),
      });
      return handleResponse<any>(res);
    },
    myHistory: async () => {
      const res = await fetch(`${API_BASE}/payments/my-history`, { headers: getAuthHeaders() });
      return handleResponse<Payment[]>(res);
    },
  },

  admin: {
    getConfig: async () => {
      const res = await fetch(`${API_BASE}/admin/config`, { headers: getAuthHeaders() });
      return handleResponse<SystemConfig>(res);
    },
    updateConfig: async (config: SystemConfig) => {
      const res = await fetch(`${API_BASE}/admin/config`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(config),
      });
      return handleResponse<SystemConfig>(res);
    },
    getStaff: async () => {
      const res = await fetch(`${API_BASE}/admin/staff`, { headers: getAuthHeaders() });
      return handleResponse<Member[]>(res);
    },
    createStaff: async (data: { name: string; email: string; password: string; role: Role; department?: string; phone?: string }) => {
      const res = await fetch(`${API_BASE}/admin/staff`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
      });
      return handleResponse<Member>(res);
    },
    updateStaffRole: async (staffId: string, role: Role) => {
      const res = await fetch(`${API_BASE}/admin/staff/${staffId}/role`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify({ role }),
      });
      return handleResponse<Member>(res);
    },
    toggleStaffStatus: async (staffId: string) => {
      const res = await fetch(`${API_BASE}/admin/staff/${staffId}/toggle-status`, {
        method: 'POST',
        headers: getAuthHeaders(),
      });
      return handleResponse<Member>(res);
    },
  },

  auditLogs: {
    getRecent: async () => {
      const res = await fetch(`${API_BASE}/audit-logs`, { headers: getAuthHeaders() });
      return handleResponse<AuditLog[]>(res);
    },
    getPaged: async (page = 0, size = 30) => {
      const res = await fetch(`${API_BASE}/audit-logs/paged?page=${page}&size=${size}`, { headers: getAuthHeaders() });
      return handleResponse<{ content: AuditLog[]; totalPages: number; totalElements: number }>(res);
    },
  },

  autocomplete: {
    suggest: async (prefix: string, limit = 8) => {
      const res = await fetch(`${API_BASE}/autocomplete?prefix=${encodeURIComponent(prefix)}&limit=${limit}`);
      return handleResponse<{ suggestions: AutocompleteSuggestion[]; latencyMs: number; count: number }>(res);
    },
    evaluate: async (k = 5) => {
      const res = await fetch(`${API_BASE}/autocomplete/evaluate?k=${k}`);
      return handleResponse<any>(res);
    },
  },

  shorturl: {
    shorten: async (targetUrl: string, resourceType: string, resourceId: string) => {
      const res = await fetch(`${API_BASE}/urls/shorten`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ targetUrl, resourceType, resourceId }),
      });
      return handleResponse<any>(res);
    },
    getStats: async () => {
      const res = await fetch(`${API_BASE}/urls/stats`);
      return handleResponse<any>(res);
    },
  },

  crawler: {
    trigger: async (sourceUrl?: string) => {
      const res = await fetch(`${API_BASE}/crawler/trigger`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ sourceUrl }),
      });
      return handleResponse<CrawlerJob>(res);
    },
    getJobs: async () => {
      const res = await fetch(`${API_BASE}/crawler/jobs`, { headers: getAuthHeaders() });
      return handleResponse<CrawlerJob[]>(res);
    },
  },

  consistenthash: {
    getRing: async () => {
      const res = await fetch(`${API_BASE}/consistenthash/ring`);
      return handleResponse<any>(res);
    },
    simulate: async (keyCount = 1000, virtualNodes = 100, initialNodes?: string[]) => {
      const res = await fetch(`${API_BASE}/consistenthash/simulate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ keyCount, virtualNodes, initialNodes }),
      });
      return handleResponse<any>(res);
    },
  },

  sharding: {
    getStats: async () => {
      const res = await fetch(`${API_BASE}/sharding/stats`);
      return handleResponse<any>(res);
    },
  },

  metrics: {
    getSummary: async () => {
      const res = await fetch(`${API_BASE}/metrics/summary`);
      return handleResponse<SystemMetrics>(res);
    },
  },
};
