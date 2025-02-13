package me.sosedik.mangoreader.conv;

import me.sosedik.mangoreader.db.ChapterEntity;
import me.sosedik.mangoreader.db.ImageEntity;
import me.sosedik.mangoreader.db.TitleEntity;
import me.sosedik.mangoreader.domain.Chapter;
import me.sosedik.mangoreader.domain.Image;
import me.sosedik.mangoreader.domain.Title;

public class EntitiesMapper {

	public static Title toViewModel(TitleEntity entity) {
		return new Title(entity.getId(), entity.getName(), entity.getStoragePath(), entity.getChapterCount());
	}

	public static TitleEntity toEntity(Title view) {
		return TitleEntity.builder()
				.id(view.id())
				.name(view.name())
				.storagePath(view.storagePath())
				.chapterCount(view.chaptersCount())
				.build();
	}

	public static Chapter toViewModel(ChapterEntity entity) {
		return new Chapter(entity.getId(), entity.getName(), entity.getStoragePath(), entity.getChapterNum());
	}

	public static ChapterEntity toEntity(Chapter view) {
		return ChapterEntity.builder()
				.id(view.id())
				.name(view.name())
				.storagePath(view.storagePath())
				.chapterNum(view.chapterNum())
				.build();
	}

	public static Image toViewModel(ImageEntity entity) {
		return new Image(
			entity.getId(),
			entity.getName(),
			entity.getMapping(),
			entity.getExtension(),
			entity.getImageNum(),
			entity.getRawData(),
			entity.getWebpData()
		);
	}

	public static ImageEntity toEntity(Image view) {
		return ImageEntity.builder()
				.id(view.id())
				.name(view.name())
				.mapping(view.mapping())
				.extension(view.extension())
				.imageNum(view.imageNum())
				.rawData(view.rawData())
				.webpData(view.webpData())
				.build();
	}

}
