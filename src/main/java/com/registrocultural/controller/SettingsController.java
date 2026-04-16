package com.registrocultural.controller;

import com.registrocultural.model.AppSettings;
import com.registrocultural.model.Entry;
import com.registrocultural.repository.AppSettingsRepository;
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
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/configuracion")
public class SettingsController {

    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EntryRepository       entryRepository;
    private final AppSettingsRepository appSettingsRepository;

    @Value("${app.covers-dir}")
    private String coversDir;

    public SettingsController(EntryRepository entryRepository,
                              AppSettingsRepository appSettingsRepository) {
        this.entryRepository       = entryRepository;
        this.appSettingsRepository = appSettingsRepository;
    }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("totalEntries", entryRepository.count());

        appSettingsRepository.findById("singleton").ifPresent(s -> {
            if (s.getLastCsvExport() != null)
                model.addAttribute("lastCsvExport", s.getLastCsvExport().format(DT_FMT));
        });

        return "configuracion";
    }

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
                    e.getId(), e.getTitle(), e.getType(), e.getDescription(),
                    e.getDate(), e.getRating(), e.getChapters(), e.getAuthor(),
                    e.getSeason(), e.getEpisode(), e.getVenue(), e.getDirector(),
                    e.getSeenInCinema(), e.getIsSingleVolume(), e.getComicVolume(),
                    e.getComicIssue(), e.getFinished(), e.getSeasonFinished(),
                    e.getSeriesFinished(), e.isPending()
                );
            }
        }

        // Guardar fecha de última descarga
        AppSettings settings = appSettingsRepository.findById("singleton")
            .orElse(new AppSettings());
        settings.setLastCsvExport(LocalDateTime.now());
        appSettingsRepository.save(settings);

        byte[] csvBytes = ('\uFEFF' + sw.toString()).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"registro-cultural.csv\"")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .contentLength(csvBytes.length)
            .body(csvBytes);
    }
}
