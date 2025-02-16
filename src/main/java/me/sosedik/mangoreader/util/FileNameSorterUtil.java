package me.sosedik.mangoreader.util;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileNameSorterUtil {

	public static final Comparator<String> COMPARATOR = Comparator
			.comparing(FileNameSorterUtil::extractPrefix)
			.thenComparingInt(FileNameSorterUtil::extractNumber)
			.thenComparing(String::compareTo);

	private static String extractPrefix(String filename) {
		// Extract the prefix (everything before the first digit)
		return filename.replaceAll("\\d.*", "");
	}

	private static int extractNumber(String filename) {
		Pattern pattern = Pattern.compile("\\d+");
		Matcher matcher = pattern.matcher(filename);
		if (!matcher.find()) return Integer.MAX_VALUE;

		try {
			return Integer.parseInt(matcher.group());
		} catch (NumberFormatException ignored) {
			return Integer.MAX_VALUE;
		}
	}

}
