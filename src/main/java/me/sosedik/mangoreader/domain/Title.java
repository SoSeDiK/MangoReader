package me.sosedik.mangoreader.domain;

import jakarta.annotation.Nullable;
import me.sosedik.mangoreader.conv.EntitiesMapper;
import me.sosedik.mangoreader.db.TitleEntity;

import java.util.List;

/**
 * Represents a manga title
 *
 * @param id database id
 * @param name title name
 * @param storagePath storage path
 * @param chaptersCount number of chapters
 */
public record Title(
	@Nullable Long id,
	String name,
	String storagePath,
	int chaptersCount
) {

	public TitleEntity toEntity() {
		return EntitiesMapper.toEntity(this);
	}

}
