package com.registrocultural.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Service
public class TmdbService {

    private static final Logger log = LoggerFactory.getLogger(TmdbService.class);

    private static final String BASE_URL   = "https://api.themoviedb.org/3";
    private static final String IMAGE_BASE = "https://image.tmdb.org/t/p/w500";
    private static final String BOOKS_URL  = "https://www.googleapis.com/books/v1";

    // Leer directamente la variable de entorno, sin pasar por application.properties
    @Value("#{systemEnvironment['TMDB_BEARER_TOKEN'] ?: ''}")
    private String bearerToken;

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public TmdbService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    @PostConstruct
    public void init() {
        if (bearerToken == null || bearerToken.isBlank()) {
            log.warn("[TMDB] ADVERTENCIA: TMDB_BEARER_TOKEN no encontrado en variables de entorno");
        } else {
            log.info("[TMDB] Token cargado correctamente (primeros 10 chars: {}...)", bearerToken.substring(0, Math.min(10, bearerToken.length())));
        }
    }

    /** Busca portada de película en TMDB. Devuelve URL completa o null. */
    public String searchMovieCover(String title) {
        if (bearerToken == null || bearerToken.isBlank()) {
            log.warn("[TMDB] bearerToken está vacío - no se puede buscar portada de película");
            return null;
        }
        try {
            log.info("[TMDB] Buscando película: '{}'", title);
            String json = webClient.get()
                .uri(BASE_URL + "/search/movie?query={q}&language=es-ES&page=1", title)
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            String result = extractPosterPath(json);
            log.info("[TMDB] Resultado película '{}': {}", title, result != null ? result : "sin resultado");
            return result;
        } catch (WebClientResponseException e) {
            log.error("[TMDB] Error HTTP {} buscando película '{}': {}", e.getStatusCode(), title, e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("[TMDB] Error buscando película '{}': {}", title, e.getMessage());
            return null;
        }
    }

    /** Busca portada de serie en TMDB. Devuelve URL completa o null. */
    public String searchSerieCover(String title) {
        if (bearerToken == null || bearerToken.isBlank()) {
            log.warn("[TMDB] bearerToken está vacío - no se puede buscar portada de serie");
            return null;
        }
        try {
            log.info("[TMDB] Buscando serie: '{}'", title);
            String json = webClient.get()
                .uri(BASE_URL + "/search/tv?query={q}&language=es-ES&page=1", title)
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            String result = extractPosterPath(json);
            log.info("[TMDB] Resultado serie '{}': {}", title, result != null ? result : "sin resultado");
            return result;
        } catch (WebClientResponseException e) {
            log.error("[TMDB] Error HTTP {} buscando serie '{}': {}", e.getStatusCode(), title, e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("[TMDB] Error buscando serie '{}': {}", title, e.getMessage());
            return null;
        }
    }

    /** Busca portada de libro en Google Books. Devuelve URL completa o null. */
    public String searchBookCover(String title, String author) {
        try {
            String query = title + (author != null && !author.isBlank() ? "+inauthor:" + author : "");
            log.info("[Books] Buscando libro: '{}'", query);
            String json = webClient.get()
                .uri(BOOKS_URL + "/volumes?q={q}&maxResults=1&langRestrict=es", query)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            JsonNode root  = mapper.readTree(json);
            JsonNode items = root.path("items");
            if (items.isArray() && items.size() > 0) {
                JsonNode img = items.get(0).path("volumeInfo").path("imageLinks");
                String url = img.path("thumbnail").asText(null);
                if (url == null) url = img.path("smallThumbnail").asText(null);
                if (url != null) {
                    url = url.replace("http://", "https://");
                    log.info("[Books] Portada encontrada para '{}': {}", title, url);
                    return url;
                }
            }
            log.info("[Books] Sin resultado para '{}'", title);
        } catch (Exception e) {
            log.error("[Books] Error buscando libro '{}': {}", title, e.getMessage());
        }
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
