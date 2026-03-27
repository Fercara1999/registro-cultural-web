package com.registrocultural.repository;

import com.registrocultural.model.Entry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EntryRepository extends JpaRepository<Entry, Integer> {

    List<Entry> findAllByOrderByDateDescIdDesc();

    List<Entry> findByTitleContainingIgnoreCaseOrderByDateDesc(String title);

    List<Entry> findByTypeContainingOrderByDateDesc(String type);

    List<Entry> findByDateOrderByDateDesc(LocalDate date);

    @Query("SELECT e FROM Entry e WHERE LOWER(e.title) LIKE LOWER(CONCAT('%',:title,'%')) AND e.type LIKE CONCAT('%',:type,'%') ORDER BY e.date DESC")
    List<Entry> findByTitleAndType(@Param("title") String title, @Param("type") String type);

    @Query("SELECT DISTINCT e.title FROM Entry e WHERE e.type LIKE CONCAT('%',:type,'%') ORDER BY e.title ASC")
    List<String> findDistinctTitlesByType(@Param("type") String type);

    @Query("SELECT e FROM Entry e WHERE e.type LIKE '%Libro%' AND LOWER(TRIM(e.title)) = LOWER(TRIM(:title)) AND e.author IS NOT NULL ORDER BY e.id DESC")
    List<Entry> findAuthorForTitle(@Param("title") String title);

    @Query("SELECT e FROM Entry e WHERE e.type LIKE '%Pel%' AND LOWER(TRIM(e.title)) = LOWER(TRIM(:title)) AND e.director IS NOT NULL ORDER BY e.id DESC")
    List<Entry> findDirectorForTitle(@Param("title") String title);

    @Query("SELECT e FROM Entry e WHERE e.type LIKE '%Serie%' AND LOWER(TRIM(e.title)) = LOWER(TRIM(:title)) ORDER BY e.season DESC, e.episode DESC")
    List<Entry> findLastSeriesEntry(@Param("title") String title);

    @Query("SELECT e FROM Entry e WHERE e.type LIKE '%Pel%' AND e.seenInCinema = true ORDER BY e.date DESC")
    List<Entry> findCinemaMovies();
}
