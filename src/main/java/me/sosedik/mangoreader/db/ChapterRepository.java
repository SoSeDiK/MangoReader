package me.sosedik.mangoreader.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<ChapterEntity, Long> {

	Optional<ChapterEntity> findByStoragePath(@Param("storagePath") String storagePath);

	Optional<ChapterEntity> findByTitleEntity_IdAndChapterNum(@Param("titleId") Long titleId, @Param("chapterNum") Integer chapterNum);

	@Query("SELECT c FROM ChapterEntity c JOIN FETCH c.titleEntity t WHERE t.id = :titleId AND c.chapterNum = :chapterNum")
	Optional<ChapterEntity> findByTitleIdAndChapterNumWithTitle(@Param("titleId") Long titleId, @Param("chapterNum") Integer chapterNum);

}
