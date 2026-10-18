import React, { useState, useEffect } from 'react';
import { api } from '../api/client';
import { Book } from '../types';
import { BookCard } from '../components/BookCard';
import { BookCardSkeleton } from '../components/LoadingSkeleton';
import { EmptyState } from '../components/EmptyState';
import { 
  BookOpen, 
  Sparkles, 
  Database, 
  ChevronLeft, 
  ChevronRight, 
  Filter, 
  X,
  Info
} from 'lucide-react';

interface Props {
  onSelectBook: (id: string) => void;
  externalSearchQuery: string;
}

const CATEGORIES = [
  'All',
  'Distributed Systems',
  'System Design',
  'Software Engineering',
  'Computer Science',
  'Electrical Engineering',
  'Mechanical Engineering',
  'Civil Engineering',
  'Mathematics',
  'Data Science',
  'Humanities',
  'General Reading',
];

export const CataloguePage: React.FC<Props> = ({ onSelectBook, externalSearchQuery }) => {
  const [books, setBooks] = useState<Book[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [searchMode, setSearchMode] = useState<'optimized' | 'baseline'>('optimized');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  useEffect(() => {
    loadBooks();
  }, [externalSearchQuery, selectedCategory, searchMode, page]);

  const loadBooks = async () => {
    setLoading(true);
    try {
      const queryToUse = externalSearchQuery.trim();
      let res;
      if (queryToUse) {
        res = await api.books.search(queryToUse, searchMode, page, 12);
        let list = res.content || [];
        if (selectedCategory !== 'All') {
          list = list.filter((b) =>
            b.categories?.some((c) => c.toLowerCase().includes(selectedCategory.toLowerCase()))
          );
        }
        setBooks(list);
        setTotalPages(res.totalPages || 1);
        setTotalElements(res.totalElements || 0);
      } else if (selectedCategory !== 'All') {
        res = await api.books.search(selectedCategory, searchMode, page, 12);
        setBooks(res.content || []);
        setTotalPages(res.totalPages || 1);
        setTotalElements(res.totalElements || 0);
      } else {
        res = await api.books.getAll(page, 12);
        setBooks(res.content || []);
        setTotalPages(res.totalPages || 1);
        setTotalElements(res.totalElements || 0);
      }
    } catch (err) {
      console.error('Error fetching books:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleClearFilters = () => {
    setSelectedCategory('All');
    setPage(0);
  };

  return (
    <div className="space-y-6">
      {/* 1. Page Introduction Header */}
      <div className="bg-white rounded-card border border-vidya-border p-6 sm:p-8 shadow-subtle">
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
          <div className="max-w-2xl">
            <span className="text-xs uppercase font-bold text-vidya-blue tracking-wider font-mono">
              University Library Catalogue
            </span>
            <h1 className="text-2xl sm:text-3xl font-display font-extrabold text-vidya-navy tracking-tight mt-1">
              Discover your next great read
            </h1>
            <p className="text-sm text-vidya-text-secondary mt-1.5 leading-relaxed">
              Explore books, technical references, and learning resources from the library catalogue.
            </p>
          </div>

          {/* Search Optimization Segmented Control (System Design Experiment) */}
          <div className="bg-vidya-blue-surface border border-vidya-border p-1.5 rounded-control flex flex-col sm:flex-row items-start sm:items-center gap-2 self-stretch sm:self-auto">
            <div className="flex items-center gap-1 w-full sm:w-auto">
              <button
                onClick={() => setSearchMode('optimized')}
                className={`flex-1 sm:flex-none flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-control-sm text-xs font-semibold transition-all ${
                  searchMode === 'optimized'
                    ? 'bg-white text-vidya-blue shadow-subtle font-bold'
                    : 'text-vidya-text-secondary hover:text-vidya-text-main'
                }`}
                title="Search using indexed database paths and cache-aside storage"
              >
                <Sparkles className="w-3.5 h-3.5 text-vidya-blue" />
                <span>Optimized Search</span>
              </button>
              <button
                onClick={() => setSearchMode('baseline')}
                className={`flex-1 sm:flex-none flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-control-sm text-xs font-semibold transition-all ${
                  searchMode === 'baseline'
                    ? 'bg-white text-vidya-warning shadow-subtle font-bold'
                    : 'text-vidya-text-secondary hover:text-vidya-text-main'
                }`}
                title="Search using unindexed regex scan across titles and authors"
              >
                <Database className="w-3.5 h-3.5 text-vidya-warning" />
                <span>Baseline Search</span>
              </button>
            </div>
          </div>
        </div>

        {/* 2. Category Filters & Active Status */}
        <div className="mt-6 pt-5 border-t border-vidya-divider flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-2 pt-1">
            <div className="flex items-center gap-1.5 text-xs text-slate-600 font-bold shrink-0 mr-1">
              <Filter className="w-3.5 h-3.5 text-[#1460A3]" />
              <span>Category:</span>
            </div>

            {CATEGORIES.map((cat) => {
              const isSelected = selectedCategory === cat;
              return (
                <button
                  key={cat}
                  onClick={() => {
                    setSelectedCategory(cat);
                    setPage(0);
                  }}
                  className={`px-3.5 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-all shrink-0 cursor-pointer ${
                    isSelected
                      ? 'bg-[#1460A3] hover:bg-[#0E4B82] text-white shadow-sm font-bold'
                      : 'bg-white text-slate-700 hover:text-slate-900 hover:bg-slate-100 border border-slate-300'
                  }`}
                >
                  {cat}
                </button>
              );
            })}
          </div>

          {/* Result Count and Clear Filters */}
          <div className="flex items-center gap-2 text-xs text-vidya-text-muted shrink-0">
            <span className="font-medium tabular-nums">
              Showing {books.length} of {totalElements} titles
            </span>
            {(selectedCategory !== 'All' || externalSearchQuery) && (
              <button
                onClick={handleClearFilters}
                className="flex items-center gap-1 text-vidya-blue hover:text-vidya-blue-hover font-semibold ml-1 transition-colors"
              >
                <X className="w-3 h-3" />
                <span>Clear filters</span>
              </button>
            )}
          </div>
        </div>
      </div>

      {/* 3. Book Catalogue Grid */}
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
          {Array.from({ length: 8 }).map((_, idx) => (
            <BookCardSkeleton key={idx} />
          ))}
        </div>
      ) : books.length === 0 ? (
        <EmptyState
          icon={BookOpen}
          title="No titles match your criteria"
          description={
            externalSearchQuery
              ? `We couldn't find any titles matching "${externalSearchQuery}". Try another keyword or reset the category filter.`
              : `No books available under "${selectedCategory}".`
          }
          actionLabel="Reset Filters"
          onAction={handleClearFilters}
        />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
          {books.map((book) => (
            <BookCard key={book.id} book={book} onSelectBook={onSelectBook} />
          ))}
        </div>
      )}

      {/* 4. Pagination */}
      {totalPages > 1 && !loading && (
        <div className="bg-white rounded-card border border-vidya-border p-4 shadow-subtle flex items-center justify-between">
          <p className="text-xs text-vidya-text-secondary">
            Page <span className="font-bold text-vidya-text-main">{page + 1}</span> of{' '}
            <span className="font-bold text-vidya-text-main">{totalPages}</span>
          </p>

          <div className="flex items-center gap-2">
            <button
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="px-3 py-1.5 rounded-control-sm bg-vidya-blue-surface hover:bg-slate-200 border border-vidya-border text-vidya-text-main text-xs font-semibold disabled:opacity-40 disabled:pointer-events-none transition-colors flex items-center gap-1"
            >
              <ChevronLeft className="w-3.5 h-3.5" />
              <span>Previous</span>
            </button>

            <button
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              className="px-3 py-1.5 rounded-control-sm bg-vidya-blue-surface hover:bg-slate-200 border border-vidya-border text-vidya-text-main text-xs font-semibold disabled:opacity-40 disabled:pointer-events-none transition-colors flex items-center gap-1"
            >
              <span>Next</span>
              <ChevronRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
