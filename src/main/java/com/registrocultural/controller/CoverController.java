package com.registrocultural.controller;

import com.registrocultural.service.ComicVineService;
import com.registrocultural.service.TmdbService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint REST que el frontend llama para obtener la URL de portada
 * automaticamente segun el tipo y titulo.
 */
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
     * GET /api/cover/search?type=Pelicula&title=Oppenheimer&extra=Christopher+Nolan
     * Devuelve: { "url": "https://..." } o { "url": "" }
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
        } else if (type.contains("mic") || type.equalsIgnoreCase("Comic") || type.equalsIgnoreCase("C\u00f3mic")) {
            url = comicVine.searchComicCover(title);
        }
        // Teatro: sin API fiable, devuelve vacio

        return ResponseEntity.ok(Map.of("url", url != null ? url : ""));
    }
}
