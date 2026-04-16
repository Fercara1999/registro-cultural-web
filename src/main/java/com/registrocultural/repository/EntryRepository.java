package com.registrocultural.repository;

import com.registrocultural.model.Entry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EntryRepository extends MongoRepository<Entry, String> {

    List<Entry> findAllByOrderByDateDescIdDesc();

    List<Entry> findByTitleContainingIgnoreCaseOrderByDateDesc(String title);

    List<Entry> findByTypeContainingOrderByDateDesc(String type);

    List<Entry> findByDateOrderByDateDesc(LocalDate date);

    @Query("{ 'title': { $regex: ?0, $options: 'i' }, 'type': { $regex: ?1 } }")
    List<Entry> findByTitleAndType(String title, String type);

    @Query(value = "{ 'type': { $regex: ?0 } }", fields = "{ 'title': 1 }")
    List<Entry> findDistinctTitlesByType(String type);

    @Query("{ 'type': { $regex: 'Libro' }, 'title': { $regex: ?0, $options: 'i' }, 'author': { $exists: true, $ne: null } }")
    List<Entry> findAuthorForTitle(String title);

    @Query("{ 'type': { $regex: 'Pel' }, 'title': { $regex: ?0, $options: 'i' }, 'director': { $exists: true, $ne: null } }")
    List<Entry> findDirectorForTitle(String title);

    @Query("{ 'type': { $regex: 'Serie' }, 'title': { $regex: ?0, $options: 'i' } }")
    List<Entry> findLastSeriesEntry(String title);

    @Query("{ 'type': { $regex: 'Pel' }, 'seenInCinema': true }")
    List<Entry> findCinemaMovies();
}
