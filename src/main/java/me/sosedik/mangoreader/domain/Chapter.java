package me.sosedik.mangoreader.domain;

import jakarta.annotation.Nullable;
import me.sosedik.mangoreader.conv.EntitiesMapper;
import me.sosedik.mangoreader.db.ChapterEntity;

/**
 * Represents a manga chapter
 *
 * @param id database id
 * @param name chapter name
 * @param storagePath storage path
 */
public record Chapter(
	@Nullable Long id,
	String name,
	String storagePath,
	int chapterNum,
	int imageCount
) {

	public ChapterEntity toEntity() {
		return EntitiesMapper.toEntity(this);
	}

}
