package com.registrocultural.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "entries")
public class Entry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDate date;

    private String coverPath;
    private Integer chapters;
    private String author;
    private Integer season;
    private Integer episode;
    private String venue;
    private Boolean isSingleVolume;
    private Integer comicVolume;
    private Integer comicIssue;
    private String director;
    private Boolean seenInCinema;
    private Integer rating;
    private Boolean finished;
    private Boolean seasonFinished;
    private Boolean seriesFinished;

    /** true = pendiente (quiero ver/leer), false/null = ya visto/leido */
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean pending = false;

    // ── Getters & Setters ────────────────────────────────────────
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getCoverPath() { return coverPath; }
    public void setCoverPath(String coverPath) { this.coverPath = coverPath; }
    public Integer getChapters() { return chapters; }
    public void setChapters(Integer chapters) { this.chapters = chapters; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public Integer getSeason() { return season; }
    public void setSeason(Integer season) { this.season = season; }
    public Integer getEpisode() { return episode; }
    public void setEpisode(Integer episode) { this.episode = episode; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public Boolean getIsSingleVolume() { return isSingleVolume; }
    public void setIsSingleVolume(Boolean v) { this.isSingleVolume = v; }
    public Integer getComicVolume() { return comicVolume; }
    public void setComicVolume(Integer v) { this.comicVolume = v; }
    public Integer getComicIssue() { return comicIssue; }
    public void setComicIssue(Integer v) { this.comicIssue = v; }
    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }
    public Boolean getSeenInCinema() { return seenInCinema; }
    public void setSeenInCinema(Boolean v) { this.seenInCinema = v; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public Boolean getFinished() { return finished; }
    public void setFinished(Boolean finished) { this.finished = finished; }
    public Boolean getSeasonFinished() { return seasonFinished; }
    public void setSeasonFinished(Boolean v) { this.seasonFinished = v; }
    public Boolean getSeriesFinished() { return seriesFinished; }
    public void setSeriesFinished(Boolean v) { this.seriesFinished = v; }
    public boolean isPending() { return pending; }
    public void setPending(boolean pending) { this.pending = pending; }
}
