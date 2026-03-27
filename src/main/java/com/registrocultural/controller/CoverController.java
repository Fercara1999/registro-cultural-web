package com.registrocultural.controller;

import com.registrocultural.service.ComicVineService;
import com.registrocultural.service.TmdbService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cover")
public class CoverController {

    private final TmdbService tmdb;
    private final ComicVineService comicVine;

    public CoverController(TmdbService tmdb, ComicVineService comicVine) {
        this.tmdb = tmdb;
        this.comicVine = comicVine;
    }

    /**
     * GET /api/cover/search?type=...&title=...&extra=
     * Para libros devuelve: { "url": "...", "author": "..." }
     * Para peliculas:       { "url": "...", "director": "..." }
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, String>> search(
            @RequestParam String type,
            @RequestParam String title,
            @RequestParam(required = false) String extra) {

        Map<String, String> response = new LinkedHashMap<>();

        if (type.contains("Pel")) {
            Map<String, String> details = tmdb.searchMovieDetails(title);
            response.put("url",      details.getOrDefault("posterUrl", ""));
            response.put("director", details.getOrDefault("director", ""));
        } else if (type.contains("Serie")) {
            String url = tmdb.searchSerieCover(title);
            response.put("url", url != null ? url : "");
        } else if (type.contains("Libro")) {
            Map<String, String> details = tmdb.searchBookDetails(title, extra);
            response.put("url",    details.getOrDefault("url", ""));
            response.put("author", details.getOrDefault("author", ""));
        } else if (type.contains("mic") || type.equalsIgnoreCase("Comic") || type.equalsIgnoreCase("Cómic")) {
            String url = comicVine.searchComicCover(title);
            response.put("url", url != null ? url : "");
        }

        return ResponseEntity.ok(response);
    }
}
