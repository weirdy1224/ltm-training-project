// // package com.examly.springapp.service;
 
// // import java.net.URI;
// // import java.net.http.HttpClient;
// // import java.net.http.HttpRequest;
// // import java.net.http.HttpResponse;
// // import java.time.Duration;
// // import java.time.LocalDate;
// // import java.util.List;
// // import java.util.Map;
// // import java.util.concurrent.atomic.AtomicInteger;
 
// // import org.slf4j.Logger;
// // import org.slf4j.LoggerFactory;
// // import org.springframework.beans.factory.annotation.Autowired;
// // import org.springframework.beans.factory.annotation.Value;
// // import org.springframework.stereotype.Service;
 
// // import com.examly.springapp.exception.ApiCommunicationException;
// // import com.fasterxml.jackson.databind.JsonNode;
// // import com.fasterxml.jackson.databind.ObjectMapper;
 
// // /**
// //  * Thin client for the Gemini embedding and generation API with a daily call budget.
// //  *
// //  * @author Sumit
// //  */
// // @Service
// // public class GeminiService {
// //     private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
// //     /** Standing instructions for the OpenAI-compatible provider; the task itself is in the user message. */
// //     private static final String SYSTEM_PROMPT =
// //             "You are the AI assistant of an online e-learning platform used by students in India. "
// //             + "Follow the formatting instructions in the user's message exactly. "
// //             + "Give the final answer directly: do not show your reasoning, do not add a preface or a closing remark. "
// //             + "When the user asks for JSON, reply with one valid JSON object only: no markdown fences and no text before or after it. "
// //             + "Be accurate. If you are not sure of a fact, say so briefly instead of guessing. Use plain, friendly English.";
// //     private static final String BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
 
// //     private final String apiKey;
// //     private final String embeddingModel;
// //     private final String generationModel;
// //     /** Optional OpenAI-compatible provider (Groq, OpenRouter, ...). When llm.api.key is set it answers all text generation. */
// //     private final String llmKey;
// //     private final String llmBase;
// //     private final String llmModel;
// //     private final ObjectMapper mapper = new ObjectMapper();
// //     private final int dailyBudget;
// //     private final AtomicInteger used = new AtomicInteger();
// //     private volatile LocalDate budgetDay = LocalDate.now();
// //     private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
 
// //     @Autowired
// //     public GeminiService(@Value("${gemini.api.key:}") String apiKey,
// //                          @Value("${gemini.embedding.model:text-embedding-004}") String embeddingModel,
// //                          @Value("${gemini.generation.model:gemini-2.5-flash}") String generationModel,
// //                          @Value("${ai.daily-embedding-budget:2000}") int dailyBudget,
// //                          @Value("${llm.api.key:}") String llmKey,
// //                          @Value("${llm.base-url:https://api.groq.com/openai/v1}") String llmBase,
// //                          @Value("${llm.model:llama-3.3-70b-versatile}") String llmModel) {
// //         this.dailyBudget = dailyBudget;
// //         this.apiKey = apiKey == null ? "" : apiKey.trim();
// //         this.embeddingModel = embeddingModel;
// //         this.generationModel = generationModel;
// //         this.llmKey = llmKey == null ? "" : llmKey.trim();
// //         this.llmBase = llmBase == null ? "" : (llmBase.trim().endsWith("/") ? llmBase.trim().substring(0, llmBase.trim().length() - 1) : llmBase.trim());
// //         this.llmModel = llmModel == null ? "" : llmModel.trim();
// //     }
 
// //     /** Gemini only (no other provider). */
// //     public GeminiService(String apiKey, String embeddingModel, String generationModel, int dailyBudget) {
// //         this(apiKey, embeddingModel, generationModel, dailyBudget, "", "", "");
// //     }
 
// //     /** True when text generation (quiz, chatbot) is available: an llm.api.key or a gemini.api.key is configured. */
// //     public boolean isEnabled() {
// //         return !llmKey.isEmpty() || !apiKey.isEmpty();
// //     }
 
// //     /** Returns the embedding vector for the text, or null when Gemini is not enabled. */
// //     public float[] embed(String text) {
// //         if (apiKey.isEmpty()) return null;   // embeddings need the Gemini key; without it the course search uses the keyword fallback
// //         try {
// //             String body = mapper.writeValueAsString(Map.of(
// //                     "content", Map.of("parts", List.of(Map.of("text", text)))));
// //             JsonNode root = post(embeddingModel + ":embedContent", body);
// //             JsonNode values = root.path("embedding").path("values");
// //             if (!values.isArray() || values.isEmpty()) {
// //                 throw new ApiCommunicationException("Gemini returned no embedding", null);
// //             }
// //             float[] vec = new float[values.size()];
// //             for (int i = 0; i < vec.length; i++) vec[i] = (float) values.get(i).asDouble();
// //             return vec;
// //         } catch (ApiCommunicationException e) {
// //             throw e;
// //         } catch (Exception e) {
// //             throw new ApiCommunicationException("Gemini embedding request failed", e);
// //         }
// //     }
 
// //     /** Returns Gemini-generated text, or null when Gemini is not enabled. */
// //     public String generate(String prompt) {
// //         if (!isEnabled()) return null;
// //         if (!llmKey.isEmpty()) return generateWithLlm(prompt);
// //         try {
// //             Map<String, Object> request = new java.util.LinkedHashMap<>();
// //             request.put("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
// //             if (generationModel.contains("2.5-flash")) {
// //                 // 2.5 Flash "thinks" before it answers, which can take longer than our timeout; a quiz does not need it
// //                 request.put("generationConfig", Map.of("thinkingConfig", Map.of("thinkingBudget", 0)));
// //             }
// //             String body = mapper.writeValueAsString(request);
// //             JsonNode root = post(generationModel + ":generateContent", body);
// //             return root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
// //         } catch (ApiCommunicationException e) {
// //             throw e;
// //         } catch (Exception e) {
// //             log.warn("Gemini generation request failed: {}", e.toString());
// //             throw new ApiCommunicationException("Gemini generation request failed", e);
// //         }
// //     }
 
// //     /** Text generation through an OpenAI-compatible "chat/completions" API (Groq, OpenRouter, ...). */
// //     private String generateWithLlm(String prompt) {
// //         try {
// //             consumeBudget();
// //             String body = mapper.writeValueAsString(Map.of(
// //                     "model", llmModel,
// //                     "temperature", 0.4,
// //                     "messages", List.of(
// //                             Map.of("role", "system", "content", SYSTEM_PROMPT),
// //                             Map.of("role", "user", "content", prompt))));
// //             HttpRequest req = HttpRequest.newBuilder(URI.create(llmBase + "/chat/completions"))
// //                     .timeout(Duration.ofSeconds(60))
// //                     .header("Content-Type", "application/json")
// //                     .header("Authorization", "Bearer " + llmKey)
// //                     .POST(HttpRequest.BodyPublishers.ofString(body))
// //                     .build();
// //             HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
// //             if (res.statusCode() / 100 != 2) {
// //                 String text = res.body() == null ? "" : res.body().replaceAll("\\s+", " ");
// //                 log.warn("LLM {} answered {}: {}", llmBase, res.statusCode(), text.length() > 400 ? text.substring(0, 400) : text);
// //                 throw new ApiCommunicationException("LLM API responded with status " + res.statusCode(), null);
// //             }
// //             String content = mapper.readTree(res.body()).path("choices").path(0).path("message").path("content").asText(null);
// //             return content == null ? null : content.replaceAll("(?s)<think>.*?</think>", "").trim();   // some models print their reasoning in <think> tags
// //         } catch (ApiCommunicationException e) {
// //             throw e;
// //         } catch (InterruptedException e) {
// //             Thread.currentThread().interrupt();
// //             throw new ApiCommunicationException("LLM request was interrupted", e);
// //         } catch (Exception e) {
// //             log.warn("LLM request failed: {}", e.toString());
// //             throw new ApiCommunicationException("LLM request failed", e);
// //         }
// //     }
 
// //     /** Caps how many Gemini calls the whole app makes per day, whatever the users do. */
// //     private void consumeBudget() {
// //         LocalDate today = LocalDate.now();
// //         if (!today.equals(budgetDay)) {
// //             synchronized (this) {
// //                 if (!today.equals(budgetDay)) {
// //                     budgetDay = today;
// //                     used.set(0);
// //                 }
// //             }
// //         }
// //         if (used.incrementAndGet() > dailyBudget) {
// //             throw new ApiCommunicationException("Daily AI call budget exhausted", null);
// //         }
// //     }
 
// //     private JsonNode post(String path, String json) throws Exception {
// //         consumeBudget();
// //         HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
// //                 .timeout(Duration.ofSeconds(60))
// //                 .header("Content-Type", "application/json")
// //                 .header("x-goog-api-key", apiKey)
// //                 .POST(HttpRequest.BodyPublishers.ofString(json))
// //                 .build();
// //         HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
// //         if (res.statusCode() / 100 != 2) {
// //             // the real reason (for example "API key not valid") goes to the log; the key itself is never logged
// //             String body = res.body() == null ? "" : res.body().replaceAll("\\s+", " ");
// //             log.warn("Gemini {} answered {}: {}", path, res.statusCode(), body.length() > 400 ? body.substring(0, 400) : body);
// //             throw new ApiCommunicationException("Gemini API responded with status " + res.statusCode(), null);
// //         }
// //         return mapper.readTree(res.body());
// //     }
// // }
 

// package com.examly.springapp.service;

// import java.net.URI;
// import java.net.http.HttpClient;
// import java.net.http.HttpRequest;
// import java.net.http.HttpResponse;
// import java.time.Duration;
// import java.time.LocalDate;
// import java.util.List;
// import java.util.Map;
// import java.util.concurrent.atomic.AtomicInteger;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;

// import com.examly.springapp.exception.ApiCommunicationException;
// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;

// /**
//  * Thin client for the Gemini embedding and generation API with a daily call budget.
//  *
//  * @author Sumit
//  */
// @Service
// public class GeminiService {
//     private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    
//     private static final String SYSTEM_PROMPT =
//             "You are the AI assistant of an online e-learning platform used by students in India. "
//             + "Follow the formatting instructions in the user's message exactly. "
//             + "Give the final answer directly: do not show your reasoning, do not add a preface or a closing remark. "
//             + "When the user asks for JSON, reply with one valid JSON object only: no markdown fences and no text before or after it. "
//             + "Be accurate. If you are not sure of a fact, say so briefly instead of guessing. Use plain, friendly English.";
//     private static final String BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

//     private final String apiKey;
//     private final String embeddingModel;
//     private final String generationModel;
//     private final String llmKey;
//     private final String llmBase;
//     private final String llmModel;
//     private final ObjectMapper mapper = new ObjectMapper();
//     private final int dailyBudget;
//     private final AtomicInteger used = new AtomicInteger();
//     private volatile LocalDate budgetDay = LocalDate.now();
//     private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

//     @Autowired
//     public GeminiService(@Value("${gemini.api.key:}") String apiKey,
//                          @Value("${gemini.embedding.model:text-embedding-004}") String embeddingModel,
//                          @Value("${gemini.generation.model:gemini-1.5-flash}") String generationModel,
//                          @Value("${ai.daily-embedding-budget:2000}") int dailyBudget,
//                          @Value("${llm.api.key:}") String llmKey,
//                          @Value("${llm.base-url:https://api.groq.com/openai/v1}") String llmBase,
//                          @Value("${llm.model:llama-3.3-70b-versatile}") String llmModel) {
//         this.dailyBudget = dailyBudget;
//         this.apiKey = sanitizeKey(apiKey);
//         this.embeddingModel = embeddingModel;
//         this.generationModel = generationModel;
//         this.llmKey = sanitizeKey(llmKey);
//         this.llmBase = llmBase == null ? "" : (llmBase.trim().endsWith("/") ? llmBase.trim().substring(0, llmBase.trim().length() - 1) : llmBase.trim());
//         this.llmModel = llmModel == null ? "" : llmModel.trim();
//     }

//     public GeminiService(String apiKey, String embeddingModel, String generationModel, int dailyBudget) {
//         this(apiKey, embeddingModel, generationModel, dailyBudget, "", "", "");
//     }

//     private static String sanitizeKey(String key) {
//         if (key == null) return "";
//         String trimmed = key.trim();
//         if (trimmed.equalsIgnoreCase("YOUR API KEY") || trimmed.contains("YOUR API KEY") || trimmed.isBlank()) {
//             return "";
//         }
//         return trimmed;
//     }

//     /** True when text generation is available (via LLM provider or Gemini). */
//     public boolean isEnabled() {
//         return !llmKey.isEmpty() || !apiKey.isEmpty();
//     }

//     /** True only when a valid Gemini API key is configured for embeddings. */
//     public boolean isEmbeddingEnabled() {
//         return !apiKey.isEmpty();
//     }

//     /** Returns the embedding vector for the text, or null when Gemini embedding is not configured. */
//     public float[] embed(String text) {
//         if (!isEmbeddingEnabled() || text == null || text.isBlank()) {
//             return null;
//         }
//         try {
//             String body = mapper.writeValueAsString(Map.of(
//                     "content", Map.of("parts", List.of(Map.of("text", text)))));
//             JsonNode root = post(embeddingModel + ":embedContent", body);
//             JsonNode values = root.path("embedding").path("values");
//             if (!values.isArray() || values.isEmpty()) {
//                 throw new ApiCommunicationException("Gemini returned no embedding", null);
//             }
//             float[] vec = new float[values.size()];
//             for (int i = 0; i < vec.length; i++) {
//                 vec[i] = (float) values.get(i).asDouble();
//             }
//             return vec;
//         } catch (ApiCommunicationException e) {
//             throw e;
//         } catch (Exception e) {
//             throw new ApiCommunicationException("Gemini embedding request failed", e);
//         }
//     }

//     /** Returns Gemini-generated text, or null when not enabled. */
//     public String generate(String prompt) {
//         if (!isEnabled()) return null;
//         if (!llmKey.isEmpty()) return generateWithLlm(prompt);
//         try {
//             Map<String, Object> request = new java.util.LinkedHashMap<>();
//             request.put("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
//             if (generationModel.contains("2.5-flash")) {
//                 request.put("generationConfig", Map.of("thinkingConfig", Map.of("thinkingBudget", 0)));
//             }
//             String body = mapper.writeValueAsString(request);
//             JsonNode root = post(generationModel + ":generateContent", body);
//             return root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
//         } catch (ApiCommunicationException e) {
//             throw e;
//         } catch (Exception e) {
//             log.warn("Gemini generation request failed: {}", e.toString());
//             throw new ApiCommunicationException("Gemini generation request failed", e);
//         }
//     }

//     private String generateWithLlm(String prompt) {
//         try {
//             consumeBudget();
//             String body = mapper.writeValueAsString(Map.of(
//                     "model", llmModel,
//                     "temperature", 0.4,
//                     "messages", List.of(
//                             Map.of("role", "system", "content", SYSTEM_PROMPT),
//                             Map.of("role", "user", "content", prompt))));
//             HttpRequest req = HttpRequest.newBuilder(URI.create(llmBase + "/chat/completions"))
//                     .timeout(Duration.ofSeconds(60))
//                     .header("Content-Type", "application/json")
//                     .header("Authorization", "Bearer " + llmKey)
//                     .POST(HttpRequest.BodyPublishers.ofString(body))
//                     .build();
//             HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
//             if (res.statusCode() / 100 != 2) {
//                 String text = res.body() == null ? "" : res.body().replaceAll("\\s+", " ");
//                 log.warn("LLM {} answered {}: {}", llmBase, res.statusCode(), text.length() > 400 ? text.substring(0, 400) : text);
//                 throw new ApiCommunicationException("LLM API responded with status " + res.statusCode(), null);
//             }
//             String content = mapper.readTree(res.body()).path("choices").path(0).path("message").path("content").asText(null);
//             return content == null ? null : content.replaceAll("(?s)<think>.*?</think>", "").trim();
//         } catch (ApiCommunicationException e) {
//             throw e;
//         } catch (InterruptedException e) {
//             Thread.currentThread().interrupt();
//             throw new ApiCommunicationException("LLM request was interrupted", e);
//         } catch (Exception e) {
//             log.warn("LLM request failed: {}", e.toString());
//             throw new ApiCommunicationException("LLM request failed", e);
//         }
//     }

//     private void consumeBudget() {
//         LocalDate today = LocalDate.now();
//         if (!today.equals(budgetDay)) {
//             synchronized (this) {
//                 if (!today.equals(budgetDay)) {
//                     budgetDay = today;
//                     used.set(0);
//                 }
//             }
//         }
//         if (used.incrementAndGet() > dailyBudget) {
//             throw new ApiCommunicationException("Daily AI call budget exhausted", null);
//         }
//     }

//     private JsonNode post(String path, String json) throws Exception {
//         consumeBudget();
//         HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
//                 .timeout(Duration.ofSeconds(60))
//                 .header("Content-Type", "application/json")
//                 .header("x-goog-api-key", apiKey)
//                 .POST(HttpRequest.BodyPublishers.ofString(json))
//                 .build();
//         HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
//         if (res.statusCode() / 100 != 2) {
//             String body = res.body() == null ? "" : res.body().replaceAll("\\s+", " ");
//             log.warn("Gemini {} answered {}: {}", path, res.statusCode(), body.length() > 400 ? body.substring(0, 400) : body);
//             throw new ApiCommunicationException("Gemini API responded with status " + res.statusCode(), null);
//         }
//         return mapper.readTree(res.body());
//     }
// }

package com.examly.springapp.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.examly.springapp.exception.ApiCommunicationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Client for Gemini embedding and fast text generation (Quiz & Chatbot)
 * with an automatic fallback to OpenRouter.
 *
 * @author Sumit
 */
@Service
public class GeminiService {
    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    
    /** Standing instructions for the AI model */
    private static final String SYSTEM_PROMPT =
            "You are the AI assistant of an online e-learning platform used by students in India. "
            + "Follow the formatting instructions in the user's message exactly. "
            + "Give the final answer directly: do not show your reasoning, do not add a preface or a closing remark. "
            + "When the user asks for JSON, reply with one valid JSON object only: no markdown fences and no text before or after it. "
            + "Be accurate. If you are not sure of a fact, say so briefly instead of guessing. Use plain, friendly English.";
    
    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final String apiKey;
    private final String embeddingModel;
    private final String generationModel;
    private final String llmKey;
    private final String llmBase;
    private final String llmModel;
    private final ObjectMapper mapper = new ObjectMapper();
    private final int dailyBudget;
    private final AtomicInteger used = new AtomicInteger();
    private volatile LocalDate budgetDay = LocalDate.now();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Autowired
    public GeminiService(@Value("${gemini.api.key:}") String apiKey,
                         @Value("${gemini.embedding.model:text-embedding-004}") String embeddingModel,
                         @Value("${gemini.generation.model:gemini-3.8-flash}") String generationModel,
                         @Value("${ai.daily-embedding-budget:2000}") int dailyBudget,
                         @Value("${llm.api.key:}") String llmKey,
                         @Value("${llm.base-url:https://api.groq.com/openai/v1}") String llmBase,
                         @Value("${llm.model:llama-3.3-70b-versatile}") String llmModel) {
        this.dailyBudget = dailyBudget;
        this.apiKey = sanitizeKey(apiKey);
        this.embeddingModel = embeddingModel;
        this.generationModel = generationModel;
        this.llmKey = sanitizeKey(llmKey);
        this.llmBase = llmBase == null ? "" : (llmBase.trim().endsWith("/") ? llmBase.trim().substring(0, llmBase.trim().length() - 1) : llmBase.trim());
        this.llmModel = llmModel == null ? "" : llmModel.trim();
    }

    public GeminiService(String apiKey, String embeddingModel, String generationModel, int dailyBudget) {
        this(apiKey, embeddingModel, generationModel, dailyBudget, "", "", "");
    }

    private static String sanitizeKey(String key) {
        if (key == null) return "";
        String trimmed = key.trim();
        if (trimmed.equalsIgnoreCase("YOUR API KEY") || trimmed.contains("YOUR API KEY") || trimmed.isBlank()) {
            return "";
        }
        return trimmed;
    }

    /** True when AI text generation is available (via Gemini or LLM provider). */
    public boolean isEnabled() {
        return !apiKey.isEmpty() || !llmKey.isEmpty();
    }

    /** True only when a valid Gemini API key is configured. */
    public boolean isEmbeddingEnabled() {
        return !apiKey.isEmpty();
    }

    /** Returns the embedding vector for course search. */
    public float[] embed(String text) {
        if (!isEmbeddingEnabled() || text == null || text.isBlank()) {
            return null;
        }
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "content", Map.of("parts", List.of(Map.of("text", text)))));
            JsonNode root = post(embeddingModel + ":embedContent", body);
            JsonNode values = root.path("embedding").path("values");
            if (!values.isArray() || values.isEmpty()) {
                throw new ApiCommunicationException("Gemini returned no embedding", null);
            }
            float[] vec = new float[values.size()];
            for (int i = 0; i < vec.length; i++) {
                vec[i] = (float) values.get(i).asDouble();
            }
            return vec;
        } catch (ApiCommunicationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiCommunicationException("Gemini embedding request failed", e);
        }
    }

    /**
     * Generates text (Quiz / Chatbot).
     * Prefers ultra-fast Gemini 1.5 Flash if a Gemini key is present;
     * automatically falls back to OpenRouter if Gemini fails or is unconfigured.
     */
        /**
     * Generates text (Quiz / Chatbot).
     * Prefers ultra-fast Gemini 1.5 Flash if a Gemini key is present;
     * automatically falls back to OpenRouter if Gemini fails or is unconfigured.
     */
        public String generate(String prompt) {
            if (!isEnabled()) {
                log.warn(">>> [AI] Neither Gemini nor OpenRouter is configured!");
                return null;
            }
    
            // 1. Try Gemini first
            if (isEmbeddingEnabled()) {
                log.info(">>> [AI] Calling GEMINI ({}) for generation...", generationModel);
                long startTime = System.currentTimeMillis();
                try {
                    Map<String, Object> request = new java.util.LinkedHashMap<>();
                    request.put("system_instruction", Map.of(
                            "parts", List.of(Map.of("text", SYSTEM_PROMPT))
                    ));
                    request.put("contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", prompt))
                    )));
    
                    Map<String, Object> genConfig = new java.util.LinkedHashMap<>();
                    genConfig.put("temperature", 0.4);
                    if (generationModel.contains("3.8-flash")) {
                        genConfig.put("thinkingConfig", Map.of("thinkingBudget", 0));
                    }
                    request.put("generationConfig", genConfig);
    
                    String body = mapper.writeValueAsString(request);
                    JsonNode root = post(generationModel + ":generateContent", body);
                    
                    String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
                    long duration = System.currentTimeMillis() - startTime;
                    log.info(">>> [AI SUCCESS] Generated via GEMINI in {} ms ({} seconds)!", duration, duration / 1000.0);
                    
                    if (text != null) {
                        return stripMarkdownFences(text.trim());
                    }
                } catch (Exception e) {
                    log.error(">>> [AI WARNING] Gemini failed: {}. Falling back to OpenRouter...", e.getMessage());
                    // Fall back to OpenRouter
                    if (!llmKey.isEmpty()) {
                        return generateWithLlm(prompt);
                    }
                    throw new ApiCommunicationException("AI generation failed", e);
                }
            }
    
            // 2. OpenRouter fallback
            if (!llmKey.isEmpty()) {
                log.info(">>> [AI] Calling OPENROUTER ({}) for generation...", llmModel);
                return generateWithLlm(prompt);
            }
    
            return null;
        }

    /** OpenAI-compatible fallback (Groq / OpenRouter) */
    private String generateWithLlm(String prompt) {
        try {
            consumeBudget();
            String body = mapper.writeValueAsString(Map.of(
                    "model", llmModel,
                    "temperature", 0.4,
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", prompt))));
            HttpRequest req = HttpRequest.newBuilder(URI.create(llmBase + "/chat/completions"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + llmKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                String text = res.body() == null ? "" : res.body().replaceAll("\\s+", " ");
                log.warn("LLM {} answered {}: {}", llmBase, res.statusCode(), text.length() > 400 ? text.substring(0, 400) : text);
                throw new ApiCommunicationException("LLM API responded with status " + res.statusCode(), null);
            }
            String content = mapper.readTree(res.body()).path("choices").path(0).path("message").path("content").asText(null);
            return content == null ? null : stripMarkdownFences(content.replaceAll("(?s)<think>.*?</think>", "").trim());
        } catch (ApiCommunicationException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiCommunicationException("LLM request was interrupted", e);
        } catch (Exception e) {
            log.warn("LLM request failed: {}", e.toString());
            throw new ApiCommunicationException("LLM request failed", e);
        }
    }

    /** Removes ```json ... ``` blocks so JSON parsers don't break */
    private String stripMarkdownFences(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastBackticks = trimmed.lastIndexOf("```");
            if (firstNewline != -1 && lastBackticks > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastBackticks).trim();
            }
        }
        return trimmed;
    }

    private void consumeBudget() {
        LocalDate today = LocalDate.now();
        if (!today.equals(budgetDay)) {
            synchronized (this) {
                if (!today.equals(budgetDay)) {
                    budgetDay = today;
                    used.set(0);
                }
            }
        }
        if (used.incrementAndGet() > dailyBudget) {
            throw new ApiCommunicationException("Daily AI call budget exhausted", null);
        }
    }

    private JsonNode post(String path, String json) throws Exception {
        consumeBudget();
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            String body = res.body() == null ? "" : res.body().replaceAll("\\s+", " ");
            log.warn("Gemini {} answered {}: {}", path, res.statusCode(), body.length() > 400 ? body.substring(0, 400) : body);
            throw new ApiCommunicationException("Gemini API responded with status " + res.statusCode(), null);
        }
        return mapper.readTree(res.body());
    }
}