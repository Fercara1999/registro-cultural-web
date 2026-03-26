package com.registrocultural.service;

import com.registrocultural.model.Entry;
import com.registrocultural.repository.EntryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EntryService {

    private final EntryRepository repo;

    @Value("${app.covers-dir}")
    private String coversDir;

    public EntryService(EntryRepository repo) {
        this.repo = repo;
    }

    public String getCoversDir() { return coversDir; }

    public List<Entry> getAll() {
        return repo.findAllByOrderByDateDescIdDesc();
    }

    public Optional<Entry> getById(Integer id) {
        return repo.findById(id);
    }

    public Entry save(Entry entry) {
        return repo.save(entry);
    }

    public void delete(Integer id) {
        repo.deleteById(id);
    }

    public List<Entry> search(String title, String type, LocalDate date) {
        boolean hasTitle = title != null && !title.isBlank();
        boolean hasType  = type  != null && !"Todo".equals(type) && !type.isBlank();
        boolean hasDate  = date  != null;

        if (hasDate) return repo.findByDateOrderByDateDesc(date);
        if (hasTitle && hasType) return repo.findByTitleAndType(title, type);
        if (hasTitle) return repo.findByTitleContainingIgnoreCaseOrderByDateDesc(title);
        if (hasType)  return repo.findByTypeContainingOrderByDateDesc(type);
        return repo.findAllByOrderByDateDescIdDesc();
    }

    public List<String> getTitleSuggestions(String type) {
        return repo.findDistinctTitlesByType(type != null ? type : "");
    }

    public String getAuthorForTitle(String title) {
        List<Entry> entries = repo.findAuthorForTitle(title);
        return entries.isEmpty() ? null : entries.get(0).getAuthor();
    }

    public String getDirectorForTitle(String title) {
        List<Entry> entries = repo.findDirectorForTitle(title);
        return entries.isEmpty() ? null : entries.get(0).getDirector();
    }

    public Entry getLastSeriesEntry(String title) {
        List<Entry> entries = repo.findLastSeriesEntry(title);
        return entries.isEmpty() ? null : entries.get(0);
    }

    public List<Entry> getCinemaMovies() {
        return repo.findCinemaMovies();
    }

    public String saveCover(MultipartFile file) throws IOException {
        Path dir = Paths.get(coversDir);
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path dest = dir.resolve(filename);
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }

    public Map<String, Object> getStats(String period) {
        List<Entry> all = repo.findAllByOrderByDateDescIdDesc();
        LocalDate now  = LocalDate.now();
        LocalDate from = switch (period != null ? period : "mes") {
            case "semana" -> now.minusDays(now.getDayOfWeek().getValue() - 1);
            case "anio"   -> now.withDayOfYear(1);
            case "todo"   -> LocalDate.of(2000, 1, 1);
            default       -> now.withDayOfMonth(1);
        };

        List<Entry> filtered = all.stream()
                .filter(e -> !e.getDate().isBefore(from))
                .collect(Collectors.toList());

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total",      filtered.size());
        stats.put("totalAll",   all.size());
        stats.put("libros",     count(filtered, "Libro"));
        stats.put("series",     count(filtered, "Serie"));
        stats.put("peliculas",  count(filtered, "Pel"));
        stats.put("teatro",     count(filtered, "Teatro"));
        stats.put("comics",     count(filtered, "mic"));
        stats.put("cine",       filtered.stream().filter(e -> Boolean.TRUE.equals(e.getSeenInCinema())).count());
        stats.put("cineTotal",  all.stream().filter(e -> Boolean.TRUE.equals(e.getSeenInCinema())).count());
        stats.put("cinemaList", repo.findCinemaMovies());

        Map<String, Long> porTipo = new LinkedHashMap<>();
        porTipo.put("Libro",    count(filtered, "Libro"));
        porTipo.put("Serie",    count(filtered, "Serie"));
        porTipo.put("Película", count(filtered, "Pel"));
        porTipo.put("Teatro",   count(filtered, "Teatro"));
        porTipo.put("Cómic",    count(filtered, "mic"));
        stats.put("porTipo", porTipo);

        Map<String, Long> porMes = new TreeMap<>(filtered.stream().collect(
                Collectors.groupingBy(
                    e -> e.getDate().getYear() + "-" + String.format("%02d", e.getDate().getMonthValue()),
                    Collectors.counting())));
        stats.put("porMes", porMes);

        String[] dias = {"Lun","Mar","Mié","Jue","Vie","Sáb","Dom"};
        long[] porDia = new long[7];
        for (Entry e : filtered) porDia[e.getDate().getDayOfWeek().getValue() - 1]++;
        stats.put("porDia",     porDia);
        stats.put("diasLabels", dias);
        stats.put("period",     period != null ? period : "mes");
        return stats;
    }

    private long count(List<Entry> list, String keyword) {
        return list.stream().filter(e -> e.getType() != null && e.getType().contains(keyword)).count();
    }
}
