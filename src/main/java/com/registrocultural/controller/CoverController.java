package com.registrocultural.controller;

import com.registrocultural.service.TmdbService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint REST que el frontend llama para obtener la URL de portada
 * automáticamente según el tipo y título.
 */
@RestController
@RequestMapping("/api/cover")
public class CoverController {

    private final TmdbService tmdb;

    public CoverController(TmdbService tmdb) {
        this.tmdb = tmdb;
    }

    /**
     * GET /api/cover/search?type=Película&title=Oppenheimer&extra=Christopher+Nolan
     * Devuelve: { "url": "https://image.tmdb.org/..." } o { "url": null }
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, String>> search(
            @RequestParam String type,
            @RequestParam String title,
            @RequestParam(required = false) String extra) {

        String url = null;

        if (type.contains("Pel")) {
            url = tmdb.searchMovieCover(title);
        } else if (type.contains("Serie")) {
            url = tmdb.searchSerieCover(title);
        } else if (type.contains("Libro")) {
            url = tmdb.searchBookCover(title, extra);
        }
        // Teatro y Cómic: sin API fiable, devuelve null

        return ResponseEntity.ok(Map.of("url", url != null ? url : ""));
    }
}
