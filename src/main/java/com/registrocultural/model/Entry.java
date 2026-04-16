package com.registrocultural.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDate;

@Document(collection = "entries")
public class Entry {

    @Id
    private String id;

    @Field("titulo")
    private String title;

    @Field("tipo")
    private String type;

    @Field("descripcion")
    private String description;

    @Field("fecha")
    private LocalDate date;

    @Field("portada")
    private String coverPath;

    @Field("capitulos")
    private Integer chapters;

    @Field("autor")
    private String author;

    @Field("temporada")
    private Integer season;

    @Field("capitulo")
    private Integer episode;

    @Field("lugar")
    private String venue;

    @Field("tomoUnico")
    private Boolean isSingleVolume;

    @Field("tomo")
    private Integer comicVolume;

    @Field("numero")
    private Integer comicIssue;

    @Field("director")
    private String director;

    @Field("vistaCine")
    private Boolean seenInCinema;

    @Field("valoracion")
    private Integer rating;

    @Field("terminado")
    private Boolean finished;

    @Field("temporadaTerminada")
    private Boolean seasonFinished;

    @Field("serieTerminada")
    private Boolean seriesFinished;

    @Field("pendiente")
    private boolean pending = false;

    // ── Getters & Setters ────────────────────────────────────────
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
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
