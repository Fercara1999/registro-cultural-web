package com.registrocultural.controller;

import com.registrocultural.model.Entry;
import com.registrocultural.repository.EntryRepository;
import com.registrocultural.service.EntryService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

@Controller
@RequestMapping("/configuracion")
public class SettingsController {

    private static final int   OPT_MAX_W   = 600;
    private static final int   OPT_MAX_H   = 900;
    private static final float OPT_QUALITY = 0.72f;

    private final EntryRepository entryRepository;
    private final EntryService    entryService;

    @Value("${app.covers-dir}")
    private String coversDir;

    public SettingsController(EntryRepository entryRepository, EntryService entryService) {
        this.entryRepository = entryRepository;
        this.entryService    = entryService;
    }

    // ── GET /configuracion ────────────────────────────────────────
    @GetMapping
    public String page(Model model) {
        long totalEntries = entryRepository.count();
        model.addAttribute("totalEntries", totalEntries);
        try {
            Path dir = Paths.get(coversDir);
            if (Files.exists(dir)) {
                long[] info = coversDirInfo(dir);
                model.addAttribute("coverCount", info[0]);
                model.addAttribute("coverSizeMb", String.format("%.1f", info[1] / 1_048_576.0));
            } else {
                model.addAttribute("coverCount", 0);
                model.addAttribute("coverSizeMb", "0.0");
            }
        } catch (IOException e) {
            model.addAttribute("coverCount", "?");
            model.addAttribute("coverSizeMb", "?");
        }
        return "configuracion";
    }

    // ── POST /configuracion/optimizar-portadas ────────────────────
    @PostMapping("/optimizar-portadas")
    public String optimizarPortadas(RedirectAttributes ra) {
        Path dir = Paths.get(coversDir);
        if (!Files.exists(dir)) {
            ra.addFlashAttribute("error", "El directorio de portadas no existe.");
            return "redirect:/configuracion";
        }
        int procesadas = 0;
        long bytesAntes = 0;
        long bytesDespues = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.{jpg,jpeg,JPG,JPEG}")) {
            for (Path file : stream) {
                long antes = Files.size(file);
                try {
                    boolean changed = compressImage(file);
                    if (changed) {
                        long despues = Files.size(file);
                        bytesAntes   += antes;
                        bytesDespues += despues;
                        procesadas++;
                    }
                } catch (Exception ignored) { }
            }
        } catch (IOException e) {
            ra.addFlashAttribute("error", "Error al procesar portadas: " + e.getMessage());
            return "redirect:/configuracion";
        }
        if (procesadas == 0) {
            ra.addFlashAttribute("success", "✅ Todas las portadas ya estaban optimizadas.");
        } else {
            double ahorroMb = (bytesAntes - bytesDespues) / 1_048_576.0;
            double anteMb   = bytesAntes  / 1_048_576.0;
            double despMb   = bytesDespues / 1_048_576.0;
            ra.addFlashAttribute("success",
                String.format("✅ %d portadas optimizadas · %.1f MB → %.1f MB (ahorro: %.1f MB)",
                    procesadas, anteMb, despMb, ahorroMb));
        }
        return "redirect:/configuracion";
    }

    // ── GET /configuracion/exportar-csv ───────────────────────────
    @GetMapping("/exportar-csv")
    public ResponseEntity<byte[]> exportarCsv() throws IOException {
        List<Entry> entries = entryRepository.findAll();

        StringWriter sw = new StringWriter();
        String[] headers = {
            "id","titulo","tipo","descripcion","fecha","valoracion",
            "capitulos","autor","temporada","capitulo","lugar","director",
            "vistaCine","tomoUnico","numTomo","numSerie",
            "terminado","finTemporada","serieFinalizada","pendiente"
        };
        try (CSVPrinter printer = new CSVPrinter(sw,
                CSVFormat.DEFAULT.builder().setHeader(headers).build())) {
            for (Entry e : entries) {
                printer.printRecord(
                    e.getId(),
                    e.getTitle(),
                    e.getType(),
                    e.getDescription(),
                    e.getDate(),
                    e.getRating(),
                    e.getChapters(),
                    e.getAuthor(),
                    e.getSeason(),
                    e.getEpisode(),
                    e.getVenue(),
                    e.getDirector(),
                    e.getSeenInCinema(),
                    e.getIsSingleVolume(),
                    e.getComicVolume(),
                    e.getComicIssue(),
                    e.getFinished(),
                    e.getSeasonFinished(),
                    e.getSeriesFinished(),
                    e.isPending()
                );
            }
        }
        byte[] csvBytes = ('\uFEFF' + sw.toString()).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"registro-cultural.csv\"")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .contentLength(csvBytes.length)
            .body(csvBytes);
    }

    // ── helpers ───────────────────────────────────────────────────
    private long[] coversDirInfo(Path dir) throws IOException {
        long count = 0, size = 0;
        try (DirectoryStream<Path> s = Files.newDirectoryStream(dir, "*.{jpg,jpeg,JPG,JPEG,png,PNG,webp,WebP}")) {
            for (Path p : s) { count++; size += Files.size(p); }
        }
        return new long[]{count, size};
    }

    /**
     * Recomprime y redimensiona la imagen si su ancho supera OPT_MAX_W o su alto OPT_MAX_H.
     * @return true si se procesó y se guardó una versión más pequeña.
     */
    private boolean compressImage(Path file) throws IOException {
        long sizeBefore = Files.size(file);
        BufferedImage original = ImageIO.read(file.toFile());
        if (original == null) return false;

        int w = original.getWidth();
        int h = original.getHeight();
        boolean needsResize = w > OPT_MAX_W || h > OPT_MAX_H;

        BufferedImage img;
        if (needsResize) {
            double scale = Math.min((double) OPT_MAX_W / w, (double) OPT_MAX_H / h);
            int nw = (int) (w * scale);
            int nh = (int) (h * scale);
            img = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, nw, nh);
            g.drawImage(original, 0, 0, nw, nh, null);
            g.dispose();
        } else {
            img = toRgb(original);
        }

        // Escribir en un buffer primero para comparar tamaño
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(OPT_QUALITY);
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), param);
        } finally {
            writer.dispose();
        }

        // Solo guardar si el resultado es más pequeño
        if (baos.size() < sizeBefore) {
            Files.write(file, baos.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING);
            return true;
        }
        return false;
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
}
