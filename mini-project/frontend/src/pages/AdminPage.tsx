import React, { useState, useEffect } from 'react';
import { api } from '../api/client';
import { Book, CrawlerJob } from '../types';
import { StatusBadge } from '../components/StatusBadge';
import { TableSkeleton } from '../components/LoadingSkeleton';
import { 
  SlidersHorizontal, 
  BookPlus, 
  CopyPlus, 
  Globe, 
  Play, 
  RotateCw, 
  CheckCircle2, 
  AlertCircle, 
  Clock, 
  Database,
  ExternalLink,
  Barcode,
  Layers,
  Sparkles
} from 'lucide-react';

export const AdminPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'add-book' | 'copies' | 'crawler'>('add-book');

  // Book creation state
  const [newTitle, setNewTitle] = useState('');
  const [newAuthor, setNewAuthor] = useState('');
  const [newIsbn, setNewIsbn] = useState('');
  const [newCategory, setNewCategory] = useState('Distributed Systems');
  const [newPublisher, setNewPublisher] = useState('');
  const [newYear, setNewYear] = useState<number>(2024);
  const [newCopies, setNewCopies] = useState<number>(3);
  const [newDescription, setNewDescription] = useState('');
  const [isCreatingBook, setIsCreatingBook] = useState(false);

  // Add copies state
  const [books, setBooks] = useState<Book[]>([]);
  const [selectedBookId, setSelectedBookId] = useState('');
  const [copiesToAdd, setCopiesToAdd] = useState<number>(2);
  const [isAddingCopies, setIsAddingCopies] = useState(false);

  // Crawler state
  const [sourceUrl, setSourceUrl] = useState('http://localhost:8080/mock-external-library/books');
  const [isCrawling, setIsCrawling] = useState(false);
  const [crawlerJobs, setCrawlerJobs] = useState<CrawlerJob[]>([]);
  const [isLoadingJobs, setIsLoadingJobs] = useState(false);

  // Notifications
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const fetchBooksAndJobs = async () => {
    try {
      const booksRes = await api.books.getAll(0, 50);
      setBooks(booksRes.content || []);
      if (booksRes.content?.length > 0 && !selectedBookId) {
        setSelectedBookId(booksRes.content[0].id);
      }
    } catch (err: any) {
      console.error('Failed to fetch books:', err);
    }

    try {
      setIsLoadingJobs(true);
      const jobs = await api.crawler.getJobs();
      setCrawlerJobs(jobs || []);
    } catch (err: any) {
      console.error('Failed to fetch crawler jobs:', err);
    } finally {
      setIsLoadingJobs(false);
    }
  };

  useEffect(() => {
    fetchBooksAndJobs();
  }, []);

  const handleCreateBook = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsCreatingBook(true);
    setSuccessMsg(null);
    setErrorMsg(null);

    try {
      const created = await api.books.create({
        title: newTitle,
        authors: newAuthor.split(',').map((s) => s.trim()).filter(Boolean),
        isbn: newIsbn,
        categories: newCategory.split(',').map((s) => s.trim()).filter(Boolean),
        publisher: newPublisher,
        publicationYear: newYear,
        totalCopies: newCopies,
        description: newDescription,
      });

      setSuccessMsg(`Book "${created.title}" successfully added with ${newCopies} physical copies!`);
      setNewTitle('');
      setNewAuthor('');
      setNewIsbn('');
      setNewPublisher('');
      setNewDescription('');
      fetchBooksAndJobs();
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to create book title');
    } finally {
      setIsCreatingBook(false);
    }
  };

  const handleAddCopies = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedBookId) return;
    setIsAddingCopies(true);
    setSuccessMsg(null);
    setErrorMsg(null);

    try {
      const updated = await api.books.addCopies(selectedBookId, copiesToAdd);
      setSuccessMsg(`Successfully allocated ${copiesToAdd} new physical copy barcodes to "${updated.title}"! Total: ${updated.totalCopies}.`);
      fetchBooksAndJobs();
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to allocate physical copies');
    } finally {
      setIsAddingCopies(false);
    }
  };

  const handleTriggerCrawler = async () => {
    setIsCrawling(true);
    setSuccessMsg(null);
    setErrorMsg(null);

    try {
      const job = await api.crawler.trigger(sourceUrl.trim());
      setSuccessMsg(`Crawler ingestion job dispatched! Reference: ${job.jobId || job.id}. Processing mock publisher repository...`);
      // Refresh jobs list after slight delay
      setTimeout(fetchBooksAndJobs, 1200);
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to dispatch crawler job');
    } finally {
      setIsCrawling(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* 1. Header */}
      <div className="bg-white rounded-card border border-vidya-border p-6 sm:p-8 shadow-subtle">
        <div>
          <span className="text-xs uppercase font-bold text-vidya-blue tracking-wider font-mono">
            Administration & Ingestion
          </span>
          <h1 className="text-2xl sm:text-3xl font-display font-extrabold text-vidya-navy tracking-tight mt-1">
            Library Operations
          </h1>
          <p className="text-sm text-vidya-text-secondary mt-1">
            Catalogue management, physical copy allocation, and external ingestion desk.
          </p>
        </div>

        {/* Section Navigation Tabs */}
        <div className="flex items-center gap-2 mt-6 pt-6 border-t border-vidya-divider overflow-x-auto scrollbar-none">
          <button
            onClick={() => setActiveTab('add-book')}
            className={`px-4 py-2 text-xs rounded-control transition-all flex items-center gap-2 shrink-0 cursor-pointer ${
              activeTab === 'add-book'
                ? 'bg-[#1460A3] hover:bg-[#0E4B82] text-white shadow-sm font-bold'
                : 'bg-white text-slate-700 hover:text-slate-900 hover:bg-slate-100 border border-slate-300 font-medium'
            }`}
          >
            <BookPlus className="w-3.5 h-3.5" />
            <span>Add New Title</span>
          </button>

          <button
            onClick={() => setActiveTab('copies')}
            className={`px-4 py-2 text-xs rounded-control transition-all flex items-center gap-2 shrink-0 cursor-pointer ${
              activeTab === 'copies'
                ? 'bg-[#1460A3] hover:bg-[#0E4B82] text-white shadow-sm font-bold'
                : 'bg-white text-slate-700 hover:text-slate-900 hover:bg-slate-100 border border-slate-300 font-medium'
            }`}
          >
            <CopyPlus className="w-3.5 h-3.5" />
            <span>Allocate Copies</span>
          </button>

          <button
            onClick={() => setActiveTab('crawler')}
            className={`px-4 py-2 text-xs rounded-control transition-all flex items-center gap-2 shrink-0 cursor-pointer ${
              activeTab === 'crawler'
                ? 'bg-[#1460A3] hover:bg-[#0E4B82] text-white shadow-sm font-bold'
                : 'bg-white text-slate-700 hover:text-slate-900 hover:bg-slate-100 border border-slate-300 font-medium'
            }`}
          >
            <Globe className="w-3.5 h-3.5" />
            <span>External Web Crawler Desk</span>
          </button>
        </div>
      </div>

      {/* Notifications */}
      {successMsg && (
        <div className="p-4 bg-vidya-success-bg text-vidya-success border border-emerald-200 rounded-control text-xs font-medium flex items-start gap-3 shadow-subtle">
          <CheckCircle2 className="w-4 h-4 text-vidya-success shrink-0 mt-0.5" />
          <span>{successMsg}</span>
        </div>
      )}

      {errorMsg && (
        <div className="p-4 bg-vidya-error-bg text-vidya-error border border-rose-200 rounded-control text-xs font-medium flex items-start gap-3 shadow-subtle">
          <AlertCircle className="w-4 h-4 text-vidya-error shrink-0 mt-0.5" />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* 2. TAB: Add New Book */}
      {activeTab === 'add-book' && (
        <div className="bg-white rounded-card border border-vidya-border p-6 sm:p-8 shadow-subtle space-y-6">
          <div className="border-b border-vidya-divider pb-4">
            <h2 className="font-display font-bold text-vidya-navy text-lg flex items-center gap-2">
              <BookPlus className="w-5 h-5 text-vidya-blue" />
              <span>Create New Catalogue Entry</span>
            </h2>
            <p className="text-xs text-vidya-text-secondary mt-0.5">
              Enter bibliographic metadata. Snowflake IDs and initial barcode copies are generated atomically.
            </p>
          </div>

          <form onSubmit={handleCreateBook} className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-vidya-text-main mb-1">
                  Book Title <span className="text-vidya-error">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  placeholder="e.g. Distributed Consensus in Modern Storage"
                  className="w-full px-3 py-2 bg-vidya-blue-surface border border-vidya-border rounded-control text-xs text-vidya-text-main focus:bg-white focus:outline-none focus:ring-2 focus:ring-vidya-blue-light"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-vidya-text-main mb-1">
                  Authors (comma-separated) <span className="text-vidya-error">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={newAuthor}
                  onChange={(e) => setNewAuthor(e.target.value)}
                  placeholder="e.g. Leslie Lamport, Martin Kleppmann"
                  className="w-full px-3 py-2 bg-vidya-blue-surface border border-vidya-border rounded-control text-xs text-vidya-text-main focus:bg-white focus:outline-none focus:ring-2 focus:ring-vidya-blue-light"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-vidya-text-main mb-1">
                  ISBN-13 <span className="text-vidya-error">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={newIsbn}
                  onChange={(e) => setNewIsbn(e.target.value)}
                  placeholder="e.g. 9780134494166"
                  className="w-full px-3 py-2 bg-vidya-blue-surface border border-vidya-border rounded-control text-xs font-mono text-vidya-text-main focus:bg-white focus:outline-none focus:ring-2 focus:ring-vidya-blue-light"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-vidya-text-main mb-1">
                  Categories (comma-separated)
                </label>
                <input
                  type="text"
                  value={newCategory}
                  onChange={(e) => setNewCategory(e.target.value)}
                  placeholder="e.g. Distributed Systems, System Design"
                  className="w-full px-3 py-2 bg-vidya-blue-surface border border-vidya-border rounded-control text-xs text-vidya-text-main focus:bg-white focus:outline-none focus:ring-2 focus:ring-vidya-blue-light"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-vidya-text-main mb-1">
                  Publisher
                </label>
                <input
                  type="text"
                  value={newPublisher}
                  onChange={(e) => setNewPublisher(e.target.value)}
                  placeholder="e.g. O'Reilly Media"
                  className="w-full px-3 py-2 bg-vidya-blue-surface border border-vidya-border rounded-control text-xs text-vidya-text-main focus:bg-white focus:outline-none focus:ring-2 focus:ring-vidya-blue-light"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-xs font-semibold text-vidya-text-main mb-1">
                    Publication Year
                  </label>
                  <input
                    type="number"
                    value={newYear}
                    onChange={(e) => setNewYear(Number(e.target.value))}
                    className="w-full px-3 py-2 bg-vidya-blue-surface border border-vidya-border rounded-control text-xs font-mono text-vidya-text-main focus:bg-white focus:outline-none focus:ring-2 focus:ring-vidya-blue-light"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-vidya-text-main mb-1">
                    Initial Copies
                  </label>
                  <input
                    type="number"
                    min={1}
                    max={20}
                    value={newCopies}
                    onChange={(e) => setNewCopies(Number(e.target.value))}
                    className="w-full px-3 py-2 bg-vidya-blue-surface border border-vidya-border rounded-control text-xs font-mono text-vidya-text-main focus:bg-white focus:outline-none focus:ring-2 focus:ring-vidya-blue-light"
                  />
                </div>
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-vidya-text-main mb-1">
                Description / Abstract
              </label>
              <textarea
                rows={3}
                value={newDescription}
                onChange={(e) => setNewDescription(e.target.value)}
                placeholder="Overview of curriculum coverage, technical concepts, or prerequisites..."
                className="w-full px-3 py-2 bg-vidya-blue-surface border border-vidya-border rounded-control text-xs text-vidya-text-main focus:bg-white focus:outline-none focus:ring-2 focus:ring-vidya-blue-light"
              />
            </div>

            <div className="pt-2 flex justify-end">
              <button
                type="submit"
                disabled={isCreatingBook}
                className="px-6 py-2.5 bg-[#1460A3] hover:bg-[#0E4B82] text-white text-xs font-bold rounded-control transition-all shadow-subtle flex items-center gap-2 disabled:opacity-50 cursor-pointer"
              >
                <BookPlus className="w-4 h-4" />
                <span>{isCreatingBook ? 'Adding Title...' : 'Add Book to Catalogue'}</span>
              </button>
            </div>
          </form>
        </div>
      )}

      {/* 3. TAB: Allocate Copies */}
      {activeTab === 'copies' && (
        <div className="bg-white rounded-card border border-vidya-border p-6 sm:p-8 shadow-subtle space-y-6">
          <div className="border-b border-vidya-divider pb-4">
            <h2 className="font-display font-bold text-vidya-navy text-lg flex items-center gap-2">
              <CopyPlus className="w-5 h-5 text-vidya-blue" />
              <span>Physical Inventory Copy Allocation</span>
            </h2>
            <p className="text-xs text-vidya-text-secondary mt-0.5">
              Generate new physical copies with barcodes and increment total catalogue availability.
            </p>
          </div>

          <form onSubmit={handleAddCopies} className="space-y-4 max-w-xl">
            <div>
              <label className="block text-xs font-semibold text-vidya-text-main mb-1">
                Select Book Title
              </label>
              <select
                value={selectedBookId}
                onChange={(e) => setSelectedBookId(e.target.value)}
                className="w-full px-3 py-2 bg-vidya-blue-surface border border-vidya-border rounded-control text-xs text-vidya-text-main focus:bg-white focus:outline-none focus:ring-2 focus:ring-vidya-blue-light"
              >
                {books.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.title} (Current: {b.availableCopies} available / {b.totalCopies} total)
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-vidya-text-main mb-1">
                Number of Physical Copies to Add
              </label>
              <input
                type="number"
                min={1}
                max={20}
                value={copiesToAdd}
                onChange={(e) => setCopiesToAdd(Number(e.target.value))}
                className="w-full px-3 py-2 bg-vidya-blue-surface border border-vidya-border rounded-control text-xs font-mono text-vidya-text-main focus:bg-white focus:outline-none focus:ring-2 focus:ring-vidya-blue-light"
              />
              <p className="text-[11px] text-vidya-text-muted mt-1 font-mono">
                Each copy receives a unique physical barcode identifier (e.g. BAR-XXXXX-N).
              </p>
            </div>

            <div className="pt-2">
              <button
                type="submit"
                disabled={isAddingCopies}
                className="px-6 py-2.5 bg-[#1460A3] hover:bg-[#0E4B82] text-white text-xs font-bold rounded-control transition-all shadow-subtle flex items-center gap-2 disabled:opacity-50 cursor-pointer"
              >
                <CopyPlus className="w-4 h-4" />
                <span>{isAddingCopies ? 'Allocating Barcodes...' : 'Allocate Physical Copies'}</span>
              </button>
            </div>
          </form>
        </div>
      )}

      {/* 4. TAB: External Web Crawler Desk */}
      {activeTab === 'crawler' && (
        <div className="space-y-6">
          <div className="bg-white rounded-card border border-vidya-border p-6 sm:p-8 shadow-subtle space-y-4">
            <div className="border-b border-vidya-divider pb-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div>
                <h2 className="font-display font-bold text-vidya-navy text-lg flex items-center gap-2">
                  <Globe className="w-5 h-5 text-vidya-blue" />
                  <span>Asynchronous Web Crawler & Ingestion Engine</span>
                </h2>
                <p className="text-xs text-vidya-text-secondary mt-0.5">
                  Simulated external publisher ingestion engine with multi-threaded HTTP workers and ISBN deduplication.
                </p>
              </div>

              <button
                onClick={fetchBooksAndJobs}
                disabled={isLoadingJobs}
                className="px-3 py-1.5 bg-vidya-blue-surface hover:bg-slate-200 border border-vidya-border rounded-control text-xs font-semibold text-vidya-text-main flex items-center gap-1.5 transition-colors"
              >
                <RotateCw className={`w-3.5 h-3.5 ${isLoadingJobs ? 'animate-spin' : ''}`} />
                <span>Refresh Ingestion Jobs</span>
              </button>
            </div>

            {/* Ingestion Endpoint Form */}
            <div className="flex flex-col sm:flex-row gap-3 items-end">
              <div className="flex-1 w-full">
                <label className="block text-xs font-semibold text-vidya-text-main mb-1">
                  Target Repository Endpoint
                </label>
                <input
                  type="text"
                  value={sourceUrl}
                  onChange={(e) => setSourceUrl(e.target.value)}
                  placeholder="http://localhost:8080/mock-external-library/books"
                  className="w-full px-3 py-2 bg-vidya-blue-surface border border-vidya-border rounded-control text-xs font-mono text-vidya-text-main focus:bg-white focus:outline-none focus:ring-2 focus:ring-vidya-blue-light"
                />
              </div>

              <button
                onClick={handleTriggerCrawler}
                disabled={isCrawling}
                className="w-full sm:w-auto px-6 py-2 bg-[#15803D] hover:bg-[#166534] text-white text-xs font-bold rounded-control transition-all shadow-subtle flex items-center justify-center gap-2 disabled:opacity-50 shrink-0 cursor-pointer"
              >
                <Play className={`w-3.5 h-3.5 ${isCrawling ? 'animate-spin' : ''}`} />
                <span>{isCrawling ? 'Dispatching Job...' : 'Trigger Crawler Ingestion'}</span>
              </button>
            </div>
          </div>

          {/* Crawler Execution History Table */}
          <div className="bg-white rounded-card border border-vidya-border overflow-hidden shadow-subtle space-y-0">
            <div className="px-6 py-4 border-b border-vidya-divider bg-vidya-blue-surface flex items-center justify-between">
              <span className="text-xs font-bold uppercase tracking-wider text-vidya-navy font-mono">
                Ingestion Job History ({crawlerJobs.length})
              </span>
              <span className="text-[11px] font-mono text-vidya-text-muted">
                Deduplication & Throughput telemetry
              </span>
            </div>

            {isLoadingJobs ? (
              <TableSkeleton rows={3} />
            ) : crawlerJobs.length === 0 ? (
              <div className="p-8 text-center text-xs text-vidya-text-muted">
                No crawler jobs triggered yet. Trigger ingestion above to fetch external catalog titles.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-vidya-blue-surface text-vidya-text-muted uppercase text-[10px] font-bold font-mono tracking-wider border-b border-vidya-border">
                    <tr>
                      <th className="p-3.5">Job ID</th>
                      <th className="p-3.5">Status</th>
                      <th className="p-3.5">Fetched</th>
                      <th className="p-3.5">Imported</th>
                      <th className="p-3.5">Deduplicated</th>
                      <th className="p-3.5">Throughput</th>
                      <th className="p-3.5">Duration</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-vidya-divider font-medium text-vidya-text-main">
                    {crawlerJobs.map((job) => {
                      const isComplete = job.status === 'COMPLETED';
                      const isFail = job.status === 'FAILED';
                      return (
                        <tr key={job.id} className="hover:bg-vidya-blue-surface/40 transition-colors">
                          <td className="p-3.5 font-mono text-vidya-navy font-bold">{job.jobId}</td>
                          <td className="p-3.5">
                            <StatusBadge
                              variant={isComplete ? 'completed' : isFail ? 'failed' : 'running'}
                              label={job.status}
                              size="sm"
                            />
                          </td>
                          <td className="p-3.5 tabular-nums">{job.recordsFetched}</td>
                          <td className="p-3.5 tabular-nums text-emerald-700 font-bold">{job.recordsImported}</td>
                          <td className="p-3.5 tabular-nums text-amber-700 font-bold">{job.recordsDeduplicated}</td>
                          <td className="p-3.5 font-mono text-[11px] tabular-nums">
                            {job.throughputBooksPerSec ? `${job.throughputBooksPerSec.toFixed(1)} books/s` : '—'}
                          </td>
                          <td className="p-3.5 font-mono text-[11px] tabular-nums text-vidya-text-muted">
                            {job.durationMs}ms
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
