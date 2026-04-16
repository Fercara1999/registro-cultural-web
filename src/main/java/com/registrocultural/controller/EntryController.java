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
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class EntryController {

    private final EntryService service;
    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("dd/MM");

    private static final Map<String, String> KPI_LABELS = Map.ofEntries(
        Map.entry("total",            "Todos los registros"),
        Map.entry("capitulosSeries",  "Capítulos de series"),
        Map.entry("capitulosLibros",  "Capítulos de libros"),
        Map.entry("peliculas",        "Películas"),
        Map.entry("comics",           "Cómics"),
        Map.entry("teatro",           "Teatro"),
        Map.entry("cine",             "Vistos en el cine"),
        Map.entry("librosTerminados", "Libros terminados"),
        Map.entry("librosEnCurso",    "Libros en curso"),
        Map.entry("seriesTerminadas", "Series terminadas"),
        Map.entry("seriesEnCurso",    "Series en curso"),
        Map.entry("pelisEnCine",      "Películas en el cine"),
        Map.entry("pelisEnCasa",      "Películas en casa"),
        Map.entry("comicsTerminados", "Cómics terminados"),
        Map.entry("comicsEnCurso",    "Cómics en curso")
    );

    private static final Map<String, String> PERIOD_LABELS = Map.of(
        "semana",       "esta semana",
        "ultimaSemana", "la última semana",
        "mes",          "este mes",
        "ultimoMes",    "el último mes",
        "anio",         "este año",
        "todo",         "todo el tiempo"
    );

    public EntryController(EntryService service) { this.service = service; }

    @GetMapping("/")
    public String root() { return "redirect:/home"; }

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
        List<Entry> last25 = service.getAllNonPending().stream().limit(25).collect(Collectors.toList());
        model.addAttribute("entries", last25);
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
            @RequestParam(name = "chapters", required = false) String chaptersRaw,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer season,
            @RequestParam(required = false) String episode,
            @RequestParam(required = false) String venue,
            @RequestParam(required = false) String director,
            @RequestParam(required = false) Boolean seenInCinema,
            @RequestParam(required = false) Boolean isSingleVolume,
            @RequestParam(required = false) Integer comicVolume,
            @RequestParam(required = false) String comicIssue,
            @RequestParam(required = false) Boolean finished,
            @RequestParam(required = false) Boolean seasonFinished,
            @RequestParam(required = false) Boolean seriesFinished,
            @RequestParam(required = false) MultipartFile cover,
            @RequestParam(required = false) String autoCoverUrl,
            @RequestParam(required = false) String existingCoverPath,
            RedirectAttributes ra) {

        LocalDate d = date != null ? date : LocalDate.now();
        List<Integer> episodes     = parseIssueRange(episode);
        List<Integer> issues       = parseIssueRange(comicIssue);
        List<Integer> chaptersList = parseIssueRange(chaptersRaw);

        boolean multiEpisode = episodes.size() > 1;
        boolean multiIssue   = issues.size()   > 1;
        boolean multiChapter = chaptersList.size() > 1;

        if (!multiEpisode && !multiIssue && !multiChapter) {
            Integer epInt       = episodes.isEmpty()     ? null : episodes.get(0);
            Integer issueInt    = issues.isEmpty()       ? null : issues.get(0);
            Integer chaptersInt = chaptersList.isEmpty() ? null : chaptersList.get(0);
            Entry entry = buildEntry(title, type, description, d, rating, chaptersInt, author,
                season, epInt, venue, director, seenInCinema, isSingleVolume,
                comicVolume, issueInt, finished, seasonFinished, seriesFinished);
            entry.setPending(false);
            applyCover(entry, cover, autoCoverUrl, existingCoverPath, ra);
            service.save(entry);
            ra.addFlashAttribute("success", "✅ Entrada registrada correctamente");

        } else {
            String coverPath = resolveCoverPath(cover, autoCoverUrl, existingCoverPath);

            if (multiChapter) {
                int lastIdx = chaptersList.size() - 1;
                for (int i = 0; i < chaptersList.size(); i++) {
                    boolean isLast = (i == lastIdx);
                    Entry entry = buildEntry(title, type, description, d,
                        isLast ? rating : null, chaptersList.get(i), author,
                        season, episodes.isEmpty() ? null : episodes.get(0),
                        venue, director, seenInCinema, isSingleVolume,
                        comicVolume, issues.isEmpty() ? null : issues.get(0),
                        isLast ? finished : null, isLast ? seasonFinished : null, isLast ? seriesFinished : null);
                    entry.setPending(false);
                    entry.setCoverPath(coverPath);
                    service.save(entry);
                }
                ra.addFlashAttribute("success", "✅ " + chaptersList.size() + " registros creados (cap. " +
                    chaptersList.get(0) + "–" + chaptersList.get(lastIdx) + ")");

            } else if (multiEpisode) {
                int lastIdx = episodes.size() - 1;
                for (int i = 0; i < episodes.size(); i++) {
                    boolean isLast = (i == lastIdx);
                    Integer chaptersInt = chaptersList.isEmpty() ? null : chaptersList.get(0);
                    Entry entry = buildEntry(title, type, description, d,
                        isLast ? rating : null, chaptersInt, author,
                        season, episodes.get(i), venue, director, seenInCinema, isSingleVolume,
                        comicVolume, issues.isEmpty() ? null : issues.get(0),
                        isLast ? finished : null, isLast ? seasonFinished : null, isLast ? seriesFinished : null);
                    entry.setPending(false);
                    entry.setCoverPath(coverPath);
                    service.save(entry);
                }
                ra.addFlashAttribute("success", "✅ " + episodes.size() + " registros creados (cap. " +
                    episodes.get(0) + "–" + episodes.get(lastIdx) + ")");

            } else {
                int lastIdx = issues.size() - 1;
                for (int i = 0; i < issues.size(); i++) {
                    boolean isLast = (i == lastIdx);
                    Integer chaptersInt = chaptersList.isEmpty() ? null : chaptersList.get(0);
                    Entry entry = buildEntry(title, type, description, d,
                        isLast ? rating : null, chaptersInt, author,
                        season, episodes.isEmpty() ? null : episodes.get(0),
                        venue, director, seenInCinema, isSingleVolume,
                        comicVolume, issues.get(i),
                        isLast ? finished : null, isLast ? seasonFinished : null, isLast ? seriesFinished : null);
                    entry.setPending(false);
                    entry.setCoverPath(coverPath);
                    service.save(entry);
                }
                ra.addFlashAttribute("success", "✅ " + issues.size() + " registros creados (nº " +
                    issues.get(0) + "–" + issues.get(lastIdx) + ")");
            }
        }
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
            @RequestParam(required = false) String existingCoverPath,
            RedirectAttributes ra) {

        Entry entry = new Entry();
        entry.setTitle(title.trim()); entry.setType(type); entry.setDescription(description);
        entry.setDate(LocalDate.now()); entry.setAuthor(author); entry.setDirector(director);
        entry.setVenue(venue); entry.setPending(true);
        applyCover(entry, cover, autoCoverUrl, existingCoverPath, ra);
        service.save(entry);
        ra.addFlashAttribute("success", "⏳ Pendiente añadido correctamente");
        return "redirect:/pendientes";
    }

    @PostMapping("/pendientes/marcar-visto/{id}")
    public String marcarVisto(@PathVariable String id, RedirectAttributes ra) {
        service.getById(id).ifPresent(e -> {
            e.setPending(false); e.setDate(LocalDate.now()); service.save(e);
        });
        ra.addFlashAttribute("success", "✅ Marcado como visto y movido a registros");
        return "redirect:/pendientes";
    }

    // ── EDITAR / ELIMINAR ───────────────────────────────────

    @GetMapping("/editar/{id}")
    public String editarForm(@PathVariable String id, Model model) {
        return service.getById(id).map(e -> { model.addAttribute("entry", e); return "editar"; })
                      .orElse("redirect:/home");
    }

    @PostMapping("/editar/{id}")
    public String editarSave(
            @PathVariable String id,
            @RequestParam String title,
            @RequestParam String type,
            @RequestParam(required = false) String description,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Integer chapters,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer season,
            @RequestParam(required = false) String episode,
            @RequestParam(required = false) String venue,
            @RequestParam(required = false) String director,
            @RequestParam(required = false) Boolean seenInCinema,
            @RequestParam(required = false) Boolean isSingleVolume,
            @RequestParam(required = false) Integer comicVolume,
            @RequestParam(required = false) String comicIssue,
            @RequestParam(required = false) Boolean finished,
            @RequestParam(required = false) Boolean seasonFinished,
            @RequestParam(required = false) Boolean seriesFinished,
            @RequestParam(required = false) MultipartFile cover,
            @RequestParam(required = false) String autoCoverUrl,
            RedirectAttributes ra) {

        List<Integer> episodes = parseIssueRange(episode);
        List<Integer> issues   = parseIssueRange(comicIssue);
        Integer epInt    = episodes.isEmpty() ? null : episodes.get(0);
        Integer issueInt = issues.isEmpty()   ? null : issues.get(0);

        service.getById(id).ifPresent(entry -> {
            entry.setTitle(title.trim()); entry.setType(type); entry.setDescription(description);
            entry.setDate(date != null ? date : LocalDate.now());
            entry.setRating(rating); entry.setChapters(chapters); entry.setAuthor(author);
            entry.setSeason(season); entry.setEpisode(epInt);
            entry.setVenue(venue); entry.setDirector(director);
            entry.setSeenInCinema(Boolean.TRUE.equals(seenInCinema));
            entry.setIsSingleVolume(Boolean.TRUE.equals(isSingleVolume));
            entry.setComicVolume(Boolean.TRUE.equals(isSingleVolume) ? null : comicVolume);
            entry.setComicIssue(issueInt);
            entry.setFinished(Boolean.TRUE.equals(finished));
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
    public String eliminar(@PathVariable String id, RedirectAttributes ra) {
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
        model.addAttribute("entries", results); model.addAttribute("searchTitle", title);
        model.addAttribute("searchType", type); model.addAttribute("searchDate", date);
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

    @GetMapping("/estadisticas/detalle")
    public String estadisticasDetalle(
            @RequestParam(required = false, defaultValue = "mes") String period,
            @RequestParam(required = false, defaultValue = "Todos") String tipo,
            @RequestParam(required = false, defaultValue = "total") String kpi,
            Model model) {
        List<Entry> entries = service.getDetalleRegistros(period, tipo, kpi);
        String titulo = KPI_LABELS.getOrDefault(kpi, kpi);
        String periodoLabel = PERIOD_LABELS.getOrDefault(period, period);
        model.addAttribute("entries",      entries);
        model.addAttribute("detalleTitle", titulo + " — " + periodoLabel);
        model.addAttribute("period",       period);
        model.addAttribute("tipo",         tipo);
        model.addAttribute("kpi",          kpi);
        model.addAttribute("backUrl",      "/estadisticas?period=" + period + "&tipo=" + tipo);
        return "detalle-stats";
    }

    @GetMapping("/covers/{filename}")
    @ResponseBody
    public org.springframework.core.io.Resource serveFile(@PathVariable String filename) throws IOException {
        java.nio.file.Path file = java.nio.file.Paths.get(service.getCoversDir()).resolve(filename);
        return new org.springframework.core.io.UrlResource(file.toUri());
    }

    @GetMapping("/admin/fix-covers")
    @ResponseBody
    public ResponseEntity<String> fixCovers() {
        List<Entry> all = service.getAll();
        int fixed = 0, failed = 0;
        for (Entry entry : all) {
            String cp = entry.getCoverPath();
            if (cp != null && cp.startsWith("http")) {
                try { entry.setCoverPath(downloadRemoteCover(cp, service.getCoversDir())); service.save(entry); fixed++; }
                catch (IOException e) { failed++; }
            }
        }
        return ResponseEntity.ok("fix-covers: " + fixed + " arregladas, " + failed + " fallidas.");
    }

    @GetMapping("/api/entry/hint")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> entryHint(
            @RequestParam(required = false, defaultValue = "") String title,
            @RequestParam(required = false, defaultValue = "") String type,
            @RequestParam(required = false) Integer season) {

        Map<String, Object> result = new LinkedHashMap<>();

        if (type.contains("Serie") || type.contains("Libro") || type.contains("mic")) {
            List<String> titles = service.getAll().stream()
                .filter(e -> !e.isPending())
                .filter(e -> e.getType() != null && e.getType().contains(
                    type.contains("Serie") ? "Serie" : type.contains("Libro") ? "Libro" : "mic"))
                .map(Entry::getTitle).filter(t -> t != null && !t.isBlank()).distinct()
                .filter(t -> title.isBlank() || t.toLowerCase().contains(title.toLowerCase()))
                .sorted().limit(10).collect(Collectors.toList());
            result.put("titles", titles);
        }

        if (title.isBlank()) return ResponseEntity.ok(result);
        String titleTrimmed = title.trim();
        String titleLower   = titleTrimmed.toLowerCase();

        service.getAll().stream()
            .filter(e -> !e.isPending())
            .filter(e -> e.getTitle() != null && e.getTitle().trim().toLowerCase().equals(titleLower))
            .filter(e -> e.getCoverPath() != null && !e.getCoverPath().isBlank())
            .findFirst()
            .ifPresent(e -> result.put("coverLocalPath", e.getCoverPath()));

        if (type.contains("Serie")) {
            Entry last = service.getLastSeriesEntry(titleTrimmed);
            if (last == null) {
                last = service.getAll().stream()
                    .filter(e -> !e.isPending())
                    .filter(e -> e.getType() != null && e.getType().contains("Serie"))
                    .filter(e -> e.getTitle() != null && e.getTitle().trim().toLowerCase().equals(titleLower))
                    .filter(e -> e.getSeason() != null && e.getEpisode() != null)
                    .max(Comparator.comparingInt(Entry::getSeason).thenComparingInt(Entry::getEpisode))
                    .orElse(null);
            }
            if (last != null) {
                boolean done = Boolean.TRUE.equals(last.getSeasonFinished()) || Boolean.TRUE.equals(last.getSeriesFinished());
                int targetSeason = (season != null) ? season : (done ? last.getSeason() + 1 : last.getSeason());
                if (season != null) {
                    Entry lastInSeason = service.getAll().stream()
                        .filter(e -> !e.isPending())
                        .filter(e -> e.getType() != null && e.getType().contains("Serie"))
                        .filter(e -> e.getTitle() != null && e.getTitle().trim().toLowerCase().equals(titleLower))
                        .filter(e -> e.getSeason() != null && e.getSeason().equals(season) && e.getEpisode() != null)
                        .max(Comparator.comparingInt(Entry::getEpisode))
                        .orElse(null);
                    if (lastInSeason != null) {
                        boolean seasonDone = Boolean.TRUE.equals(lastInSeason.getSeasonFinished()) || Boolean.TRUE.equals(lastInSeason.getSeriesFinished());
                        result.put("season",  seasonDone ? lastInSeason.getSeason() + 1 : lastInSeason.getSeason());
                        result.put("episode", seasonDone ? 1 : lastInSeason.getEpisode() + 1);
                    } else {
                        result.put("season", season);
                        result.put("episode", 1);
                    }
                } else {
                    result.put("season",  done ? last.getSeason() + 1 : last.getSeason());
                    result.put("episode", done ? 1 : last.getEpisode() + 1);
                }
            }

        } else if (type.contains("Libro")) {
            String author = service.getAuthorForTitle(titleTrimmed);
            if (author != null && !author.isBlank()) result.put("author", author);
            service.getAll().stream()
                .filter(e -> !e.isPending())
                .filter(e -> e.getType() != null && e.getType().contains("Libro"))
                .filter(e -> e.getTitle() != null && e.getTitle().trim().toLowerCase().equals(titleLower))
                .filter(e -> e.getChapters() != null)
                .max(Comparator.comparingInt(Entry::getChapters))
                .ifPresent(last -> result.put("chapters", last.getChapters() + 1));

        } else if (type.contains("mic")) {
            List<Entry> comicEntries = service.getAll().stream()
                .filter(e -> !e.isPending())
                .filter(e -> e.getType() != null && e.getType().contains("mic"))
                .filter(e -> e.getTitle() != null && e.getTitle().trim().toLowerCase().equals(titleLower))
                .collect(Collectors.toList());

            Optional<Entry> lastVolOpt = comicEntries.stream()
                .filter(e -> e.getComicVolume() != null)
                .max(Comparator.comparingInt(Entry::getComicVolume));

            if (lastVolOpt.isPresent()) {
                Entry lastVol = lastVolOpt.get();
                boolean tomoTerminado = Boolean.TRUE.equals(lastVol.getFinished());
                int nextVolume = tomoTerminado ? lastVol.getComicVolume() + 1 : lastVol.getComicVolume();
                result.put("comicVolume", nextVolume);
                if (tomoTerminado) {
                    result.put("comicIssue", 1);
                } else {
                    comicEntries.stream()
                        .filter(e -> e.getComicVolume() != null && e.getComicVolume().equals(lastVol.getComicVolume()))
                        .filter(e -> e.getComicIssue() != null)
                        .max(Comparator.comparingInt(Entry::getComicIssue))
                        .ifPresent(lastIssue -> result.put("comicIssue", lastIssue.getComicIssue() + 1));
                }
            } else {
                comicEntries.stream()
                    .filter(e -> e.getComicIssue() != null)
                    .max(Comparator.comparingInt(Entry::getComicIssue))
                    .ifPresent(last -> result.put("comicIssue", last.getComicIssue() + 1));
            }
        }

        return ResponseEntity.ok(result);
    }

    // ── Helpers ──────────────────────────────────────────

    private List<Integer> parseIssueRange(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();
        raw = raw.trim();
        if (raw.matches("\\d+-\\d+")) {
            String[] parts = raw.split("-");
            int from = Integer.parseInt(parts[0]);
            int to   = Integer.parseInt(parts[1]);
            if (from > to || to - from > 100) return List.of(from);
            List<Integer> list = new ArrayList<>();
            for (int i = from; i <= to; i++) list.add(i);
            return list;
        }
        if (raw.contains(",")) {
            return Arrays.stream(raw.split(","))
                .map(String::trim).filter(s -> s.matches("\\d+"))
                .map(Integer::parseInt).collect(Collectors.toList());
        }
        if (raw.matches("\\d+")) return List.of(Integer.parseInt(raw));
        return Collections.emptyList();
    }

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
        e.setComicIssue(comicIssue);
        e.setFinished(Boolean.TRUE.equals(finished));
        e.setSeasonFinished(seasonFinished);
        e.setSeriesFinished(Boolean.TRUE.equals(isSingleVolume) ? null : seriesFinished);
        return e;
    }

    private String resolveCoverPath(MultipartFile cover, String autoCoverUrl, String existingCoverPath) {
        if (cover != null && !cover.isEmpty()) {
            try { return service.saveCover(cover); } catch (IOException e) { /* sin portada */ }
        }
        if (autoCoverUrl != null && !autoCoverUrl.isBlank()) {
            try { return downloadRemoteCover(autoCoverUrl, service.getCoversDir()); } catch (IOException e) { /* sin portada */ }
        }
        if (existingCoverPath != null && !existingCoverPath.isBlank()) return existingCoverPath;
        return null;
    }

    private void applyCover(Entry entry, MultipartFile cover, String autoCoverUrl, String existingCoverPath, RedirectAttributes ra) {
        String path = resolveCoverPath(cover, autoCoverUrl, existingCoverPath);
        if (path != null) entry.setCoverPath(path);
    }

    private String downloadRemoteCover(String imageUrl, String coversDir) throws IOException {
        Path dir = Paths.get(coversDir);
        Files.createDirectories(dir);
        HttpURLConnection conn = (HttpURLConnection) new URL(imageUrl).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setConnectTimeout(8000); conn.setReadTimeout(10000); conn.connect();
        String ext = ".jpg";
        String contentType = conn.getContentType();
        if (contentType != null) {
            if (contentType.contains("png"))       ext = ".png";
            else if (contentType.contains("webp")) ext = ".webp";
            else if (contentType.contains("gif"))  ext = ".gif";
        } else {
            String path = imageUrl.split("[?#]")[0];
            if (path.contains(".")) { String c = path.substring(path.lastIndexOf('.')); if (c.length() <= 5) ext = c; }
        }
        String filename = UUID.randomUUID() + ext;
        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        }
        return filename;
    }
}
