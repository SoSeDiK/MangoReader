package me.sosedik.mangoreader.misc;

import jakarta.annotation.Nullable;
import lombok.Getter;
import me.sosedik.mangoreader.util.FileUtil;
import org.springframework.http.MediaType;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum ImageType {

	PNG(MediaType.IMAGE_PNG, false),
	JPEG(MediaType.IMAGE_JPEG, false, "jpg", "jpeg"),
	GIF(MediaType.IMAGE_GIF, true),
	WEBP(MediaType.parseMediaType("image/webp"), false),
	JXL(MediaType.parseMediaType("image/jxl"), false);

	private static final Map<String, ImageType> IMAGE_TYPES = new HashMap<>();

	static {
		for (ImageType imageType : values()) {
			for (String extension : imageType.getExtensions()) {
				IMAGE_TYPES.put(extension, imageType);
			}
		}
	}

	@Getter
	private final MediaType mediaType;
	@Getter
	private final String[] extensions;
	@Getter
	private boolean animated;

	ImageType(MediaType mediaType, boolean animated) {
		this.mediaType = mediaType;
		this.extensions = new String[] { name().toLowerCase(Locale.US) };
	}

	ImageType(MediaType mediaType, boolean animated, String... extensions) {
		this.mediaType = mediaType;
		this.extensions = extensions;
	}

	public String getExtension() {
		return this.extensions[0];
	}

	public String getMediaTypeValue() {
		return this.mediaType.toString();
	}

	public static @Nullable ImageType imageType(File file) {
		return imageType(FileUtil.getFileExtension(file));
	}

	public static @Nullable ImageType imageType(String extension) {
		return IMAGE_TYPES.get(extension);
	}

}
