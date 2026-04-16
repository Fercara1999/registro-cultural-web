package com.registrocultural.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "app_settings")
public class AppSettings {

    @Id
    private String id = "singleton";

    private LocalDateTime lastCsvExport;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDateTime getLastCsvExport() { return lastCsvExport; }
    public void setLastCsvExport(LocalDateTime lastCsvExport) { this.lastCsvExport = lastCsvExport; }
}
