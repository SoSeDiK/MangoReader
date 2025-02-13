package me.sosedik.mangoreader.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class SettingsService {

	@Value("${mango.inputDirs:./samples}")
	private String directoryPaths;

	/**
	 * Gets all configured input paths
	 *
	 * @return input directory paths
	 */
	public List<String> getAllDirectories() {
		return Arrays.stream(this.directoryPaths.split(","))
				.map(String::trim)
				.toList();
	}

}
