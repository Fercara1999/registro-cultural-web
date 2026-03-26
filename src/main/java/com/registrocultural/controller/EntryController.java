package com.registrocultural.controller;

import com.registrocultural.model.Entry;
import com.registrocultural.service.EntryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class EntryController {

    private final EntryService service;
    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("dd/MM");

    public EntryController(EntryService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(Model model) {
        List<Entry> all = service.getAllNonPending();
        List<Entry> recent = all.stream().limit(10).collect(Collectors.toList());
        LocalDate today  = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = today.with(DayOfWeek.SUNDAY);
        List<Entry> week = all.stream()
            .filter(e -> e.getDate() != null && !e.getDate().isBefore(monday) && !e.getDate().isAfter(sunday))
            .collect(Collectors.toList());
        model.addAttribute("recentEntries", recent);
        model.addAttribute("weekEntries",   week);
        model.addAttribute("weekStart",     monday.format(LABEL_FMT));
        model.addAttribute("weekEnd",       sunday.format(LABEL_FMT));
        return "home";
    }

    @GetMapping("/registrar")
    public String registrarForm(Model model) {
        model.addAttribute("entries", service.getAllNonPending());
        model.addAttribute("newEntry", new Entry());
        return "index";
    }

    @PostMapping("/registrar")
    public String registrar(
            @RequestParam String title,
            @RequestParam String type,
            @RequestParam(required = false) String description,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Integer chapters,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer season,
            @RequestParam(required = false) Integer episode,
            @RequestParam(required = false) String venue,
            @RequestParam(required = false) String director,
            @RequestParam(required = false) Boolean seenInCinema,
            @RequestParam(required = false) Boolean isSingleVolume,
            @RequestParam(required = false) Integer comicVolume,
            @RequestParam(required = false) Integer comicIssue,
            @RequestParam(required = false) Boolean finished,
            @RequestParam(required = false) Boolean seasonFinished,
            @RequestParam(required = false) Boolean seriesFinished,
            @RequestParam(required = false) MultipartFile cover,
            @RequestParam(required = false) String autoCoverUrl,
            RedirectAttributes ra) {

        Entry entry = buildEntry(title, type, description, date != null ? date : LocalDate.now(),
            rating, chapters, author, season, episode, venue, director,
            seenInCinema, isSingleVolume, comicVolume, comicIssue, finished, seasonFinished, seriesFinished);
        entry.setPending(false);
        saveCoverToEntry(entry, cover, autoCoverUrl, ra);
        service.save(entry);
        ra.addFlashAttribute("success", "✅ Entrada registrada correctamente");
        return "redirect:/registrar";
    }

    // ── PENDIENTES ────────────────────────────────────────────

    @GetMapping("/pendientes")
    public String pendientes(Model model) {
        model.addAttribute("pendingEntries", service.getAllPending());
        return "pendientes";
    }

    @PostMapping("/pendientes/guardar")
    public String guardarPendiente(
            @RequestParam String title,
            @RequestParam String type,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String director,
            @RequestParam(required = false) String venue,
            @RequestParam(required = false) MultipartFile cover,
            @RequestParam(required = false) String autoCoverUrl,
            RedirectAttributes ra) {

        Entry entry = new Entry();
        entry.setTitle(title.trim());
        entry.setType(type);
        entry.setDescription(description);
        entry.setDate(LocalDate.now());
        entry.setAuthor(author);
        entry.setDirector(director);
        entry.setVenue(venue);
        entry.setPending(true);
        saveCoverToEntry(entry, cover, autoCoverUrl, ra);
        service.save(entry);
        ra.addFlashAttribute("success", "⏳ Pendiente añadido correctamente");
        return "redirect:/pendientes";
    }

    @PostMapping("/pendientes/marcar-visto/{id}")
    public String marcarVisto(@PathVariable Integer id, RedirectAttributes ra) {
        service.getById(id).ifPresent(e -> {
            e.setPending(false);
            e.setDate(LocalDate.now());
            service.save(e);
        });
        ra.addFlashAttribute("success", "✅ Marcado como visto y movido a registros");
        return "redirect:/pendientes";
    }

    // ── EDITAR / ELIMINAR ─────────────────────────────────────────

    @GetMapping("/editar/{id}")
    public String editarForm(@PathVariable Integer id, Model model) {
        return service.getById(id).map(e -> {
            model.addAttribute("entry", e);
            return "editar";
        }).orElse("redirect:/home");
    }

    @PostMapping("/editar/{id}")
    public String editarSave(
            @PathVariable Integer id,
            @RequestParam String title,
            @RequestParam String type,
            @RequestParam(required = false) String description,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Integer chapters,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer season,
            @RequestParam(required = false) Integer episode,
            @RequestParam(required = false) String venue,
            @RequestParam(required = false) String director,
            @RequestParam(required = false) Boolean seenInCinema,
            @RequestParam(required = false) Boolean isSingleVolume,
            @RequestParam(required = false) Integer comicVolume,
            @RequestParam(required = false) Integer comicIssue,
            @RequestParam(required = false) Boolean finished,
            @RequestParam(required = false) Boolean seasonFinished,
            @RequestParam(required = false) Boolean seriesFinished,
            @RequestParam(required = false) MultipartFile cover,
            @RequestParam(required = false) String autoCoverUrl,
            RedirectAttributes ra) {

        service.getById(id).ifPresent(entry -> {
            entry.setTitle(title.trim());
            entry.setType(type);
            entry.setDescription(description);
            entry.setDate(date != null ? date : LocalDate.now());
            entry.setRating(rating);
            entry.setChapters(chapters);
            entry.setAuthor(author);
            entry.setSeason(season);
            entry.setEpisode(episode);
            entry.setVenue(venue);
            entry.setDirector(director);
            entry.setSeenInCinema(Boolean.TRUE.equals(seenInCinema));
            entry.setIsSingleVolume(Boolean.TRUE.equals(isSingleVolume));
            entry.setComicVolume(Boolean.TRUE.equals(isSingleVolume) ? null : comicVolume);
            entry.setComicIssue(Boolean.TRUE.equals(isSingleVolume) ? null : comicIssue);
            entry.setFinished(Boolean.TRUE.equals(isSingleVolume) ? null : finished);
            entry.setSeasonFinished(seasonFinished);
            entry.setSeriesFinished(Boolean.TRUE.equals(isSingleVolume) ? null : seriesFinished);
            if (cover != null && !cover.isEmpty()) {
                try { entry.setCoverPath(service.saveCover(cover)); } catch (IOException e) { /* log */ }
            } else if (autoCoverUrl != null && !autoCoverUrl.isBlank()) {
                try { entry.setCoverPath(downloadRemoteCover(autoCoverUrl, service.getCoversDir())); } catch (IOException e) { /* sin portada */ }
            }
            service.save(entry);
        });
        ra.addFlashAttribute("success", "✅ Registro actualizado");
        return "redirect:/home";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes ra) {
        service.delete(id);
        ra.addFlashAttribute("success", "🗑️ Registro eliminado");
        return "redirect:/home";
    }

    @GetMapping("/buscar")
    public String buscar(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        List<Entry> results = service.search(title, type, date);
        model.addAttribute("entries",     results);
        model.addAttribute("searchTitle", title);
        model.addAttribute("searchType",  type);
        model.addAttribute("searchDate",  date);
        return "buscar";
    }

    @GetMapping("/estadisticas")
    public String estadisticas(
            @RequestParam(required = false, defaultValue = "mes") String period,
            @RequestParam(required = false, defaultValue = "Todos") String tipo,
            Model model) {
        model.addAllAttributes(service.getStats(period, tipo));
        model.addAttribute("selectedTipo", tipo);
        return "estadisticas";
    }

    @GetMapping("/covers/{filename}")
    @ResponseBody
    public org.springframework.core.io.Resource serveFile(@PathVariable String filename) throws IOException {
        java.nio.file.Path file = java.nio.file.Paths.get(service.getCoversDir()).resolve(filename);
        return new org.springframework.core.io.UrlResource(file.toUri());
    }

    /**
     * Endpoint de migración: recorre todas las entradas cuyo coverPath sea una URL
     * remota (empieza por "http") y las re-descarga localmente con el método corregido.
     * Usar una sola vez: GET /admin/fix-covers
     */
    @GetMapping("/admin/fix-covers")
    @ResponseBody
    public ResponseEntity<String> fixCovers() {
        List<Entry> all = service.getAll();
        int fixed = 0, failed = 0;
        for (Entry entry : all) {
            String cp = entry.getCoverPath();
            if (cp != null && cp.startsWith("http")) {
                try {
                    String localFile = downloadRemoteCover(cp, service.getCoversDir());
                    entry.setCoverPath(localFile);
                    service.save(entry);
                    fixed++;
                } catch (IOException e) {
                    failed++;
                }
            }
        }
        return ResponseEntity.ok("fix-covers completado: " + fixed + " arregladas, " + failed + " fallidas.");
    }

    // ── Helpers ───────────────────────────────────────────────────

    private Entry buildEntry(String title, String type, String description, LocalDate date,
            Integer rating, Integer chapters, String author, Integer season, Integer episode,
            String venue, String director, Boolean seenInCinema, Boolean isSingleVolume,
            Integer comicVolume, Integer comicIssue, Boolean finished, Boolean seasonFinished, Boolean seriesFinished) {
        Entry e = new Entry();
        e.setTitle(title.trim()); e.setType(type); e.setDescription(description); e.setDate(date);
        e.setRating(rating); e.setChapters(chapters); e.setAuthor(author);
        e.setSeason(season); e.setEpisode(episode); e.setVenue(venue); e.setDirector(director);
        e.setSeenInCinema(Boolean.TRUE.equals(seenInCinema));
        e.setIsSingleVolume(Boolean.TRUE.equals(isSingleVolume));
        e.setComicVolume(Boolean.TRUE.equals(isSingleVolume) ? null : comicVolume);
        e.setComicIssue(Boolean.TRUE.equals(isSingleVolume) ? null : comicIssue);
        e.setFinished(Boolean.TRUE.equals(isSingleVolume) ? null : finished);
        e.setSeasonFinished(seasonFinished);
        e.setSeriesFinished(Boolean.TRUE.equals(isSingleVolume) ? null : seriesFinished);
        return e;
    }

    private void saveCoverToEntry(Entry entry, MultipartFile cover, String autoCoverUrl, RedirectAttributes ra) {
        if (cover != null && !cover.isEmpty()) {
            try { entry.setCoverPath(service.saveCover(cover)); }
            catch (IOException e) { if (ra != null) ra.addFlashAttribute("error", "No se pudo guardar la portada"); }
        } else if (autoCoverUrl != null && !autoCoverUrl.isBlank()) {
            try { entry.setCoverPath(downloadRemoteCover(autoCoverUrl, service.getCoversDir())); }
            catch (IOException e) { /* sin portada */ }
        }
    }

    private String downloadRemoteCover(String imageUrl, String coversDir) throws IOException {
        Path dir = Paths.get(coversDir);
        Files.createDirectories(dir);

        HttpURLConnection conn = (HttpURLConnection) new URL(imageUrl).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(10000);
        conn.connect();

        String ext = ".jpg";
        String contentType = conn.getContentType();
        if (contentType != null) {
            if (contentType.contains("png"))       ext = ".png";
            else if (contentType.contains("webp")) ext = ".webp";
            else if (contentType.contains("gif"))  ext = ".gif";
            else                                   ext = ".jpg";
        } else {
            String path = imageUrl.split("[?#]")[0];
            if (path.contains(".")) {
                String candidate = path.substring(path.lastIndexOf('.'));
                if (candidate.length() <= 5) ext = candidate;
            }
        }

        String filename = UUID.randomUUID() + ext;
        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        }
        return filename;
    }
}
