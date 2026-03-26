package com.registrocultural.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class ComicVineService {

    private static final Logger log = LoggerFactory.getLogger(ComicVineService.class);

    @Value("${comicvine.api-key:}")
    private String apiKey;

    @Value("${comicvine.base-url:https://comicvine.gamespot.com/api}")
    private String baseUrl;

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public ComicVineService(WebClient.Builder builder) {
        this.webClient = builder
            .defaultHeader("User-Agent", "registro-cultural-app/1.0")
            .build();
    }

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[ComicVine] ADVERTENCIA: comicvine.api-key no encontrado");
        } else {
            log.info("[ComicVine] API key cargada OK");
        }
    }

    /**
     * Busca la portada de un cómic por título (preferiblemente en español).
     * Devuelve la URL de la imagen medium o null si no se encuentra.
     */
    public String searchComicCover(String title) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[ComicVine] API key vacía - no se puede buscar portada");
            return null;
        }
        try {
            String encoded = URLEncoder.encode(title, StandardCharsets.UTF_8);
            String url = baseUrl + "/issues/"
                + "?api_key=" + apiKey
                + "&format=json"
                + "&filter=name:" + encoded
                + "&field_list=name,image,issue_number,cover_date,volume"
                + "&limit=5";

            log.info("[ComicVine] Buscando cómic: '{}'", title);
            String json = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            String result = extractCoverUrl(json, title);
            log.info("[ComicVine] Resultado para '{}': {}", title,
                result != null ? result : "sin resultado");
            return result;

        } catch (WebClientResponseException e) {
            log.error("[ComicVine] Error HTTP {} buscando '{}': {}",
                e.getStatusCode(), title, e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("[ComicVine] Error buscando '{}': {}", title, e.getMessage());
            return null;
        }
    }

    /**
     * Extrae la URL de portada del JSON de Comic Vine.
     * Prioriza resultados cuyo nombre contenga el título buscado.
     */
    private String extractCoverUrl(String json, String title) throws Exception {
        if (json == null) return null;
        JsonNode root = mapper.readTree(json);

        // Comic Vine devuelve status_code 1 cuando hay resultados
        int statusCode = root.path("status_code").asInt(-1);
        if (statusCode != 1) {
            log.warn("[ComicVine] status_code inesperado: {}", statusCode);
            return null;
        }

        JsonNode results = root.path("results");
        if (!results.isArray() || results.size() == 0) return null;

        // Intentar encontrar el resultado más relevante (nombre más parecido al buscado)
        String titleLower = title.toLowerCase();
        JsonNode best = null;
        for (JsonNode issue : results) {
            String name = issue.path("name").asText("").toLowerCase();
            if (name.contains(titleLower)) {
                best = issue;
                break;
            }
        }
        // Si no hay coincidencia exacta, coger el primero
        if (best == null) best = results.get(0);

        // Extraer URL de imagen: medium_url es la mejor resolución para portadas
        JsonNode image = best.path("image");
        String coverUrl = image.path("medium_url").asText(null);
        if (coverUrl == null || coverUrl.isBlank()) {
            coverUrl = image.path("small_url").asText(null);
        }
        return (coverUrl != null && !coverUrl.isBlank()) ? coverUrl : null;
    }
}
