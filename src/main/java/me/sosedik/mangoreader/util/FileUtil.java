package me.sosedik.mangoreader.util;

import java.io.File;

public class FileUtil {

	public static String getFileExtension(File file) {
		return getFileExtension(file.getName());
	}

	public static String getFileExtension(String fileName) {
		String[] split = fileName.split("\\.");
		return split[split.length - 1].toLowerCase();
	}

}
