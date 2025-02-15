package me.sosedik.mangoreader.misc;

import jakarta.annotation.Nullable;
import lombok.Getter;
import me.sosedik.mangoreader.util.FileUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum ArchiveType {

	ZIP("zip", "cbz"),
	RAR("rar", "cbr");

	private static final Map<String, ArchiveType> ARCHIVE_TYPES = new HashMap<>();

	static {
		for (ArchiveType archiveType : values()) {
			for (String extension : archiveType.getExtensions()) {
				ARCHIVE_TYPES.put(extension, archiveType);
			}
		}
	}

	@Getter
	private final String[] extensions;

	ArchiveType() {
		this.extensions = new String[] { name().toLowerCase(Locale.US) };
	}

	ArchiveType(String... extensions) {
		this.extensions = extensions;
	}

	public static @Nullable ArchiveType archiveType(File file) {
		return archiveType(FileUtil.getFileExtension(file));
	}

	public static @Nullable ArchiveType archiveType(String extension) {
		return ARCHIVE_TYPES.get(extension);
	}

}
