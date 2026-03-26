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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class TmdbService {

    private static final Logger log = LoggerFactory.getLogger(TmdbService.class);

    private static final String BASE_URL   = "https://api.themoviedb.org/3";
    private static final String IMAGE_BASE = "https://image.tmdb.org/t/p/w500";
    private static final String BOOKS_URL  = "https://www.googleapis.com/books/v1";

    @Value("${tmdb.bearer-token:}")
    private String bearerTokenFromProps;

    private String bearerToken;

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public TmdbService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    @PostConstruct
    public void init() {
        bearerToken = bearerTokenFromProps;
        if (bearerToken == null || bearerToken.isBlank()) {
            bearerToken = System.getenv("TMDB_BEARER_TOKEN");
            if (bearerToken != null && !bearerToken.isBlank())
                log.info("[TMDB] Token obtenido via System.getenv");
        } else {
            log.info("[TMDB] Token obtenido via application.properties");
        }
        if (bearerToken == null || bearerToken.isBlank()) {
            log.warn("[TMDB] ADVERTENCIA: TMDB_BEARER_TOKEN vacio por todas las vias");
        } else {
            log.info("[TMDB] Token cargado OK (primeros 10 chars: {}...)",
                bearerToken.substring(0, Math.min(10, bearerToken.length())));
        }
    }

    public String searchMovieCover(String title) {
        if (bearerToken == null || bearerToken.isBlank()) return null;
        try {
            log.info("[TMDB] Buscando pelicula: '{}'", title);
            String json = webClient.get()
                .uri(BASE_URL + "/search/movie?query={q}&language=es-ES&page=1", title)
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve().bodyToMono(String.class).block();
            String result = extractPosterPath(json);
            log.info("[TMDB] Resultado pelicula '{}': {}", title, result != null ? result : "sin resultado");
            return result;
        } catch (WebClientResponseException e) {
            log.error("[TMDB] Error HTTP {} buscando pelicula '{}': {}", e.getStatusCode(), title, e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("[TMDB] Error buscando pelicula '{}': {}", title, e.getMessage());
            return null;
        }
    }

    public String searchSerieCover(String title) {
        if (bearerToken == null || bearerToken.isBlank()) return null;
        try {
            log.info("[TMDB] Buscando serie: '{}'", title);
            String json = webClient.get()
                .uri(BASE_URL + "/search/tv?query={q}&language=es-ES&page=1", title)
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve().bodyToMono(String.class).block();
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

    /**
     * Busca portada de libro en Google Books.
     * Estrategia: primero busca con título+autor, si no hay resultado busca solo título.
     * Sin langRestrict para no perder ediciones.
     */
    public String searchBookCover(String title, String author) {
        try {
            // Intento 1: título + autor
            if (author != null && !author.isBlank()) {
                String url = _googleBooksSearch(
                    "intitle:" + URLEncoder.encode(title, StandardCharsets.UTF_8)
                    + "+inauthor:" + URLEncoder.encode(author, StandardCharsets.UTF_8));
                if (url != null) return url;
                log.info("[Books] Sin resultado con autor, reintentando solo con titulo");
            }
            // Intento 2: solo título
            String url = _googleBooksSearch("intitle:" + URLEncoder.encode(title, StandardCharsets.UTF_8));
            if (url != null) return url;

            // Intento 3: búsqueda libre (más permisiva)
            url = _googleBooksSearch(URLEncoder.encode(title, StandardCharsets.UTF_8));
            log.info("[Books] Resultado final para '{}': {}", title, url != null ? url : "sin resultado");
            return url;
        } catch (Exception e) {
            log.error("[Books] Error buscando libro '{}': {}", title, e.getMessage());
            return null;
        }
    }

    private String _googleBooksSearch(String encodedQuery) {
        try {
            String json = webClient.get()
                .uri(BOOKS_URL + "/volumes?q=" + encodedQuery + "&maxResults=3&orderBy=relevance")
                .retrieve().bodyToMono(String.class).block();
            JsonNode root  = mapper.readTree(json);
            JsonNode items = root.path("items");
            if (!items.isArray() || items.size() == 0) return null;
            // Recorrer los primeros resultados buscando uno con imagen
            for (JsonNode item : items) {
                JsonNode img = item.path("volumeInfo").path("imageLinks");
                String url = img.path("thumbnail").asText(null);
                if (url == null) url = img.path("smallThumbnail").asText(null);
                if (url != null && !url.isBlank()) {
                    url = url.replace("http://", "https://")
                             .replaceAll("&edge=curl", ""); // quitar efecto curl
                    log.info("[Books] Portada encontrada: {}", url);
                    return url;
                }
            }
        } catch (Exception e) {
            log.warn("[Books] Error en búsqueda '{}': {}", encodedQuery, e.getMessage());
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
