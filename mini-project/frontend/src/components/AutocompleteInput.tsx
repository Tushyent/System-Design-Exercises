import React, { useState, useEffect, useRef } from 'react';
import { api } from '../api/client';
import { AutocompleteSuggestion } from '../types';
import { Search, Book, User, Tag, Zap, X } from 'lucide-react';

interface Props {
  onSelectBook: (id: string) => void;
  onSearchSubmit: (query: string) => void;
  placeholder?: string;
  className?: string;
}

export const AutocompleteInput: React.FC<Props> = ({
  onSelectBook,
  onSearchSubmit,
  placeholder = 'Search by title, author, category, or ISBN...',
  className = '',
}) => {
  const [query, setQuery] = useState('');
  const [suggestions, setSuggestions] = useState<AutocompleteSuggestion[]>([]);
  const [latencyMs, setLatencyMs] = useState<number | null>(null);
  const [isOpen, setIsOpen] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState<number>(-1);
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!query.trim()) {
      setSuggestions([]);
      setIsOpen(false);
      return;
    }

    const timer = setTimeout(async () => {
      try {
        const res = await api.autocomplete.suggest(query, 6);
        setSuggestions(res.suggestions || []);
        setLatencyMs(res.latencyMs);
        setIsOpen(true);
        setSelectedIndex(-1);
      } catch (err) {
        console.error('Autocomplete error:', err);
      }
    }, 150);

    return () => clearTimeout(timer);
  }, [query]);

  // Click outside listener
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelect = (item: AutocompleteSuggestion) => {
    setIsOpen(false);
    if (item.type === 'TITLE' && item.id) {
      onSelectBook(item.id);
    } else {
      setQuery(item.text);
      onSearchSubmit(item.text);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedIndex((prev) => (prev < suggestions.length - 1 ? prev + 1 : prev));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedIndex((prev) => (prev > 0 ? prev - 1 : -1));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (selectedIndex >= 0 && selectedIndex < suggestions.length) {
        handleSelect(suggestions[selectedIndex]);
      } else {
        setIsOpen(false);
        onSearchSubmit(query);
      }
    } else if (e.key === 'Escape') {
      setIsOpen(false);
    }
  };

  const handleClear = () => {
    setQuery('');
    setSuggestions([]);
    setIsOpen(false);
    onSearchSubmit('');
  };

  return (
    <div ref={wrapperRef} className={`relative w-full ${className}`}>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          setIsOpen(false);
          onSearchSubmit(query);
        }}
        className="relative flex items-center"
      >
        <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-athena-text-muted pointer-events-none" />
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={() => {
            if (suggestions.length > 0) setIsOpen(true);
          }}
          placeholder={placeholder}
          aria-label="Search catalogue"
          className="w-full pl-10 pr-24 py-2 bg-athena-blue-surface hover:bg-white focus:bg-white border border-athena-border focus:border-athena-blue rounded-control text-sm text-athena-text-main placeholder:text-athena-text-muted focus:outline-none focus:ring-2 focus:ring-athena-blue-light transition-all shadow-subtle"
        />

        {query && (
          <button
            type="button"
            onClick={handleClear}
            className="absolute right-14 top-1/2 -translate-y-1/2 p-1 text-athena-text-muted hover:text-athena-text-main rounded transition-colors"
            title="Clear search"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        )}

        {latencyMs !== null && query.trim().length > 0 && (
          <div
            className="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-1 text-[10px] font-mono font-medium text-emerald-700 bg-emerald-50 px-1.5 py-0.5 rounded border border-emerald-200 tabular-nums"
            title="Ranked Prefix Trie response latency"
          >
            <Zap className="w-3 h-3 text-emerald-600" />
            {latencyMs}ms
          </div>
        )}
      </form>

      {/* Autocomplete Dropdown */}
      {isOpen && suggestions.length > 0 && (
        <div className="absolute z-50 left-0 right-0 mt-1.5 bg-white border border-athena-border rounded-control shadow-elevated overflow-hidden divide-y divide-athena-divider animate-in fade-in duration-150">
          <div className="px-3 py-1.5 bg-athena-blue-surface text-[10px] font-bold text-athena-text-muted uppercase tracking-wider flex justify-between items-center">
            <span>Autocomplete Matches</span>
            <span className="font-mono">{suggestions.length} suggestions</span>
          </div>

          <div className="max-h-64 overflow-y-auto">
            {suggestions.map((item, idx) => {
              const isSelected = idx === selectedIndex;
              return (
                <div
                  key={idx}
                  onClick={() => handleSelect(item)}
                  onMouseEnter={() => setSelectedIndex(idx)}
                  className={`px-3 py-2.5 cursor-pointer flex items-center justify-between text-xs transition-colors ${
                    isSelected ? 'bg-athena-blue-light text-athena-blue' : 'hover:bg-athena-blue-surface text-athena-text-main'
                  }`}
                >
                  <div className="flex items-center gap-2.5 truncate pr-2">
                    {item.type === 'TITLE' && <Book className="w-3.5 h-3.5 text-athena-blue shrink-0" />}
                    {item.type === 'AUTHOR' && <User className="w-3.5 h-3.5 text-amber-600 shrink-0" />}
                    {item.type === 'CATEGORY' && <Tag className="w-3.5 h-3.5 text-emerald-600 shrink-0" />}
                    <span className="font-medium truncate">{item.text}</span>
                  </div>

                  <span
                    className={`text-[9px] font-bold px-1.5 py-0.5 rounded uppercase tracking-wider font-mono shrink-0 ${
                      item.type === 'TITLE'
                        ? 'bg-blue-50 text-athena-blue border border-blue-200'
                        : item.type === 'AUTHOR'
                        ? 'bg-amber-50 text-amber-800 border border-amber-200'
                        : 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                    }`}
                  >
                    {item.type}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};
