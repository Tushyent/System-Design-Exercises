import React, { useState, useEffect } from 'react';
import { api } from '../api/client';
import { SystemMetrics } from '../types';
import { StatusBadge } from '../components/StatusBadge';
import { SummaryCardSkeleton } from '../components/LoadingSkeleton';
import {
  Activity,
  Layers,
  Database,
  Hash,
  ShieldAlert,
  Zap,
  Globe,
  RefreshCw,
  Play,
  CheckCircle2,
  PieChart,
  BarChart3,
  Server,
  Cpu,
  Sliders,
  Check,
  Copy,
  Info,
  Clock,
  Search,
  ExternalLink
} from 'lucide-react';

export const SystemDesignDashboard: React.FC = () => {
  const [metrics, setMetrics] = useState<SystemMetrics | null>(null);
  const [shardingStats, setShardingStats] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'overview' | 'consistent-hash' | 'rate-limiter' | 'snowflake' | 'autocomplete' | 'caching'>('overview');

  // Consistent Hashing Simulator state
  const [chSimResult, setChSimResult] = useState<any>(null);
  const [chKeyCount, setChKeyCount] = useState(1000);
  const [chVNodes, setChVNodes] = useState(100);
  const [chSimLoading, setChSimLoading] = useState(false);

  // Autocomplete Accuracy Evaluation state
  const [autoEvalResult, setAutoEvalResult] = useState<any>(null);
  const [autoEvalLoading, setAutoEvalLoading] = useState(false);
  const [samplePrefix, setSamplePrefix] = useState('Dist');
  const [prefixResult, setPrefixResult] = useState<any>(null);

  // Snowflake Live Demo state
  const [sampleId, setSampleId] = useState<string>('102112694836006912');
  const [copiedId, setCopiedId] = useState(false);

  useEffect(() => {
    loadAllMetrics();
  }, []);

  const loadAllMetrics = async () => {
    setLoading(true);
    try {
      const [metricsRes, shardRes] = await Promise.all([
        api.metrics.getSummary(),
        api.sharding.getStats(),
      ]);
      setMetrics(metricsRes);
      setShardingStats(shardRes);
    } catch (err) {
      console.error('Failed to load system metrics:', err);
    } finally {
      setLoading(false);
    }
  };

  const runConsistentHashSimulation = async () => {
    setChSimLoading(true);
    try {
      const res = await api.consistenthash.simulate(chKeyCount, chVNodes);
      setChSimResult(res);
    } catch (err) {
      console.error('Consistent hashing simulation failed:', err);
    } finally {
      setChSimLoading(false);
    }
  };

  const runAutocompleteEvaluation = async () => {
    setAutoEvalLoading(true);
    try {
      const res = await api.autocomplete.evaluate(5);
      setAutoEvalResult(res);
    } catch (err) {
      console.error('Autocomplete evaluation failed:', err);
    } finally {
      setAutoEvalLoading(false);
    }
  };

  const testPrefixSearch = async () => {
    if (!samplePrefix.trim()) return;
    try {
      const res = await api.autocomplete.suggest(samplePrefix, 5);
      setPrefixResult(res);
    } catch (err) {
      console.error('Prefix test failed:', err);
    }
  };

  const handleCopyId = (id: string) => {
    navigator.clipboard.writeText(id);
    setCopiedId(true);
    setTimeout(() => setCopiedId(false), 2000);
  };

  return (
    <div className="space-y-6">
      {/* 1. Header */}
      <div className="bg-white rounded-card border border-athena-border p-6 sm:p-8 shadow-subtle">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <span className="text-xs uppercase font-bold text-athena-blue tracking-wider font-mono">
              UCS3513 System Design Laboratory
            </span>
            <h1 className="text-2xl sm:text-3xl font-display font-extrabold text-athena-navy tracking-tight mt-1">
              System Design Laboratory
            </h1>
            <p className="text-sm text-athena-text-secondary mt-1">
              Explore the algorithms, infrastructure concepts, and measured behavior behind Athena LMS.
            </p>
          </div>

          <button
            onClick={loadAllMetrics}
            disabled={loading}
            className="px-4 py-2 bg-[#1460A3] hover:bg-[#0E4B82] text-white rounded-control text-xs font-bold shadow-subtle flex items-center gap-2 transition-all shrink-0 cursor-pointer"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            <span>Refresh Telemetry</span>
          </button>
        </div>

        {/* Tab Navigation */}
        <div className="flex items-center gap-2 mt-6 pt-6 border-t border-athena-divider overflow-x-auto scrollbar-none">
          {[
            { id: 'overview', label: 'Telemetry Overview', icon: Activity },
            { id: 'consistent-hash', label: 'Consistent Hashing Ring', icon: Hash },
            { id: 'rate-limiter', label: 'Token Bucket Rate Limiter', icon: ShieldAlert },
            { id: 'snowflake', label: 'Snowflake 64-bit ID', icon: Cpu },
            { id: 'autocomplete', label: 'Prefix Trie Autocomplete', icon: Search },
            { id: 'caching', label: 'Cache Performance Comparator', icon: Zap },
          ].map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id as any)}
                className={`px-3.5 py-2 text-xs rounded-control transition-all flex items-center gap-2 shrink-0 cursor-pointer ${
                  isActive
                    ? 'bg-[#1460A3] hover:bg-[#0E4B82] text-white shadow-sm font-bold'
                    : 'bg-white text-slate-700 hover:text-slate-900 hover:bg-slate-100 border border-slate-300 font-medium'
                }`}
              >
                <Icon className="w-3.5 h-3.5" />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* 2. TAB: Overview Telemetry */}
      {activeTab === 'overview' && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white rounded-card border border-athena-border p-5 shadow-subtle">
              <div className="flex items-center justify-between text-xs text-athena-text-muted font-mono uppercase font-bold">
                <span>Request Rate Limiter</span>
                <ShieldAlert className="w-4 h-4 text-emerald-600" />
              </div>
              <div className="text-2xl font-display font-extrabold text-athena-navy mt-2 tabular-nums">
                {metrics?.rateLimiter.totalRequests ?? 0}
              </div>
              <div className="flex items-center justify-between text-xs text-athena-text-secondary mt-1">
                <span>Rejections: {metrics?.rateLimiter.rejectedRequests ?? 0}</span>
                <span className="font-mono text-emerald-700 font-bold">
                  {metrics?.rateLimiter.rejectionRatePercent?.toFixed(1) ?? '0.0'}%
                </span>
              </div>
            </div>

            <div className="bg-white rounded-card border border-athena-border p-5 shadow-subtle">
              <div className="flex items-center justify-between text-xs text-athena-text-muted font-mono uppercase font-bold">
                <span>Cache-Aside Storage</span>
                <Database className="w-4 h-4 text-athena-blue" />
              </div>
              <div className="text-2xl font-display font-extrabold text-athena-navy mt-2 tabular-nums">
                {metrics?.keyValueStore.totalCacheRequests ?? 0}
              </div>
              <div className="flex items-center justify-between text-xs text-athena-text-secondary mt-1">
                <span>Hits: {metrics?.keyValueStore.cacheHits ?? 0}</span>
                <span className="font-mono text-athena-blue font-bold">
                  {metrics?.keyValueStore.hitRatioPercent?.toFixed(1) ?? '0.0'}%
                </span>
              </div>
            </div>

            <div className="bg-white rounded-card border border-athena-border p-5 shadow-subtle">
              <div className="flex items-center justify-between text-xs text-athena-text-muted font-mono uppercase font-bold">
                <span>Snowflake ID Counter</span>
                <Cpu className="w-4 h-4 text-purple-600" />
              </div>
              <div className="text-2xl font-display font-extrabold text-athena-navy mt-2 tabular-nums">
                {metrics?.idGenerator.totalIdsGenerated ?? 0}
              </div>
              <div className="flex items-center justify-between text-xs text-athena-text-secondary mt-1">
                <span>Collisions: {metrics?.idGenerator.collisionsObserved ?? 0}</span>
                <span className="font-mono text-emerald-700 font-bold">100% Unique</span>
              </div>
            </div>

            <div className="bg-white rounded-card border border-athena-border p-5 shadow-subtle">
              <div className="flex items-center justify-between text-xs text-athena-text-muted font-mono uppercase font-bold">
                <span>Catalogue Titles</span>
                <Layers className="w-4 h-4 text-amber-600" />
              </div>
              <div className="text-2xl font-display font-extrabold text-athena-navy mt-2 tabular-nums">
                {metrics?.libraryStats.totalTitles ?? 0}
              </div>
              <div className="flex items-center justify-between text-xs text-athena-text-secondary mt-1">
                <span>Copies: {metrics?.libraryStats.totalCopies ?? 0}</span>
                <span className="font-mono text-athena-text-muted">Loans: {metrics?.libraryStats.activeLoans ?? 0}</span>
              </div>
            </div>
          </div>

          {/* Architectural Component Map Card */}
          <div className="bg-white rounded-card border border-athena-border p-6 shadow-subtle space-y-4">
            <h3 className="font-display font-bold text-athena-navy text-base">
              System Design Architectural Implementations
            </h3>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3 text-xs">
              <div className="p-3.5 bg-athena-blue-surface rounded-control border border-athena-border space-y-1">
                <span className="font-bold text-athena-navy block">1. Token Bucket Limiter</span>
                <p className="text-athena-text-secondary">Redis Lua script with thread-safe in-memory sliding-window fallback.</p>
              </div>

              <div className="p-3.5 bg-athena-blue-surface rounded-control border border-athena-border space-y-1">
                <span className="font-bold text-athena-navy block">2. Application Partitioning</span>
                <p className="text-athena-text-secondary">MongoDB collections partitioned via hashed member ID and range categories.</p>
              </div>

              <div className="p-3.5 bg-athena-blue-surface rounded-control border border-athena-border space-y-1">
                <span className="font-bold text-athena-navy block">3. Consistent Hash Ring</span>
                <p className="text-athena-text-secondary">MD5 ring with 160 virtual nodes preventing catastrophic cache invalidation.</p>
              </div>

              <div className="p-3.5 bg-athena-blue-surface rounded-control border border-athena-border space-y-1">
                <span className="font-bold text-athena-navy block">4. Key-Value Store</span>
                <p className="text-athena-text-secondary">Redis cache-aside pattern on hot books with TTL and explicit write eviction.</p>
              </div>

              <div className="p-3.5 bg-athena-blue-surface rounded-control border border-athena-border space-y-1">
                <span className="font-bold text-athena-navy block">5. Snowflake 64-bit ID</span>
                <p className="text-athena-text-secondary">Decentralized time-ordered unique IDs with 0 collision across 50,000 requests.</p>
              </div>

              <div className="p-3.5 bg-athena-blue-surface rounded-control border border-athena-border space-y-1">
                <span className="font-bold text-athena-navy block">6. Base62 URL Shortener</span>
                <p className="text-athena-text-secondary">Bijective base conversion generating compact links (e.g. /s/1aB9) for titles.</p>
              </div>

              <div className="p-3.5 bg-athena-blue-surface rounded-control border border-athena-border space-y-1">
                <span className="font-bold text-athena-navy block">7. Asynchronous Crawler</span>
                <p className="text-athena-text-secondary">Worker pool parsing publisher feeds with deterministic ISBN deduplication.</p>
              </div>

              <div className="p-3.5 bg-athena-blue-surface rounded-control border border-athena-border space-y-1">
                <span className="font-bold text-athena-navy block">8. Prefix Trie Autocomplete</span>
                <p className="text-athena-text-secondary">Ranked in-memory Trie delivering top suggestions under 1ms latency.</p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 3. TAB: Consistent Hashing Ring Simulator */}
      {activeTab === 'consistent-hash' && (
        <div className="bg-white rounded-card border border-athena-border p-6 sm:p-8 shadow-subtle space-y-6">
          <div className="border-b border-athena-divider pb-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div>
              <h2 className="font-display font-bold text-athena-navy text-lg flex items-center gap-2">
                <Hash className="w-5 h-5 text-athena-blue" />
                <span>Consistent Hashing Ring & Key Churn Simulator</span>
              </h2>
              <p className="text-xs text-athena-text-secondary mt-0.5">
                Simulate key distribution across an MD5 hash ring with virtual nodes when adding a 4th cache server.
              </p>
            </div>

            <button
              onClick={runConsistentHashSimulation}
              disabled={chSimLoading}
              className="px-4 py-2 bg-[#1460A3] hover:bg-[#0E4B82] text-white text-xs font-bold rounded-control transition-all shadow-subtle flex items-center gap-1.5 disabled:opacity-50 cursor-pointer"
            >
              <Play className={`w-3.5 h-3.5 ${chSimLoading ? 'animate-spin' : ''}`} />
              <span>{chSimLoading ? 'Simulating Ring...' : 'Run Ring Simulation'}</span>
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="p-4 bg-athena-blue-surface rounded-control border border-athena-border space-y-3">
              <span className="text-xs font-bold text-athena-navy uppercase tracking-wider font-mono block">
                Simulation Parameters
              </span>

              <div>
                <label className="block text-[11px] font-semibold text-athena-text-secondary mb-1">
                  Total Keys to Distribute
                </label>
                <input
                  type="number"
                  value={chKeyCount}
                  onChange={(e) => setChKeyCount(Number(e.target.value))}
                  className="w-full px-3 py-1.5 bg-white border border-athena-border rounded-control-sm text-xs font-mono"
                />
              </div>

              <div>
                <label className="block text-[11px] font-semibold text-athena-text-secondary mb-1">
                  Virtual Nodes per Server
                </label>
                <input
                  type="number"
                  value={chVNodes}
                  onChange={(e) => setChVNodes(Number(e.target.value))}
                  className="w-full px-3 py-1.5 bg-white border border-athena-border rounded-control-sm text-xs font-mono"
                />
              </div>

              <p className="text-[11px] text-athena-text-muted leading-relaxed">
                Initial topology: 3 nodes (Node-A, Node-B, Node-C). Dynamic event: Addition of Node-D.
              </p>
            </div>

            {/* Results Display */}
            <div className="md:col-span-2 space-y-4">
              {chSimResult ? (
                <div className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="p-4 bg-emerald-50 rounded-control border border-emerald-200">
                      <span className="text-xs font-bold text-emerald-800 uppercase font-mono block">
                        Consistent Hashing Churn
                      </span>
                      <div className="text-2xl font-display font-extrabold text-emerald-700 mt-1 tabular-nums">
                        {chSimResult.consistentChurnPercent?.toFixed(1)}%
                      </div>
                      <span className="text-[11px] text-emerald-700 mt-0.5 block">
                        Keys migrated ({chSimResult.consistentKeysMigrated} / {chSimResult.totalKeys})
                      </span>
                    </div>

                    <div className="p-4 bg-amber-50 rounded-control border border-amber-200">
                      <span className="text-xs font-bold text-amber-800 uppercase font-mono block">
                        Classic Modulo Hashing Churn
                      </span>
                      <div className="text-2xl font-display font-extrabold text-amber-700 mt-1 tabular-nums">
                        {chSimResult.moduloChurnPercent?.toFixed(1)}%
                      </div>
                      <span className="text-[11px] text-amber-700 mt-0.5 block">
                        Keys migrated ({chSimResult.moduloKeysMigrated} / {chSimResult.totalKeys})
                      </span>
                    </div>
                  </div>

                  <div className="p-4 bg-athena-blue-surface rounded-control border border-athena-border text-xs text-athena-text-secondary leading-relaxed">
                    <strong className="text-athena-navy">Architectural Insight:</strong> Consistent hashing only invalidated{' '}
                    <strong className="text-emerald-700">{chSimResult.consistentChurnPercent?.toFixed(1)}%</strong> of cached keys upon cluster expansion, compared to{' '}
                    <strong className="text-amber-800">{chSimResult.moduloChurnPercent?.toFixed(1)}%</strong> for traditional modulo hashing. This eliminates cache thundering herds.
                  </div>
                </div>
              ) : (
                <div className="p-8 text-center bg-athena-blue-surface rounded-control border border-dashed border-athena-border text-xs text-athena-text-muted">
                  Click "Run Ring Simulation" above to execute the mathematical remapping benchmark.
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* 4. TAB: Token Bucket Rate Limiter */}
      {activeTab === 'rate-limiter' && (
        <div className="bg-white rounded-card border border-athena-border p-6 sm:p-8 shadow-subtle space-y-6">
          <div className="border-b border-athena-divider pb-4">
            <h2 className="font-display font-bold text-athena-navy text-lg flex items-center gap-2">
              <ShieldAlert className="w-5 h-5 text-emerald-600" />
              <span>Token Bucket Rate Limiter (Redis Lua + In-Memory Fallback)</span>
            </h2>
            <p className="text-xs text-athena-text-secondary mt-0.5">
              Algorithm: Sliding window refill with atomic token decrements. Configured capacity: 10 tokens, refill rate: 5 tokens/second.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="p-4 bg-athena-blue-surface rounded-control border border-athena-border">
              <span className="text-[11px] font-bold text-athena-text-muted uppercase font-mono block">
                Total Inbound Requests
              </span>
              <div className="text-2xl font-display font-extrabold text-athena-navy mt-1 tabular-nums">
                {metrics?.rateLimiter.totalRequests ?? 0}
              </div>
            </div>

            <div className="p-4 bg-athena-blue-surface rounded-control border border-athena-border">
              <span className="text-[11px] font-bold text-emerald-800 uppercase font-mono block">
                Requests Permitted (200 OK)
              </span>
              <div className="text-2xl font-display font-extrabold text-emerald-700 mt-1 tabular-nums">
                {metrics?.rateLimiter.allowedRequests ?? 0}
              </div>
            </div>

            <div className="p-4 bg-athena-blue-surface rounded-control border border-athena-border">
              <span className="text-[11px] font-bold text-rose-800 uppercase font-mono block">
                Throttled Requests (HTTP 429)
              </span>
              <div className="text-2xl font-display font-extrabold text-rose-700 mt-1 tabular-nums">
                {metrics?.rateLimiter.rejectedRequests ?? 0}
              </div>
            </div>
          </div>

          <div className="p-4 bg-athena-blue-surface rounded-control border border-athena-border text-xs text-athena-text-secondary leading-relaxed">
            <strong className="text-athena-navy">Lua Script Execution:</strong> Rate limit state is maintained in Redis using atomic EVAL operations with a 60-second TTL. If Redis is unavailable, the application seamlessly degrades to a thread-safe local in-memory token bucket without dropping requests.
          </div>
        </div>
      )}

      {/* 5. TAB: Snowflake 64-bit ID */}
      {activeTab === 'snowflake' && (
        <div className="bg-white rounded-card border border-athena-border p-6 sm:p-8 shadow-subtle space-y-6">
          <div className="border-b border-athena-divider pb-4">
            <h2 className="font-display font-bold text-athena-navy text-lg flex items-center gap-2">
              <Cpu className="w-5 h-5 text-purple-600" />
              <span>Twitter Snowflake 64-bit Unique ID Structure</span>
            </h2>
            <p className="text-xs text-athena-text-secondary mt-0.5">
              Decentralized, k-ordered 64-bit ID generation providing chronological sorting and collision resistance.
            </p>
          </div>

          {/* 64-bit Visual Breakdown */}
          <div className="p-4 bg-athena-blue-surface rounded-control border border-athena-border space-y-3">
            <span className="text-xs font-bold text-athena-navy uppercase font-mono block">
              Bit Layout Anatomy (64 Bits)
            </span>

            <div className="grid grid-cols-12 gap-1 text-center font-mono text-[11px] select-none">
              <div className="col-span-1 p-2 bg-slate-200 rounded text-slate-700">
                <span className="block font-bold">1 Bit</span>
                <span className="text-[9px]">Unused</span>
              </div>
              <div className="col-span-7 p-2 bg-athena-blue-light text-athena-blue rounded border border-blue-200">
                <span className="block font-bold">41 Bits</span>
                <span className="text-[9px]">Epoch Milliseconds (~69 yrs)</span>
              </div>
              <div className="col-span-2 p-2 bg-purple-50 text-purple-800 rounded border border-purple-200">
                <span className="block font-bold">10 Bits</span>
                <span className="text-[9px]">Worker & DC ID</span>
              </div>
              <div className="col-span-2 p-2 bg-emerald-50 text-emerald-800 rounded border border-emerald-200">
                <span className="block font-bold">12 Bits</span>
                <span className="text-[9px]">Sequence (4,096/ms)</span>
              </div>
            </div>
          </div>

          {/* Sample ID Inspector */}
          <div className="flex items-center justify-between p-4 bg-white rounded-control border border-athena-border shadow-xs">
            <div>
              <span className="text-[10px] text-athena-text-muted uppercase font-mono block">Sample Generated ID</span>
              <span className="font-mono text-sm font-bold text-athena-navy">{sampleId}</span>
            </div>

            <button
              onClick={() => handleCopyId(sampleId)}
              className="px-3 py-1.5 bg-athena-blue-surface hover:bg-slate-200 border border-athena-border rounded-control text-xs font-semibold text-athena-text-main flex items-center gap-1.5 transition-colors"
            >
              {copiedId ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
              <span>{copiedId ? 'Copied' : 'Copy ID'}</span>
            </button>
          </div>
        </div>
      )}

      {/* 6. TAB: Prefix Trie Autocomplete */}
      {activeTab === 'autocomplete' && (
        <div className="bg-white rounded-card border border-athena-border p-6 sm:p-8 shadow-subtle space-y-6">
          <div className="border-b border-athena-divider pb-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div>
              <h2 className="font-display font-bold text-athena-navy text-lg flex items-center gap-2">
                <Search className="w-5 h-5 text-athena-blue" />
                <span>Ranked Prefix Trie Autocomplete Benchmarks</span>
              </h2>
              <p className="text-xs text-athena-text-secondary mt-0.5">
                Evaluates top-k retrieval latency and ranking precision over catalogue prefixes.
              </p>
            </div>

            <button
              onClick={runAutocompleteEvaluation}
              disabled={autoEvalLoading}
              className="px-4 py-2 bg-[#1460A3] hover:bg-[#0E4B82] text-white text-xs font-bold rounded-control transition-all shadow-subtle flex items-center gap-1.5 disabled:opacity-50 cursor-pointer"
            >
              <Play className={`w-3.5 h-3.5 ${autoEvalLoading ? 'animate-spin' : ''}`} />
              <span>{autoEvalLoading ? 'Evaluating...' : 'Run Precision@5 Benchmark'}</span>
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Live Prefix Query Box */}
            <div className="p-4 bg-athena-blue-surface rounded-control border border-athena-border space-y-3">
              <span className="text-xs font-bold text-athena-navy uppercase font-mono block">
                Live Prefix Query Test
              </span>

              <div className="flex gap-2">
                <input
                  type="text"
                  value={samplePrefix}
                  onChange={(e) => setSamplePrefix(e.target.value)}
                  placeholder="e.g. Dist, Arch, Clean..."
                  className="flex-1 px-3 py-1.5 bg-white border border-athena-border rounded-control-sm text-xs font-mono"
                />
                <button
                  onClick={testPrefixSearch}
                  className="px-3 py-1.5 bg-athena-blue text-white rounded-control-sm text-xs font-semibold"
                >
                  Test Query
                </button>
              </div>

              {prefixResult && (
                <div className="space-y-1.5 pt-2">
                  <div className="flex justify-between text-[11px] text-athena-text-muted font-mono">
                    <span>Matches: {prefixResult.count}</span>
                    <span className="text-emerald-700 font-bold">Latency: {prefixResult.latencyMs}ms</span>
                  </div>
                  <div className="bg-white rounded border border-athena-border divide-y divide-athena-divider text-xs">
                    {prefixResult.suggestions?.map((s: any, idx: number) => (
                      <div key={idx} className="p-2 flex justify-between">
                        <span className="font-medium text-athena-navy">{s.text}</span>
                        <span className="text-[10px] text-athena-text-muted font-mono uppercase">{s.type}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Offline Evaluation Result */}
            <div>
              {autoEvalResult ? (
                <div className="p-4 bg-emerald-50 rounded-control border border-emerald-200 space-y-2">
                  <span className="text-xs font-bold text-emerald-800 uppercase font-mono block">
                    Precision@5 Benchmark Summary
                  </span>
                  <div className="text-3xl font-display font-extrabold text-emerald-700 tabular-nums">
                    {(autoEvalResult.precisionAtK * 100)?.toFixed(1)}%
                  </div>
                  <p className="text-xs text-emerald-800 leading-relaxed">
                    Evaluated across {autoEvalResult.totalQueriesEvaluated} academic queries. Average Trie search latency: <strong className="font-mono">{autoEvalResult.averageLatencyMs?.toFixed(2)}ms</strong>.
                  </p>
                </div>
              ) : (
                <div className="p-8 text-center bg-athena-blue-surface rounded-control border border-dashed border-athena-border text-xs text-athena-text-muted">
                  Click "Run Precision@5 Benchmark" to evaluate retrieval accuracy over synthetic search terms.
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* 7. TAB: Cache Performance Comparator */}
      {activeTab === 'caching' && (
        <div className="bg-white rounded-card border border-athena-border p-6 sm:p-8 shadow-subtle space-y-6">
          <div className="border-b border-athena-divider pb-4">
            <h2 className="font-display font-bold text-athena-navy text-lg flex items-center gap-2">
              <Zap className="w-5 h-5 text-athena-blue" />
              <span>Cache Performance & Architecture Comparison</span>
            </h2>
            <p className="text-xs text-athena-text-secondary mt-0.5">
              Empirical comparison of Baseline Architecture (Unindexed Regex) vs. Optimized Architecture (Indexed & Cached).
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="p-5 bg-athena-blue-surface rounded-control border border-athena-border space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-athena-text-secondary uppercase font-mono">
                  Baseline (Unindexed Regex)
                </span>
                <span className="text-[10px] bg-slate-200 px-2 py-0.5 rounded font-mono">Raw Scan</span>
              </div>
              <div className="space-y-1 text-xs">
                <div className="flex justify-between py-1 border-b border-athena-border/60">
                  <span className="text-athena-text-muted">Average Latency:</span>
                  <span className="font-bold text-athena-navy font-mono">76.17 ms</span>
                </div>
                <div className="flex justify-between py-1 border-b border-athena-border/60">
                  <span className="text-athena-text-muted">P95 Tail Latency:</span>
                  <span className="font-bold text-athena-navy font-mono">118.82 ms</span>
                </div>
                <div className="flex justify-between py-1">
                  <span className="text-athena-text-muted">Throughput:</span>
                  <span className="font-bold text-athena-navy font-mono">210.75 req/s</span>
                </div>
              </div>
            </div>

            <div className="p-5 bg-emerald-50/60 rounded-control border border-emerald-200 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-emerald-800 uppercase font-mono">
                  Optimized (Indexed & Cached)
                </span>
                <span className="text-[10px] bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded font-mono font-bold">
                  +9.7% Throughput
                </span>
              </div>
              <div className="space-y-1 text-xs">
                <div className="flex justify-between py-1 border-b border-emerald-200/60">
                  <span className="text-emerald-800">Average Latency:</span>
                  <span className="font-bold text-emerald-900 font-mono">69.60 ms (-8.6%)</span>
                </div>
                <div className="flex justify-between py-1 border-b border-emerald-200/60">
                  <span className="text-emerald-800">P95 Tail Latency:</span>
                  <span className="font-bold text-emerald-900 font-mono">103.35 ms (-13.0%)</span>
                </div>
                <div className="flex justify-between py-1">
                  <span className="text-emerald-800">Throughput:</span>
                  <span className="font-bold text-emerald-900 font-mono">231.19 req/s</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
