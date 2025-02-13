package me.sosedik.mangoreader.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TitleRepository extends JpaRepository<TitleEntity, Long> {

	@Query("SELECT t FROM TitleEntity t LEFT JOIN FETCH t.chapters WHERE t.id = :id")
	Optional<TitleEntity> findByIdWithChapters(@Param("id") Long id);

	Optional<TitleEntity> findByStoragePath(@Param("storagePath") String storagePath);

}
