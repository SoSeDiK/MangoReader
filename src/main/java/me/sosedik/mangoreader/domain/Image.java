package me.sosedik.mangoreader.domain;

import jakarta.annotation.Nullable;
import me.sosedik.mangoreader.conv.EntitiesMapper;
import me.sosedik.mangoreader.db.ImageEntity;

public record Image(
	@Nullable Long id,
	String name,
	String mapping,
	String extension,
	int imageNum,
	byte[] rawData,
	@Nullable byte[] webpData
) {

	public ImageEntity toEntity() {
		return EntitiesMapper.toEntity(this);
	}

}
