package com.registrocultural.service;

import com.registrocultural.model.Entry;
import com.registrocultural.repository.EntryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EntryService {

    private static final int    COVER_MAX_W   = 300;
    private static final int    COVER_MAX_H   = 450;
    private static final float  COVER_QUALITY = 0.75f;

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

    public List<Entry> getAllNonPending() {
        return repo.findAllByOrderByDateDescIdDesc().stream()
            .filter(e -> !e.isPending())
            .collect(Collectors.toList());
    }

    public List<Entry> getAllPending() {
        return repo.findAllByOrderByDateDescIdDesc().stream()
            .filter(Entry::isPending)
            .collect(Collectors.toList());
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
        if (hasDate) return repo.findByDateOrderByDateDesc(date).stream().filter(e -> !e.isPending()).collect(Collectors.toList());
        if (hasTitle && hasType) return repo.findByTitleAndType(title, type).stream().filter(e -> !e.isPending()).collect(Collectors.toList());
        if (hasTitle) return repo.findByTitleContainingIgnoreCaseOrderByDateDesc(title).stream().filter(e -> !e.isPending()).collect(Collectors.toList());
        if (hasType)  return repo.findByTypeContainingOrderByDateDesc(type).stream().filter(e -> !e.isPending()).collect(Collectors.toList());
        return getAllNonPending();
    }

    public String saveCover(MultipartFile file) throws IOException {
        Path dir = Paths.get(coversDir);
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + ".jpg";
        Path dest = dir.resolve(filename);
        try (InputStream in = file.getInputStream()) {
            BufferedImage original = ImageIO.read(in);
            if (original == null) {
                Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
                return filename;
            }
            BufferedImage resized = resizeCover(original);
            writeJpeg(resized, dest, COVER_QUALITY);
        }
        return filename;
    }

    public String saveCoverFromBytes(byte[] bytes, String ext) throws IOException {
        Path dir = Paths.get(coversDir);
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + ".jpg";
        Path dest = dir.resolve(filename);
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            BufferedImage original = ImageIO.read(in);
            if (original == null) {
                Files.write(dest, bytes);
                return filename;
            }
            BufferedImage resized = resizeCover(original);
            writeJpeg(resized, dest, COVER_QUALITY);
        }
        return filename;
    }

    private BufferedImage resizeCover(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= COVER_MAX_W && h <= COVER_MAX_H) {
            return toRgb(src);
        }
        double scale = Math.min((double) COVER_MAX_W / w, (double) COVER_MAX_H / h);
        int nw = (int) (w * scale);
        int nh = (int) (h * scale);
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, nw, nh);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }

    private BufferedImage toRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) return src;
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, src.getWidth(), src.getHeight());
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return rgb;
    }

    private void writeJpeg(BufferedImage img, Path dest, float quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(dest.toFile())) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    public Map<String, Object> getStats(String period, String tipo) {
        List<Entry> all = getAllNonPending();
        LocalDate now  = LocalDate.now();
        LocalDate from = switch (period != null ? period : "mes") {
            case "semana" -> now.minusDays(now.getDayOfWeek().getValue() - 1);
            case "anio"   -> now.withDayOfYear(1);
            case "todo"   -> LocalDate.of(2000, 1, 1);
            default       -> now.withDayOfMonth(1);
        };

        List<Entry> byPeriod = all.stream()
            .filter(e -> !e.getDate().isBefore(from))
            .collect(Collectors.toList());

        boolean filterTipo = tipo != null && !"Todos".equals(tipo);
        List<Entry> filtered = filterTipo
            ? byPeriod.stream().filter(e -> e.getType() != null && e.getType().contains(tipoKeyword(tipo))).collect(Collectors.toList())
            : byPeriod;
        List<Entry> filteredAll = filterTipo
            ? all.stream().filter(e -> e.getType() != null && e.getType().contains(tipoKeyword(tipo))).collect(Collectors.toList())
            : all;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total",      filtered.size());
        stats.put("totalAll",   filteredAll.size());
        stats.put("libros",     count(filtered, "Libro"));
        stats.put("series",     count(filtered, "Serie"));
        stats.put("peliculas",  count(filtered, "Pel"));
        stats.put("teatro",     count(filtered, "Teatro"));
        stats.put("comics",     count(filtered, "mic"));
        stats.put("cine",       filtered.stream().filter(e -> Boolean.TRUE.equals(e.getSeenInCinema())).count());
        stats.put("cineTotal",  filteredAll.stream().filter(e -> Boolean.TRUE.equals(e.getSeenInCinema())).count());
        stats.put("cinemaList", filteredAll.stream().filter(e -> Boolean.TRUE.equals(e.getSeenInCinema())).collect(Collectors.toList()));

        Map<String, Long> porTipo = new LinkedHashMap<>();
        if (!filterTipo) {
            porTipo.put("Libro",       count(filtered, "Libro"));
            porTipo.put("Serie",       count(filtered, "Serie"));
            porTipo.put("Pel\u00edcula",    count(filtered, "Pel"));
            porTipo.put("Teatro",      count(filtered, "Teatro"));
            porTipo.put("C\u00f3mic",       count(filtered, "mic"));
        } else {
            porTipo.put(tipo, (long) filtered.size());
        }
        stats.put("porTipo", porTipo);

        Map<String, Long> porMes = new TreeMap<>(filtered.stream().collect(
            Collectors.groupingBy(
                e -> e.getDate().getYear() + "-" + String.format("%02d", e.getDate().getMonthValue()),
                Collectors.counting())));
        stats.put("porMes", porMes);

        String[] dias = {"Lun","Mar","Mi\u00e9","Jue","Vie","S\u00e1b","Dom"};
        long[] porDia = new long[7];
        for (Entry e : filtered) porDia[e.getDate().getDayOfWeek().getValue() - 1]++;
        stats.put("porDia",     porDia);
        stats.put("diasLabels", dias);
        stats.put("period",     period != null ? period : "mes");

        if (filterTipo) {
            stats.putAll(buildTypeStats(filtered, tipo));
        }

        return stats;
    }

    private Map<String, Object> buildTypeStats(List<Entry> entries, String tipo) {
        Map<String, Object> extra = new LinkedHashMap<>();
        if ("Libro".equals(tipo)) {
            long terminados = entries.stream().filter(e -> Boolean.TRUE.equals(e.getFinished())).count();
            OptionalDouble avgRating = entries.stream().filter(e -> e.getRating() != null && e.getRating() > 0).mapToInt(Entry::getRating).average();
            List<String> autores = entries.stream().filter(e -> e.getAuthor() != null && !e.getAuthor().isBlank()).map(Entry::getAuthor).distinct().sorted().collect(Collectors.toList());
            extra.put("librosTerminados", terminados);
            extra.put("librosEnCurso",    entries.size() - terminados);
            extra.put("avgRating",        avgRating.isPresent() ? String.format("%.1f", avgRating.getAsDouble()) : "-");
            extra.put("autoresDistintos", autores.size());
            extra.put("topAutores",       autores.stream().limit(5).collect(Collectors.toList()));
        } else if ("Serie".equals(tipo)) {
            long terminadas = entries.stream().filter(e -> Boolean.TRUE.equals(e.getSeriesFinished())).count();
            OptionalDouble avgRating = entries.stream().filter(e -> e.getRating() != null && e.getRating() > 0).mapToInt(Entry::getRating).average();
            extra.put("seriesTerminadas", terminadas);
            extra.put("seriesEnCurso",    entries.size() - terminadas);
            extra.put("avgRating",        avgRating.isPresent() ? String.format("%.1f", avgRating.getAsDouble()) : "-");
        } else if ("Pel\u00edcula".equals(tipo)) {
            long enCine = entries.stream().filter(e -> Boolean.TRUE.equals(e.getSeenInCinema())).count();
            OptionalDouble avgRating = entries.stream().filter(e -> e.getRating() != null && e.getRating() > 0).mapToInt(Entry::getRating).average();
            extra.put("pelisEnCine",      enCine);
            extra.put("pelisEnCasa",      entries.size() - enCine);
            extra.put("avgRating",        avgRating.isPresent() ? String.format("%.1f", avgRating.getAsDouble()) : "-");
        } else if ("Teatro".equals(tipo)) {
            OptionalDouble avgRating = entries.stream().filter(e -> e.getRating() != null && e.getRating() > 0).mapToInt(Entry::getRating).average();
            List<String> lugares = entries.stream().filter(e -> e.getVenue() != null && !e.getVenue().isBlank()).map(Entry::getVenue).distinct().sorted().collect(Collectors.toList());
            extra.put("avgRating",        avgRating.isPresent() ? String.format("%.1f", avgRating.getAsDouble()) : "-");
            extra.put("lugaresDistintos", lugares.size());
            extra.put("topLugares",       lugares.stream().limit(5).collect(Collectors.toList()));
        } else if ("C\u00f3mic".equals(tipo)) {
            long terminados = entries.stream().filter(e -> Boolean.TRUE.equals(e.getFinished())).count();
            OptionalDouble avgRating = entries.stream().filter(e -> e.getRating() != null && e.getRating() > 0).mapToInt(Entry::getRating).average();
            extra.put("comicsTerminados", terminados);
            extra.put("comicsEnCurso",    entries.size() - terminados);
            extra.put("avgRating",        avgRating.isPresent() ? String.format("%.1f", avgRating.getAsDouble()) : "-");
        }
        return extra;
    }

    private String tipoKeyword(String tipo) {
        return switch (tipo) {
            case "Libro"       -> "Libro";
            case "Serie"       -> "Serie";
            case "Pel\u00edcula"    -> "Pel";
            case "Teatro"      -> "Teatro";
            case "C\u00f3mic"       -> "mic";
            default            -> tipo;
        };
    }

    public Map<String, Object> getStats(String period) {
        return getStats(period, "Todos");
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

    public List<Entry> getCinemaMovies() { return repo.findCinemaMovies(); }

    private long count(List<Entry> list, String keyword) {
        return list.stream().filter(e -> e.getType() != null && e.getType().contains(keyword)).count();
    }
}
