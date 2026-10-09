// package com.examly.springapp.service;

// import com.examly.springapp.model.Course;
// import com.examly.springapp.repository.CourseRepo;
// import java.util.ArrayList;
// import java.util.Comparator;
// import java.util.Collections;
// import java.util.HashSet;
// import java.util.LinkedHashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.Set;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.concurrent.atomic.AtomicInteger;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;

// /**
//  * AI course search: Gemini embeddings with a lexical fallback.
//  *
//  * @author Sumit
//  */
// @Service
// public class CourseAiService {
//     private static final Logger log = LoggerFactory.getLogger(CourseAiService.class);
//     private static final int MIN_RESULTS = 3;

//     private record CacheEntry(int hash, float[] vector) {}
//     private record Scored(Course course, double score) {}

//     private final CourseRepo courseRepo;
//     private final GeminiService gemini;
//     private final double threshold;
//     private final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();

//     // recent query embeddings, so repeating a search costs no Gemini call
//     private final Map<String, float[]> queryCache = Collections.synchronizedMap(
//             new LinkedHashMap<String, float[]>(16, 0.75f, true) {
//                 @Override
//                 protected boolean removeEldestEntry(Map.Entry<String, float[]> eldest) {
//                     return size() > 200;
//                 }
//             });

//     // circuit breaker: after 3 Gemini failures in a row, skip Gemini for a minute
//     private final AtomicInteger consecutiveFailures = new AtomicInteger();
//     private volatile long pausedUntil = 0;

//     public CourseAiService(CourseRepo courseRepo, GeminiService gemini,
//                            @Value("${course.ai.similarity-threshold:0.55}") double threshold) {
//         this.courseRepo = courseRepo;
//         this.gemini = gemini;
//         this.threshold = threshold;
//     }

//     public List<Course> searchCourses(String query) {
//         List<Course> courses = courseRepo.findAll();
//         if (query == null || query.isBlank() || courses.isEmpty()) {
//             return courses;
//         }
//         if (gemini.isEnabled() && System.currentTimeMillis() >= pausedUntil) {
//             try {
//                 float[] queryVec = queryVector(query);
//                 if (queryVec != null) {
//                     List<Course> ranked = semanticRank(courses, queryVec);
//                     consecutiveFailures.set(0);
//                     return ranked;
//                 }
//             } catch (RuntimeException e) {
//                 log.warn("Gemini search failed, using lexical fallback: {}", e.getMessage());
//                 if (consecutiveFailures.incrementAndGet() >= 3) {
//                     pausedUntil = System.currentTimeMillis() + 60_000L;
//                     consecutiveFailures.set(0);
//                     log.warn("Gemini paused for 60 seconds after repeated failures");
//                 }
//             }
//         }
//         return lexicalRank(courses, query);
//     }

//     private float[] queryVector(String query) {
//         String key = query.trim().toLowerCase();
//         float[] cached = queryCache.get(key);
//         if (cached != null) return cached;
//         float[] vec = gemini.embed(query);
//         if (vec != null) queryCache.put(key, vec);
//         return vec;
//     }

//     private List<Course> semanticRank(List<Course> courses, float[] queryVec) {
//         List<Scored> scored = new ArrayList<>();
//         for (Course c : courses) {
//             float[] vec = courseVector(c);
//             scored.add(new Scored(c, vec == null ? 0 : cosine(queryVec, vec)));
//         }
//         scored.sort(Comparator.comparingDouble(Scored::score).reversed());
//         List<Course> hits = scored.stream().filter(s -> s.score() >= threshold).map(Scored::course).toList();
//         if (!hits.isEmpty()) return hits;
//         // nothing cleared the threshold: still show the closest matches rather than an empty page
//         return scored.stream().limit(MIN_RESULTS).map(Scored::course).toList();
//     }

//     private float[] courseVector(Course c) {
//         String profile = profileText(c);
//         int hash = profile.hashCode();
//         CacheEntry cached = cache.get(c.getCourseId());
//         if (cached != null && cached.hash() == hash) return cached.vector();
//         float[] vec = gemini.embed(profile);
//         if (vec != null) cache.put(c.getCourseId(), new CacheEntry(hash, vec));
//         return vec;
//     }

//     private List<Course> lexicalRank(List<Course> courses, String query) {
//         Set<String> q = tokens(query);
//         return courses.stream()
//                 .map(c -> new Scored(c, jaccard(q, tokens(profileText(c)))))
//                 .sorted(Comparator.comparingDouble(Scored::score).reversed())
//                 .map(Scored::course)
//                 .toList();
//     }

//     static String profileText(Course c) {
//         return "Course type: " + nullToEmpty(c.getCourseType())
//                 + ". Details: " + nullToEmpty(c.getCourseDetails())
//                 + ". Price: " + (c.getCoursePrice() == null ? "" : c.getCoursePrice());
//     }

//     private static String nullToEmpty(String s) {
//         return s == null ? "" : s;
//     }

//     static Set<String> tokens(String text) {
//         Set<String> out = new HashSet<>();
//         for (String t : text.toLowerCase().split("[^a-z0-9+#]+")) {
//             if (!t.isBlank()) out.add(t);
//         }
//         return out;
//     }

//     static double jaccard(Set<String> a, Set<String> b) {
//         if (a.isEmpty() || b.isEmpty()) return 0;
//         Set<String> inter = new HashSet<>(a);
//         inter.retainAll(b);
//         Set<String> union = new HashSet<>(a);
//         union.addAll(b);
//         return (double) inter.size() / union.size();
//     }

//     static double cosine(float[] a, float[] b) {
//         int n = Math.min(a.length, b.length);
//         double dot = 0, na = 0, nb = 0;
//         for (int i = 0; i < n; i++) {
//             dot += a[i] * b[i];
//             na += a[i] * a[i];
//             nb += b[i] * b[i];
//         }
//         return (na == 0 || nb == 0) ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
//     }
// }


package com.examly.springapp.service;

import com.examly.springapp.model.Course;
import com.examly.springapp.repository.CourseRepo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AI course search: Hybrid search with price filter extraction + Gemini embeddings + lexical fallback.
 *
 * @author Sumit
 */
@Service
public class CourseAiService {
    private static final Logger log = LoggerFactory.getLogger(CourseAiService.class);
    private static final int MIN_RESULTS = 3;

    private record CacheEntry(int hash, float[] vector) {}
    private record Scored(Course course, double score) {}
    private record PriceConstraint(Double minPrice, Double maxPrice, String cleanQuery) {}

    private final CourseRepo courseRepo;
    private final GeminiService gemini;
    private final double threshold;
    private final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    private final Map<String, float[]> queryCache = Collections.synchronizedMap(
            new LinkedHashMap<String, float[]>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, float[]> eldest) {
                    return size() > 200;
                }
            });

    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private volatile long pausedUntil = 0;

    public CourseAiService(CourseRepo courseRepo, GeminiService gemini,
                           @Value("${course.ai.similarity-threshold:0.55}") double threshold) {
        this.courseRepo = courseRepo;
        this.gemini = gemini;
        this.threshold = threshold;
    }

    public List<Course> searchCourses(String query) {
        List<Course> courses = courseRepo.findAll();
        if (query == null || query.isBlank() || courses.isEmpty()) {
            return courses;
        }

        // 1. Extract any price constraints from user query (e.g. "greater than 2000", "under 1500", "free")
        PriceConstraint constraint = extractPriceConstraint(query);

        // 2. Apply mathematical price filter
        List<Course> filteredCourses = courses.stream()
                .filter(c -> matchesPrice(c, constraint))
                .toList();

        // If no courses satisfy the price constraint, return empty list!
        if (filteredCourses.isEmpty()) {
            return List.of();
        }

        // If the query was purely a price filter (e.g. "course prize greater than 2000"),
        // return the filtered courses directly without text ranking
        String remainingQuery = constraint.cleanQuery().trim();
        if (remainingQuery.isEmpty()) {
            return filteredCourses;
        }

        // 3. Perform Semantic Search on remaining keywords if Gemini is available
        if (gemini.isEmbeddingEnabled() && System.currentTimeMillis() >= pausedUntil) {
            try {
                float[] queryVec = queryVector(remainingQuery);
                if (queryVec != null) {
                    List<Course> ranked = semanticRank(filteredCourses, queryVec);
                    if (!ranked.isEmpty()) {
                        consecutiveFailures.set(0);
                        return ranked;
                    }
                }
            } catch (RuntimeException e) {
                log.warn("Gemini search failed, using lexical fallback: {}", e.getMessage());
                if (consecutiveFailures.incrementAndGet() >= 3) {
                    pausedUntil = System.currentTimeMillis() + 60_000L;
                    consecutiveFailures.set(0);
                    log.warn("Gemini paused for 60 seconds after repeated failures");
                }
            }
        }

        // 4. Lexical keyword fallback on remaining text
        return lexicalRank(filteredCourses, remainingQuery);
    }

    /**
     * Parses numeric conditions like "greater than 2000", "above 500", "under 1000", "free".
     */
    private PriceConstraint extractPriceConstraint(String query) {
        Double min = null;
        Double max = null;
        String clean = query;

        // Check for "free"
        if (clean.matches("(?i).*\\bfree\\b.*")) {
            max = 0.0;
            clean = clean.replaceAll("(?i)\\bfree\\b", " ");
        }

        // Check for: greater than / more than / above / over / >
        Pattern gtPattern = Pattern.compile(
                "(?i)\\b(?:greater\\s+than|more\\s+than|above|over|>)\\s*(?:rs\\.?|inr|₹|\\$)?\\s*(\\d+(?:\\.\\d+)?)");
        Matcher gtMatcher = gtPattern.matcher(clean);
        if (gtMatcher.find()) {
            min = Double.parseDouble(gtMatcher.group(1));
            clean = clean.replace(gtMatcher.group(0), " ");
        }

        // Check for: less than / under / below / cheaper than / <
        Pattern ltPattern = Pattern.compile(
                "(?i)\\b(?:less\\s+than|under|below|cheaper\\s+than|<)\\s*(?:rs\\.?|inr|₹|\\$)?\\s*(\\d+(?:\\.\\d+)?)");
        Matcher ltMatcher = ltPattern.matcher(clean);
        if (ltMatcher.find()) {
            max = Double.parseDouble(ltMatcher.group(1));
            clean = clean.replace(ltMatcher.group(0), " ");
        }

        // Remove filler query terms like "course", "courses", "price", "prize" if they were part of the condition
        clean = clean.replaceAll("(?i)\\b(?:course|courses|price|prize|cost|fee)\\b", " ").replaceAll("\\s+", " ").trim();

        return new PriceConstraint(min, max, clean);
    }

    private boolean matchesPrice(Course c, PriceConstraint constraint) {
        if (constraint.minPrice() == null && constraint.maxPrice() == null) {
            return true;
        }
        if (c.getCoursePrice() == null) {
            return false;
        }

        double price;
        try {
            price = Double.parseDouble(c.getCoursePrice().toString());
        } catch (NumberFormatException e) {
            return false;
        }

        // Strict mathematical comparison
        if (constraint.minPrice() != null && price <= constraint.minPrice()) {
            return false; // 1999 is NOT greater than 2000!
        }
        if (constraint.maxPrice() != null && price >= constraint.maxPrice()) {
            return false;
        }

        return true;
    }

    private float[] queryVector(String query) {
        String key = query.trim().toLowerCase();
        float[] cached = queryCache.get(key);
        if (cached != null) return cached;
        float[] vec = gemini.embed(query);
        if (vec != null) queryCache.put(key, vec);
        return vec;
    }

    private List<Course> semanticRank(List<Course> courses, float[] queryVec) {
        List<Scored> scored = new ArrayList<>();
        for (Course c : courses) {
            float[] vec = courseVector(c);
            scored.add(new Scored(c, vec == null ? 0 : cosine(queryVec, vec)));
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed());

        List<Course> hits = scored.stream()
                .filter(s -> s.score() >= threshold)
                .map(Scored::course)
                .toList();
        if (!hits.isEmpty()) return hits;

        return scored.stream()
                .filter(s -> s.score() > 0.25)
                .limit(MIN_RESULTS)
                .map(Scored::course)
                .toList();
    }

    private float[] courseVector(Course c) {
        if (c.getCourseId() == null) return null;
        String profile = profileText(c);
        int hash = profile.hashCode();
        CacheEntry cached = cache.get(c.getCourseId());
        if (cached != null && cached.hash() == hash) return cached.vector();
        float[] vec = gemini.embed(profile);
        if (vec != null) cache.put(c.getCourseId(), new CacheEntry(hash, vec));
        return vec;
    }

    private List<Course> lexicalRank(List<Course> courses, String query) {
        Set<String> q = tokens(query);
        if (q.isEmpty()) return courses;

        List<Course> matched = courses.stream()
                .map(c -> new Scored(c, jaccard(q, tokens(profileText(c)))))
                .filter(s -> s.score() > 0.0)
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .map(Scored::course)
                .toList();

        if (matched.isEmpty()) {
            String lowerQuery = query.toLowerCase().trim();
            matched = courses.stream()
                    .filter(c -> (c.getCourseType() != null && c.getCourseType().toLowerCase().contains(lowerQuery))
                            || (c.getCourseDetails() != null && c.getCourseDetails().toLowerCase().contains(lowerQuery)))
                    .toList();
        }

        return matched;
    }

    static String profileText(Course c) {
        return "Course type: " + nullToEmpty(c.getCourseType())
                + ". Details: " + nullToEmpty(c.getCourseDetails())
                + ". Price: " + (c.getCoursePrice() == null ? "" : c.getCoursePrice());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    static Set<String> tokens(String text) {
        Set<String> out = new HashSet<>();
        for (String t : text.toLowerCase().split("[^a-z0-9+#]+")) {
            if (!t.isBlank()) out.add(t);
        }
        return out;
    }

    static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) inter.size() / union.size();
    }

    static double cosine(float[] a, float[] b) {
        int n = Math.min(a.length, b.length);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return (na == 0 || nb == 0) ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}