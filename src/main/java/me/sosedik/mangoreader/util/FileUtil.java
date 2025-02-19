package me.sosedik.mangoreader.util;

import java.io.File;
import java.nio.file.Path;

public class FileUtil {

	/**
	 * Gets the file extension
	 *
	 * @param file file
	 * @return file extension
	 */
	public static String getFileExtension(File file) {
		return getFileExtension(file.getName());
	}

	/**
	 * Gets the file extension
	 *
	 * @param path path to the file
	 * @return file extension
	 */
	public static String getFileExtension(Path path) {
		return getFileExtension(path.getFileName().toFile());
	}

	/**
	 * Gets the file extension from the file name
	 *
	 * @param fileName file name
	 * @return file extension
	 */
	public static String getFileExtension(String fileName) {
		String[] split = fileName.split("\\.");
		return split[split.length - 1].toLowerCase();
	}

	/**
	 * Gets the file name without the extension
	 *
	 * @param fileName file name
	 * @return file name without extension
	 */
	public static String getFileNameWithoutExtension(String fileName) {
		int lastIndexOfDot = fileName.lastIndexOf('.');
		if (lastIndexOfDot != -1) fileName = fileName.substring(0, lastIndexOfDot);
		return fileName;
	}

}
