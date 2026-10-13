import { Book, BookDetailResponse, Loan, RenewResponse, Reservation, Fine, Payment, SystemMetrics, AutocompleteSuggestion, CrawlerJob } from '../types';

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
    addCopies: async (bookId: string, count: number) => {
      const res = await fetch(`${API_BASE}/books/${bookId}/copies?count=${count}`, {
        method: 'POST',
        headers: getAuthHeaders(),
      });
      return handleResponse<Book>(res);
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
    returnBook: async (copyIdOrBarcode: string) => {
      const res = await fetch(`${API_BASE}/circulation/return`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ copyIdOrBarcode }),
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
  },

  fines: {
    myFines: async () => {
      const res = await fetch(`${API_BASE}/fines/my-fines`, { headers: getAuthHeaders() });
      return handleResponse<{ unpaidFines: Fine[]; totalUnpaidAmount: number; unpaidCount: number }>(res);
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
