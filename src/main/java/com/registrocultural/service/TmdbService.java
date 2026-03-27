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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
        Map<String, String> details = searchMovieDetails(title);
        return details.get("posterUrl");
    }

    public Map<String, String> searchMovieDetails(String title) {
        Map<String, String> result = new LinkedHashMap<>();
        if (bearerToken == null || bearerToken.isBlank()) return result;
        try {
            log.info("[TMDB] Buscando pelicula: '{}'", title);
            String searchJson = webClient.get()
                .uri(BASE_URL + "/search/movie?query={q}&language=es-ES&page=1", title)
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve().bodyToMono(String.class).block();

            JsonNode root    = mapper.readTree(searchJson);
            JsonNode results = root.path("results");
            if (!results.isArray() || results.size() == 0) return result;

            JsonNode first = results.get(0);
            String posterPath = first.path("poster_path").asText(null);
            if (posterPath != null && !posterPath.isBlank())
                result.put("posterUrl", IMAGE_BASE + posterPath);

            int movieId = first.path("id").asInt(0);
            if (movieId > 0) {
                try {
                    String creditsJson = webClient.get()
                        .uri(BASE_URL + "/movie/{id}/credits?language=es-ES", movieId)
                        .header("Authorization", "Bearer " + bearerToken)
                        .retrieve().bodyToMono(String.class).block();

                    JsonNode crew = mapper.readTree(creditsJson).path("crew");
                    String directors = StreamSupport.stream(crew.spliterator(), false)
                        .filter(m -> "Director".equals(m.path("job").asText()))
                        .map(m -> m.path("name").asText())
                        .collect(Collectors.joining(", "));
                    if (!directors.isBlank()) result.put("director", directors);
                } catch (Exception e) {
                    log.warn("[TMDB] No se pudieron obtener creditos para id {}: {}", movieId, e.getMessage());
                }
            }
        } catch (WebClientResponseException e) {
            log.error("[TMDB] Error HTTP {} buscando pelicula '{}': {}", e.getStatusCode(), title, e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[TMDB] Error buscando pelicula '{}': {}", title, e.getMessage());
        }
        return result;
    }

    public String searchSerieCover(String title) {
        if (bearerToken == null || bearerToken.isBlank()) return null;
        try {
            String json = webClient.get()
                .uri(BASE_URL + "/search/tv?query={q}&language=es-ES&page=1", title)
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve().bodyToMono(String.class).block();
            return extractPosterPath(json);
        } catch (Exception e) {
            log.error("[TMDB] Error buscando serie '{}': {}", title, e.getMessage());
            return null;
        }
    }

    /**
     * Busca portada y autor de libro en Google Books.
     * Devuelve mapa con claves "url" y "author".
     */
    public Map<String, String> searchBookDetails(String title, String author) {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            Map<String, String> found = null;
            if (author != null && !author.isBlank()) {
                found = _googleBooksSearch(
                    "intitle:" + URLEncoder.encode(title, StandardCharsets.UTF_8)
                    + "+inauthor:" + URLEncoder.encode(author, StandardCharsets.UTF_8));
                if (found == null) log.info("[Books] Sin resultado con autor, reintentando solo con titulo");
            }
            if (found == null)
                found = _googleBooksSearch("intitle:" + URLEncoder.encode(title, StandardCharsets.UTF_8));
            if (found == null)
                found = _googleBooksSearch(URLEncoder.encode(title, StandardCharsets.UTF_8));
            if (found != null) result.putAll(found);
            log.info("[Books] Resultado final para '{}': url={}, author={}", title, result.get("url"), result.get("author"));
        } catch (Exception e) {
            log.error("[Books] Error buscando libro '{}': {}", title, e.getMessage());
        }
        return result;
    }

    /** Compatibilidad con llamadas anteriores: devuelve solo la URL. */
    public String searchBookCover(String title, String author) {
        return searchBookDetails(title, author).get("url");
    }

    private Map<String, String> _googleBooksSearch(String encodedQuery) {
        try {
            String json = webClient.get()
                .uri(BOOKS_URL + "/volumes?q=" + encodedQuery + "&maxResults=3&orderBy=relevance")
                .retrieve().bodyToMono(String.class).block();
            JsonNode root  = mapper.readTree(json);
            JsonNode items = root.path("items");
            if (!items.isArray() || items.size() == 0) return null;
            for (JsonNode item : items) {
                JsonNode volumeInfo = item.path("volumeInfo");
                JsonNode img = volumeInfo.path("imageLinks");
                String url = img.path("thumbnail").asText(null);
                if (url == null) url = img.path("smallThumbnail").asText(null);
                if (url != null && !url.isBlank()) {
                    url = url.replace("http://", "https://").replaceAll("&edge=curl", "");
                    Map<String, String> entry = new LinkedHashMap<>();
                    entry.put("url", url);
                    JsonNode authorsNode = volumeInfo.path("authors");
                    if (authorsNode.isArray() && authorsNode.size() > 0) {
                        String authors = StreamSupport.stream(authorsNode.spliterator(), false)
                            .map(JsonNode::asText)
                            .collect(Collectors.joining(", "));
                        if (!authors.isBlank()) entry.put("author", authors);
                    }
                    log.info("[Books] Encontrado: url={}, author={}", url, entry.get("author"));
                    return entry;
                }
            }
        } catch (Exception e) {
            log.warn("[Books] Error en busqueda '{}': {}", encodedQuery, e.getMessage());
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
