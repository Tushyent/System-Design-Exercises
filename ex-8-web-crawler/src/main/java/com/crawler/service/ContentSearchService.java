package com.crawler.service;

import com.crawler.dto.MatchOccurrenceDetail;
import com.crawler.dto.SearchResponse;
import com.crawler.dto.SearchResult;
import com.crawler.model.PageMetadata;
import com.crawler.util.PorterStemmer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Advanced Information Retrieval & Content Discovery Indexing Engine.
 * Implements Okapi BM25 ranking, Google PageRank integration, multi-field inverted indexing,
 * stemming, phrase matching, vocabulary autocomplete, corpus-wide topic discovery,
 * and exact line & location breakdown (Content-Title, Content Line Numbers, Metadata).
 */
@Slf4j
@Service
public class ContentSearchService {

    private static final double BM25_K1 = 1.2;
    private static final double BM25_B = 0.75;

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't",
            "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by",
            "can", "can't", "cannot", "could", "couldn't", "did", "didn't", "do", "does", "doesn't", "doing",
            "don't", "down", "during", "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't",
            "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here", "here's", "hers", "herself",
            "him", "himself", "his", "how", "how's", "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is",
            "isn't", "it", "it's", "its", "itself", "let's", "me", "more", "most", "mustn't", "my", "myself",
            "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours",
            "ourselves", "out", "over", "own", "same", "shan't", "she", "she'd", "she'll", "she's", "should",
            "shouldn't", "so", "some", "such", "than", "that", "that's", "the", "their", "theirs", "them",
            "themselves", "then", "there", "there's", "these", "they", "they'd", "they'll", "they're", "they've",
            "this", "those", "through", "to", "too", "under", "until", "up", "very", "was", "wasn't", "we", "we'd",
            "we'll", "we're", "we've", "were", "weren't", "what", "what's", "when", "when's", "where", "where's",
            "which", "while", "who", "who's", "whom", "why", "why's", "with", "won't", "would", "wouldn't",
            "you", "you'd", "you'll", "you're", "you've", "your", "yours", "yourself", "yourselves"
    ));

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Posting {
        private String url;
        private int termFrequency;
        @Builder.Default
        private List<Integer> positions = new ArrayList<>();
        private boolean inTitle;
        private boolean inHeading;
        private boolean inMeta;
    }

    private final PageRankService pageRankService;

    // Lexicon: term -> (url -> Posting)
    private final Map<String, Map<String, Posting>> invertedIndex = new ConcurrentHashMap<>();
    // Stemmed Lexicon: stem -> set of actual terms
    private final Map<String, Set<String>> stemToTerms = new ConcurrentHashMap<>();
    // Document Store: url -> PageMetadata
    private final Map<String, PageMetadata> documentStore = new ConcurrentHashMap<>();
    // Document token count (length) for BM25 normalization
    private final Map<String, Integer> docLengths = new ConcurrentHashMap<>();
    // Corpus vocabulary frequency count: term -> total occurrences across all documents
    private final Map<String, Integer> termFrequencies = new ConcurrentHashMap<>();

    public ContentSearchService(PageRankService pageRankService) {
        this.pageRankService = pageRankService;
    }

    /**
     * Indexes a crawled page into the multi-field inverted search index.
     */
    public void indexPage(PageMetadata page) {
        if (page == null || page.getUrl() == null) return;
        String url = page.getUrl();
        documentStore.put(url, page);

        // Auto-classify category
        String category = classifyCategory(page);
        page.setCategory(category);

        List<String> bodyTokens = tokenize(page.getCleanText());
        docLengths.put(url, Math.max(1, bodyTokens.size()));

        // Index body tokens with positions
        for (int i = 0; i < bodyTokens.size(); i++) {
            String token = bodyTokens.get(i);
            recordPosting(token, url, i, false, false, false);
        }

        // Index title tokens
        List<String> titleTokens = tokenize(page.getTitle());
        for (String token : titleTokens) {
            recordPosting(token, url, -1, true, false, false);
        }

        // Index headings
        if (page.getHeadings() != null) {
            for (String h : page.getHeadings()) {
                for (String token : tokenize(h)) {
                    recordPosting(token, url, -1, false, true, false);
                }
            }
        }

        // Index meta keywords and description
        String metaText = (page.getMetaKeywords() != null ? page.getMetaKeywords() : "") + " " +
                (page.getMetaDescription() != null ? page.getMetaDescription() : "");
        for (String token : tokenize(metaText)) {
            recordPosting(token, url, -1, false, false, true);
        }
    }

    private void recordPosting(String token, String url, int pos, boolean isTitle, boolean isHeading, boolean isMeta) {
        if (token.length() < 2 || STOP_WORDS.contains(token)) return;

        // Record vocabulary frequency
        termFrequencies.merge(token, 1, Integer::sum);

        // Record stem mapping
        String stem = PorterStemmer.stemWord(token);
        stemToTerms.computeIfAbsent(stem, k -> ConcurrentHashMap.newKeySet()).add(token);

        invertedIndex.compute(token, (k, postMap) -> {
            if (postMap == null) postMap = new ConcurrentHashMap<>();
            Posting p = postMap.computeIfAbsent(url, u -> Posting.builder().url(u).termFrequency(0).build());
            p.setTermFrequency(p.getTermFrequency() + 1);
            if (pos >= 0) p.getPositions().add(pos);
            if (isTitle) p.setInTitle(true);
            if (isHeading) p.setInHeading(true);
            if (isMeta) p.setInMeta(true);
            return postMap;
        });
    }

    /**
     * Executes advanced content discovery search. Supports sorting by relevance, PageRank, or BM25.
     * Evaluates exact locations (Content-Title, Content Line Numbers, Metadata) for every hit.
     */
    public SearchResponse search(String query, String sortBy, String filterCategory) {
        long start = System.currentTimeMillis();
        if (query == null || query.trim().isEmpty()) {
            return SearchResponse.builder()
                    .query("")
                    .totalMatches(0)
                    .searchTimeMs(0)
                    .results(Collections.emptyList())
                    .build();
        }

        String sortMode = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy.trim().toUpperCase() : "COMPOSITE";

        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) {
            return SearchResponse.builder()
                    .query(query)
                    .totalMatches(0)
                    .searchTimeMs(System.currentTimeMillis() - start)
                    .rankingMode(sortMode)
                    .results(Collections.emptyList())
                    .build();
        }

        // Expand query terms via stemming
        Set<String> expandedTerms = new LinkedHashSet<>();
        for (String q : queryTokens) {
            expandedTerms.add(q);
            String stem = PorterStemmer.stemWord(q);
            Set<String> variants = stemToTerms.get(stem);
            if (variants != null) {
                expandedTerms.addAll(variants);
            }
        }

        int N = Math.max(1, documentStore.size());
        double avgdl = docLengths.values().stream().mapToInt(Integer::intValue).average().orElse(100.0);

        Map<String, Double> bm25Scores = new HashMap<>();
        Map<String, Double> compositeScores = new HashMap<>();
        Map<String, Set<String>> matchedTermsMap = new HashMap<>();
        Map<String, Integer> matchOccurrencesMap = new HashMap<>();
        int corpusOccurrences = 0;

        for (String term : expandedTerms) {
            Map<String, Posting> postings = invertedIndex.get(term);
            if (postings != null) {
                int df = postings.size();
                // BM25 IDF with smoothing
                double idf = Math.log(1.0 + (N - df + 0.5) / (df + 0.5));
                if (idf < 0.1) idf = 0.1;

                for (Map.Entry<String, Posting> entry : postings.entrySet()) {
                    String url = entry.getKey();
                    Posting p = entry.getValue();

                    int tf = p.getTermFrequency();
                    corpusOccurrences += tf;
                    matchOccurrencesMap.merge(url, tf, Integer::sum);
                    matchedTermsMap.computeIfAbsent(url, k -> new HashSet<>()).add(term);

                    int docLen = docLengths.getOrDefault(url, (int) avgdl);
                    double tfComponent = (tf * (BM25_K1 + 1.0)) / (tf + BM25_K1 * (1.0 - BM25_B + BM25_B * (docLen / avgdl)));
                    double termBm25 = idf * tfComponent;

                    // Field boosts
                    double fieldBoost = 0.0;
                    if (p.isInTitle()) fieldBoost += 5.0 * idf;
                    if (p.isInHeading()) fieldBoost += 3.0 * idf;
                    if (p.isInMeta()) fieldBoost += 2.0 * idf;

                    bm25Scores.merge(url, termBm25, Double::sum);
                    compositeScores.merge(url, termBm25 + fieldBoost, Double::sum);
                }
            }
        }

        // Incorporate PageRank scores
        Map<String, Integer> categoryDistribution = new HashMap<>();
        Set<String> coOccurringKeywords = new LinkedHashSet<>();

        List<SearchResult> results = new ArrayList<>();
        for (String url : compositeScores.keySet()) {
            PageMetadata pm = documentStore.get(url);
            if (pm == null) continue;

            // Apply category filter if specified
            if (filterCategory != null && !filterCategory.trim().isEmpty() && !"ALL".equalsIgnoreCase(filterCategory)) {
                if (!filterCategory.equalsIgnoreCase(pm.getCategory())) {
                    continue;
                }
            }

            double bm25 = Math.round(bm25Scores.getOrDefault(url, 0.0) * 100.0) / 100.0;
            double pageRank = pageRankService.getPageRank(url);
            pm.setPageRankScore(pageRank);

            // Composite formula: BM25 * (1 + 0.05 * PageRank) + bonuses
            double baseComp = compositeScores.getOrDefault(url, 0.0);
            double totalScore = Math.round((baseComp * (1.0 + (pageRank / 20.0))) * 100.0) / 100.0;

            int occurrences = matchOccurrencesMap.getOrDefault(url, 0);
            String cat = pm.getCategory() != null ? pm.getCategory() : "General";

            categoryDistribution.merge(cat, 1, Integer::sum);
            if (pm.getTopKeywords() != null) {
                coOccurringKeywords.addAll(pm.getTopKeywords());
            }

            String breakdown = String.format("BM25: %.2f | PageRank: %.1f | Field Boost: %.2f | Occurrences: %d",
                    bm25, pageRank, Math.max(0.0, baseComp - bm25), occurrences);

            // Extract exact occurrences (Content-Title, Content Lines, Metadata)
            List<MatchOccurrenceDetail> matchDetails = extractMatchOccurrences(pm, queryTokens, expandedTerms);

            boolean inTitle = matchDetails.stream().anyMatch(d -> "content-title".equalsIgnoreCase(d.getSection()));
            boolean inContent = matchDetails.stream().anyMatch(d -> "content".equalsIgnoreCase(d.getSection()));
            boolean inMetadata = matchDetails.stream().anyMatch(d -> "metadata".equalsIgnoreCase(d.getSection()));

            List<String> locTags = new ArrayList<>();
            if (inTitle) locTags.add("Content-Title");

            List<Integer> contentLines = matchDetails.stream()
                    .filter(d -> "content".equalsIgnoreCase(d.getSection()) && d.getLineNumber() > 0)
                    .map(MatchOccurrenceDetail::getLineNumber)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            if (!contentLines.isEmpty()) {
                if (contentLines.size() <= 4) {
                    locTags.add("Content (Line " + contentLines.stream().map(String::valueOf).collect(Collectors.joining(", ")) + ")");
                } else {
                    locTags.add("Content (Lines " + contentLines.get(0) + "-" + contentLines.get(contentLines.size() - 1) + ")");
                }
            } else if (inContent) {
                locTags.add("Content (Body)");
            }

            if (inMetadata) {
                List<String> metaLabels = matchDetails.stream()
                        .filter(d -> "metadata".equalsIgnoreCase(d.getSection()))
                        .map(MatchOccurrenceDetail::getLocationLabel)
                        .distinct()
                        .collect(Collectors.toList());
                locTags.addAll(metaLabels);
            }

            String titleSnippet = highlightText(pm.getTitle(), expandedTerms);
            String primarySnippet = generateSnippet(pm.getCleanText(), queryTokens);

            // If we have content match, make sure snippet is relevant
            if (!matchDetails.isEmpty()) {
                Optional<MatchOccurrenceDetail> firstContent = matchDetails.stream()
                        .filter(d -> "content".equalsIgnoreCase(d.getSection()))
                        .findFirst();
                if (firstContent.isPresent()) {
                    primarySnippet = firstContent.get().getSnippet();
                }
            }

            String linesSummary = String.format("Title: %s | Content: %s | Metadata: %s",
                    inTitle ? "Matched" : "None",
                    contentLines.isEmpty() ? (inContent ? "Detected" : "None") : "Lines " + contentLines.stream().map(String::valueOf).collect(Collectors.joining(", ")),
                    inMetadata ? "Detected" : "None");

            results.add(SearchResult.builder()
                    .url(url)
                    .title(pm.getTitle())
                    .titleSnippet(titleSnippet)
                    .snippet(primarySnippet)
                    .metaDescription(pm.getMetaDescription())
                    .relevanceScore(totalScore)
                    .bm25Score(bm25)
                    .pageRankScore(pageRank)
                    .category(cat)
                    .matchOccurrences(occurrences)
                    .scoreBreakdown(breakdown)
                    .wordCount(pm.getWordCount())
                    .readingTimeMinutes(pm.getReadingTimeMinutes())
                    .depth(pm.getDepth())
                    .matchedTerms(new ArrayList<>(matchedTermsMap.getOrDefault(url, Collections.emptySet())))
                    .crawledAt(pm.getCrawledAt())
                    .matchedInTitle(inTitle)
                    .matchedInContent(inContent)
                    .matchedInMetadata(inMetadata)
                    .locationTags(locTags)
                    .occurrences(matchDetails)
                    .matchedLinesSummary(linesSummary)
                    .build());
        }

        // Apply chosen ranking strategy
        if ("PAGERANK".equalsIgnoreCase(sortMode)) {
            results.sort(Comparator.comparingDouble(SearchResult::getPageRankScore).reversed()
                    .thenComparing(Comparator.comparingDouble(SearchResult::getRelevanceScore).reversed()));
        } else if ("BM25".equalsIgnoreCase(sortMode)) {
            results.sort(Comparator.comparingDouble(SearchResult::getBm25Score).reversed());
        } else if ("WORDS".equalsIgnoreCase(sortMode)) {
            results.sort(Comparator.comparingInt(SearchResult::getWordCount).reversed());
        } else if ("NEWEST".equalsIgnoreCase(sortMode)) {
            results.sort(Comparator.comparingLong(SearchResult::getCrawledAt).reversed());
        } else {
            // Default: Composite Score
            results.sort(Comparator.comparingDouble(SearchResult::getRelevanceScore).reversed());
        }

        // Assign 1-based ranks
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRank(i + 1);
        }

        // Filter out search terms from related keywords
        coOccurringKeywords.removeAll(queryTokens);
        List<String> topRelated = coOccurringKeywords.stream().limit(8).collect(Collectors.toList());

        long elapsed = System.currentTimeMillis() - start;
        return SearchResponse.builder()
                .query(query)
                .totalMatches(results.size())
                .searchTimeMs(elapsed)
                .rankingMode(sortMode)
                .totalOccurrencesInCorpus(corpusOccurrences)
                .categoryDistribution(categoryDistribution)
                .relatedKeywords(topRelated)
                .results(results)
                .build();
    }

    /**
     * Extracts structured match occurrences across Content-Title, Content Lines, and Metadata.
     */
    private List<MatchOccurrenceDetail> extractMatchOccurrences(PageMetadata pm, List<String> queryTokens, Set<String> expandedTerms) {
        List<MatchOccurrenceDetail> details = new ArrayList<>();
        if (pm == null) return details;

        // 1. Content-Title
        if (pm.getTitle() != null && !pm.getTitle().trim().isEmpty()) {
            int titleMatches = countMatches(pm.getTitle(), expandedTerms);
            if (titleMatches > 0) {
                details.add(MatchOccurrenceDetail.builder()
                        .locationType("CONTENT_TITLE")
                        .locationLabel("Content-Title")
                        .section("content-title")
                        .lineNumber(1)
                        .snippet(highlightText(pm.getTitle(), expandedTerms))
                        .rawText(pm.getTitle())
                        .matchCount(titleMatches)
                        .build());
            }
        }

        // 2. Metadata: Meta Description
        if (pm.getMetaDescription() != null && !pm.getMetaDescription().trim().isEmpty()) {
            int metaMatches = countMatches(pm.getMetaDescription(), expandedTerms);
            if (metaMatches > 0) {
                details.add(MatchOccurrenceDetail.builder()
                        .locationType("METADATA_DESC")
                        .locationLabel("Metadata (Description)")
                        .section("metadata")
                        .lineNumber(-1)
                        .snippet(highlightText(pm.getMetaDescription(), expandedTerms))
                        .rawText(pm.getMetaDescription())
                        .matchCount(metaMatches)
                        .build());
            }
        }

        // 3. Metadata: Meta Keywords
        if (pm.getMetaKeywords() != null && !pm.getMetaKeywords().trim().isEmpty()) {
            int kwMatches = countMatches(pm.getMetaKeywords(), expandedTerms);
            if (kwMatches > 0) {
                details.add(MatchOccurrenceDetail.builder()
                        .locationType("METADATA_KEYWORDS")
                        .locationLabel("Metadata (Keywords)")
                        .section("metadata")
                        .lineNumber(-1)
                        .snippet(highlightText(pm.getMetaKeywords(), expandedTerms))
                        .rawText(pm.getMetaKeywords())
                        .matchCount(kwMatches)
                        .build());
            }
        }

        // 4. Metadata: Headings
        if (pm.getHeadings() != null) {
            for (String h : pm.getHeadings()) {
                if (h != null && !h.trim().isEmpty()) {
                    int hMatches = countMatches(h, expandedTerms);
                    if (hMatches > 0) {
                        details.add(MatchOccurrenceDetail.builder()
                                .locationType("HEADING")
                                .locationLabel("Metadata (Heading)")
                                .section("metadata")
                                .lineNumber(-1)
                                .snippet(highlightText(h, expandedTerms))
                                .rawText(h)
                                .matchCount(hMatches)
                                .build());
                    }
                }
            }
        }

        // 5. Content Body (with explicit line numbers)
        List<String> contentLines = extractContentLines(pm);
        int lineNum = 1;
        int contentAdded = 0;
        for (String line : contentLines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                int lineMatches = countMatches(trimmed, expandedTerms);
                if (lineMatches > 0) {
                    details.add(MatchOccurrenceDetail.builder()
                            .locationType("CONTENT_BODY")
                            .locationLabel("Content (Line " + lineNum + ")")
                            .section("content")
                            .lineNumber(lineNum)
                            .snippet(highlightText(trimmed, expandedTerms))
                            .rawText(trimmed)
                            .matchCount(lineMatches)
                            .build());
                    contentAdded++;
                    if (contentAdded >= 8) {
                        break;
                    }
                }
            }
            lineNum++;
        }

        return details;
    }

    /**
     * Extracts lines / paragraphs of body text from crawled page.
     */
    private List<String> extractContentLines(PageMetadata pm) {
        List<String> lines = new ArrayList<>();
        if (pm.getRawHtml() != null && !pm.getRawHtml().trim().isEmpty()) {
            try {
                Document doc = Jsoup.parse(pm.getRawHtml());
                doc.select("nav, header, footer, script, style, aside, noscript, svg, form").remove();
                Elements blocks = doc.select("h1, h2, h3, h4, h5, h6, p, li, blockquote, dt, dd, div.content, section, article");
                for (Element block : blocks) {
                    String txt = block.text().trim();
                    if (!txt.isEmpty()) {
                        lines.add(txt);
                    }
                }
            } catch (Exception e) {
                log.debug("Error parsing HTML blocks: {}", e.getMessage());
            }
        }
        if (lines.isEmpty() && pm.getCleanText() != null && !pm.getCleanText().trim().isEmpty()) {
            String[] split = pm.getCleanText().split("(?<=[.!?\\n])\\s+");
            for (String s : split) {
                if (!s.trim().isEmpty()) {
                    lines.add(s.trim());
                }
            }
        }
        return lines;
    }

    private int countMatches(String text, Set<String> terms) {
        if (text == null || terms == null || terms.isEmpty()) return 0;
        int count = 0;
        for (String term : terms) {
            Pattern p = Pattern.compile("(?i)\\b" + Pattern.quote(term) + "[a-z]*\\b");
            Matcher m = p.matcher(text);
            while (m.find()) {
                count++;
            }
        }
        return count;
    }

    private String highlightText(String text, Set<String> terms) {
        if (text == null) return "";
        String result = text;
        for (String term : terms) {
            Pattern p = Pattern.compile("(?i)\\b(" + Pattern.quote(term) + "[a-z]*)\\b");
            Matcher m = p.matcher(result);
            result = m.replaceAll("<mark>$1</mark>");
        }
        return result;
    }

    public SearchResponse search(String query) {
        return search(query, "COMPOSITE", null);
    }

    /**
     * Autocomplete suggestion generator: returns indexed lexicon words matching a prefix.
     */
    public List<Map<String, Object>> getAutocompleteSuggestions(String prefix, int limit) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String cleanPrefix = prefix.toLowerCase().trim();
        return termFrequencies.entrySet().stream()
                .filter(e -> e.getKey().startsWith(cleanPrefix) || e.getKey().contains(cleanPrefix))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit > 0 ? limit : 8)
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("term", e.getKey());
                    map.put("frequency", e.getValue());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private String classifyCategory(PageMetadata page) {
        String text = (page.getTitle() + " " + page.getCleanText() + " " +
                (page.getMetaKeywords() != null ? page.getMetaKeywords() : "")).toLowerCase();

        if (text.contains("quantum") || text.contains("qubit") || text.contains("entanglement") || text.contains("physics")) {
            return "Quantum Computing";
        } else if (text.contains("intelligence") || text.contains("neural") || text.contains("learning") || text.contains("ai") || text.contains("algorithm")) {
            return "Artificial Intelligence";
        } else if (text.contains("cloud") || text.contains("docker") || text.contains("devops") || text.contains("container") || text.contains("redis")) {
            return "Cloud & Infrastructure";
        } else if (text.contains("football") || text.contains("cricket") || text.contains("sports") || text.contains("championship") || text.contains("trophy")) {
            return "Sports & Athletics";
        } else if (text.contains("science") || text.contains("research") || text.contains("study") || text.contains("laboratory")) {
            return "Science & Research";
        } else if (text.contains("news") || text.contains("headline") || text.contains("world") || text.contains("global")) {
            return "World News";
        }
        return "General Web";
    }

    private String generateSnippet(String text, List<String> queryTokens) {
        if (text == null || text.trim().isEmpty()) {
            return "(No content text preview available)";
        }
        String clean = text.replaceAll("\\s+", " ").trim();
        int snippetStart = 0;
        int snippetEnd = Math.min(clean.length(), 220);

        for (String token : queryTokens) {
            int idx = clean.toLowerCase().indexOf(token.toLowerCase());
            if (idx != -1) {
                snippetStart = Math.max(0, idx - 50);
                snippetEnd = Math.min(clean.length(), idx + 160);
                break;
            }
        }

        String excerpt = clean.substring(snippetStart, snippetEnd);
        if (snippetStart > 0) excerpt = "..." + excerpt;
        if (snippetEnd < clean.length()) excerpt = excerpt + "...";

        for (String token : queryTokens) {
            Pattern p = Pattern.compile("(?i)\\b(" + Pattern.quote(token) + "[a-z]*)\\b");
            Matcher m = p.matcher(excerpt);
            excerpt = m.replaceAll("<mark>$1</mark>");
        }

        return excerpt;
    }

    public List<String> tokenize(String input) {
        if (input == null) return Collections.emptyList();
        String[] words = input.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").split("\\s+");
        List<String> valid = new ArrayList<>();
        for (String w : words) {
            String trimmed = w.trim();
            if (trimmed.length() >= 2 && !STOP_WORDS.contains(trimmed)) {
                valid.add(trimmed);
            }
        }
        return valid;
    }

    public void clear() {
        invertedIndex.clear();
        stemToTerms.clear();
        documentStore.clear();
        docLengths.clear();
        termFrequencies.clear();
    }

    public int getIndexedDocumentCount() {
        return documentStore.size();
    }

    public int getIndexedVocabularySize() {
        return invertedIndex.size();
    }
}
