package com.registrocultural.controller;

import com.registrocultural.model.Entry;
import com.registrocultural.service.EntryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
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

    @GetMapping("/home")
    public String home(Model model) {
        List<Entry> all = service.getAll();

        // Últimos 10 registros (ya vienen ordenados por fecha desc desde el servicio)
        List<Entry> recent = all.stream().limit(10).collect(Collectors.toList());

        // Semana actual: lunes-domingo de la semana en curso
        LocalDate today   = LocalDate.now();
        LocalDate monday  = today.with(DayOfWeek.MONDAY);
        LocalDate sunday  = today.with(DayOfWeek.SUNDAY);

        List<Entry> week = all.stream()
            .filter(e -> e.getDate() != null
                && !e.getDate().isBefore(monday)
                && !e.getDate().isAfter(sunday))
            .collect(Collectors.toList());

        model.addAttribute("recentEntries", recent);
        model.addAttribute("weekEntries",   week);
        model.addAttribute("weekStart",     monday.format(LABEL_FMT));
        model.addAttribute("weekEnd",       sunday.format(LABEL_FMT));
        return "home";
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("entries", service.getAll());
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

        Entry entry = new Entry();
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
            try { entry.setCoverPath(service.saveCover(cover)); }
            catch (IOException e) { ra.addFlashAttribute("error", "No se pudo guardar la portada"); }
        } else if (autoCoverUrl != null && !autoCoverUrl.isBlank()) {
            try {
                String filename = downloadRemoteCover(autoCoverUrl, service.getCoversDir());
                entry.setCoverPath(filename);
            } catch (IOException e) { /* sin portada */ }
        }

        service.save(entry);
        ra.addFlashAttribute("success", "✅ Entrada registrada correctamente");
        return "redirect:/";
    }

    @GetMapping("/editar/{id}")
    public String editarForm(@PathVariable Integer id, Model model) {
        return service.getById(id).map(e -> {
            model.addAttribute("entry", e);
            return "editar";
        }).orElse("redirect:/");
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
                try {
                    String filename = downloadRemoteCover(autoCoverUrl, service.getCoversDir());
                    entry.setCoverPath(filename);
                } catch (IOException e) { /* sin portada */ }
            }
            service.save(entry);
        });
        ra.addFlashAttribute("success", "✅ Registro actualizado");
        return "redirect:/";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes ra) {
        service.delete(id);
        ra.addFlashAttribute("success", "🗑️ Registro eliminado");
        return "redirect:/";
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
    public String estadisticas(@RequestParam(required = false, defaultValue = "mes") String period, Model model) {
        model.addAllAttributes(service.getStats(period));
        return "estadisticas";
    }

    @GetMapping("/covers/{filename}")
    @ResponseBody
    public org.springframework.core.io.Resource serveFile(@PathVariable String filename) throws IOException {
        java.nio.file.Path file = java.nio.file.Paths.get(service.getCoversDir()).resolve(filename);
        return new org.springframework.core.io.UrlResource(file.toUri());
    }

    private String downloadRemoteCover(String imageUrl, String coversDir) throws IOException {
        Path dir = Paths.get(coversDir);
        Files.createDirectories(dir);
        String ext = imageUrl.contains(".") ? imageUrl.substring(imageUrl.lastIndexOf('.')) : ".jpg";
        if (ext.length() > 5) ext = ".jpg";
        String filename = UUID.randomUUID() + ext;
        Path dest = dir.resolve(filename);
        try (InputStream in = new URL(imageUrl).openStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        return filename;
    }
}
