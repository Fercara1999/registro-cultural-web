package com.registrocultural.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TmdbService {

    private static final String BASE_URL    = "https://api.themoviedb.org/3";
    private static final String IMAGE_BASE  = "https://image.tmdb.org/t/p/w500";
    private static final String BOOKS_URL   = "https://www.googleapis.com/books/v1";

    @Value("${TMDB_BEARER_TOKEN:}")
    private String bearerToken;

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public TmdbService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    /** Busca portada de película en TMDB. Devuelve URL completa o null. */
    public String searchMovieCover(String title) {
        try {
            String json = webClient.get()
                .uri(BASE_URL + "/search/movie?query={q}&language=es-ES&page=1", title)
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            return extractPosterPath(json);
        } catch (Exception e) { return null; }
    }

    /** Busca portada de serie en TMDB. Devuelve URL completa o null. */
    public String searchSerieCover(String title) {
        try {
            String json = webClient.get()
                .uri(BASE_URL + "/search/tv?query={q}&language=es-ES&page=1", title)
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            return extractPosterPath(json);
        } catch (Exception e) { return null; }
    }

    /** Busca portada de libro en Google Books. Devuelve URL completa o null. */
    public String searchBookCover(String title, String author) {
        try {
            String query = title + (author != null && !author.isBlank() ? "+inauthor:" + author : "");
            String json = webClient.get()
                .uri(BOOKS_URL + "/volumes?q={q}&maxResults=1&langRestrict=es", query)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            JsonNode root = mapper.readTree(json);
            JsonNode items = root.path("items");
            if (items.isArray() && items.size() > 0) {
                JsonNode img = items.get(0).path("volumeInfo").path("imageLinks");
                String url = img.path("thumbnail").asText(null);
                if (url == null) url = img.path("smallThumbnail").asText(null);
                if (url != null) return url.replace("http://", "https://");
            }
        } catch (Exception e) { /* no cover */ }
        return null;
    }

    private String extractPosterPath(String json) throws Exception {
        if (json == null) return null;
        JsonNode root    = mapper.readTree(json);
        JsonNode results = root.path("results");
        if (results.isArray() && results.size() > 0) {
            String path = results.get(0).path("poster_path").asText(null);
            if (path != null && !path.isBlank()) return IMAGE_BASE + path;
        }
        return null;
    }
}
