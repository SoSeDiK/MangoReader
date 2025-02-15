package me.sosedik.mangoreader.service;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import me.sosedik.mangoreader.db.ChapterEntity;
import me.sosedik.mangoreader.db.ChapterRepository;
import me.sosedik.mangoreader.db.ImageEntity;
import me.sosedik.mangoreader.db.ImageRepository;
import me.sosedik.mangoreader.db.TitleEntity;
import me.sosedik.mangoreader.db.TitleRepository;
import me.sosedik.mangoreader.domain.Chapter;
import me.sosedik.mangoreader.domain.Image;
import me.sosedik.mangoreader.domain.Title;
import me.sosedik.mangoreader.misc.ArchiveType;
import me.sosedik.mangoreader.misc.ImageType;
import me.sosedik.mangoreader.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class FileService {

	private static final Logger log = LoggerFactory.getLogger(FileService.class);

	@Autowired
	private SettingsService settingsService;
	@Autowired
	private TitleRepository titleRepository;
	@Autowired
	private ChapterRepository chapterRepository;
	@Autowired
	private ImageRepository imageRepository;

	@PostConstruct
	public void init() {
		List<String> paths = this.settingsService.getAllDirectories();
		paths.forEach(path -> processPath(Path.of(path)));
	}

	private void processPath(Path startPath) {
		log.info("Processing input dir {}", startPath);

		File startFile = startPath.toFile();
		if (!startFile.exists()) return;

		try (Stream<Path> walk = Files.walk(startPath)) {
			walk.forEach(path -> {
				File file = path.toFile();
				if (!file.isDirectory()) return;
				if (isChapterDir(path)) return;

				processTitle(file);
			});
		} catch (IOException e) {
			log.warn("Couldn't process path at {}", startPath, e);
		}
	}

	/**
	 * Checks whether the provided path is a chapter directory,
	 * i.e., contains an image inside
	 *
	 * @param dir directory path
	 * @return whether the dir should be treated as chapter
	 */
	private boolean isChapterDir(Path dir) {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			for (Path entry : stream) {
				File file = entry.toFile();
				if (!file.isFile()) continue;

				if (file.isDirectory())
					return false;

				String extension = FileUtil.getFileExtension(file);
				if (ArchiveType.archiveType(extension) != null)
					return false;

				if (ImageType.imageType(extension) != null)
					return true;
			}
		} catch (IOException e) {
			log.error("Error reading directory", e);
		}

		return false;
	}

	private void processTitle(File titleDir) {
		log.info("Processing title {}", titleDir.toPath());

		var title = new Title(null, titleDir.getName(), titleDir.getAbsolutePath(), 0);
		TitleEntity titleEntity = this.titleRepository.findByStoragePath(title.storagePath()).orElse(null);
		if (titleEntity != null)
			title = titleEntity.toViewModel();

		int chapterCount = 0;
		try (
			Stream<Path> walk = Files.walk(titleDir.toPath(), 1)
				.sorted(Comparator.comparingInt(FileService::extractNumber).thenComparing(Path::getFileName))
		) {
			for (Path path : walk.toList()) {
				File chapterFile = path.toFile();

				Chapter chapter = processChapter(chapterFile, chapterCount);
				if (chapter == null) continue;

				chapterCount++;

				if (this.chapterRepository.findByStoragePath(chapter.storagePath()).isPresent()) {
					log.info("Chapter {} is already cached", chapter.storagePath());
					continue;
				}

				log.info("Processing chapter {}", chapterFile.toPath());

				if (titleEntity == null) {
					titleEntity = title.toEntity();
					log.info("Caching title {}", titleDir.toPath());
					this.titleRepository.save(titleEntity);
				}
				titleEntity.setChapterCount(titleEntity.getChapterCount() + 1);

				ChapterEntity chapterEntity = chapter.toEntity();
				chapterEntity.setTitleEntity(titleEntity);
				this.chapterRepository.save(chapterEntity);

				int chapterImageCount;
				try {
					List<ImageEntity> imageEntities = processChapterImages(chapterFile); // TODO this eats quite some memory, better way?
					imageEntities.sort(Comparator.comparing(ImageEntity::getName));

					String prefix = titleEntity.getId() + "/" + chapterEntity.getChapterNum() + "/";
					for (int i = 0; i < imageEntities.size(); i++) {
						ImageEntity imageEntity = imageEntities.get(i);
						imageEntity.setImageNum(i + 1);
						imageEntity.setMapping(prefix + imageEntity.getImageNum());
					}

					this.imageRepository.saveAll(imageEntities);

					chapterImageCount = imageEntities.size();
				} catch (IOException e) {
					log.warn("Couldn't process chapter at {}", chapterFile.toPath(), e);
					this.chapterRepository.delete(chapterEntity);
					continue;
				}
				chapterEntity.setImageCount(chapterImageCount);
				this.chapterRepository.save(chapterEntity); // Refresh image count

				log.info("Cached chapter {} with {} images", chapterFile.toPath(), chapterImageCount);
			}
			if (titleEntity != null) {
				this.titleRepository.save(titleEntity); // Refresh chapter count
				log.info("Finished caching title {}, processed {} chapters", titleDir.toPath(), titleEntity.getChapterCount());
			}
		} catch (IOException e) {
			log.warn("Couldn't process title at {}", titleDir.toPath(), e);
		}
	}

	private static int extractNumber(Path path) {
		String filename = path.getFileName().toString();
		Pattern pattern = Pattern.compile("\\d+");
		Matcher matcher = pattern.matcher(filename);
		return matcher.find() ? Integer.parseInt(matcher.group()) : Integer.MAX_VALUE;
	}

	private @Nullable Chapter processChapter(File chapterFile, int chapterCount) {
		if (chapterFile.isDirectory()) {
			if (!isChapterDir(chapterFile.toPath()))
				return null;
		} else {
			if (ArchiveType.archiveType(chapterFile) == null)
				return null;
		}

		String fileName = chapterFile.getName();
		if (chapterFile.isFile()) {
			int lastIndexOfDot = fileName.lastIndexOf('.');
			if (lastIndexOfDot != -1) fileName = fileName.substring(0, lastIndexOfDot);
		}
		return new Chapter(null, fileName, chapterFile.getAbsolutePath(), chapterCount + 1, 0);
	}

	private List<ImageEntity> processChapterImages(File chapterFile) throws IOException {
		List<ImageEntity> chapterEntities = new ArrayList<>();

		if (chapterFile.isDirectory()) {
			try (Stream<Path> walk = Files.walk(chapterFile.toPath(), 1)) {
				walk.forEach(path -> {
					File imageFile = path.toFile();
					Image image = readImage(imageFile);
					if (image != null) {
						ImageEntity imageEntity = image.toEntity();
						chapterEntities.add(imageEntity);
					}
				});
			}
			return chapterEntities;
		}

		var archiveType = ArchiveType.archiveType(chapterFile);
		if (archiveType == null)
			throw new UnsupportedOperationException("Unsupported chapter file: " + chapterFile.toPath());

		switch (archiveType) {
			case ZIP -> {
				try (var zipIn = new ZipInputStream(new FileInputStream(chapterFile))) {
					ZipEntry entry;
					while ((entry = zipIn.getNextEntry()) != null) {
						Image image = processZipImageEntry(zipIn, entry);
						if (image != null) {
							ImageEntity imageEntity = image.toEntity();
							chapterEntities.add(imageEntity);
						}

						zipIn.closeEntry();
					}
				}
			}
			case RAR -> {
				try (var archive = new Archive(chapterFile)) {
					FileHeader fileHeader;
					while ((fileHeader = archive.nextFileHeader()) != null) {
						Image image = processRarImageEntry(archive, fileHeader);
						if (image != null) {
							ImageEntity imageEntity = image.toEntity();
							chapterEntities.add(imageEntity);
						}
					}
				} catch (RarException e) {
					throw new IOException("Couldn't read RAR archive", e);
				}
			}
		}

		return chapterEntities;
	}

	private @Nullable Image readImage(File imageFile) {
		ImageType imageType = ImageType.imageType(imageFile);
		if (imageType == null) return null;

		String fileName = imageFile.getName();
		int lastIndexOfDot = fileName.lastIndexOf('.');
		if (lastIndexOfDot != -1) fileName = fileName.substring(0, lastIndexOfDot);
		return new Image(null, fileName, imageFile.getName(), imageType.getExtension(), imageFile.getAbsolutePath(), -1, null, null);
	}

	private @Nullable Image processZipImageEntry(ZipInputStream zipIn, ZipEntry entry) throws IOException {
		if (entry.isDirectory()) return null;

		ImageType imageType = ImageType.imageType(FileUtil.getFileExtension(entry.getName()));
		if (imageType == null) return null;

		byte[] imageData = readImageData(zipIn);
//		byte[] webpData = imageType == ImageType.WEBP ? null : WebpUtil.convertToWebP(imageData, 0.8F); // TODO generating webps (separate job?), causes segfault randomly

		String fileName = entry.getName();
		int lastIndexOfDot = fileName.lastIndexOf('.');
		if (lastIndexOfDot != -1) fileName = fileName.substring(0, lastIndexOfDot);
		return new Image(null, fileName, entry.getName(), imageType.getExtension(), null, -1, imageData, null);
	}

	private @Nullable Image processRarImageEntry(Archive archive, FileHeader fileHeader) throws IOException {
		if (fileHeader.isDirectory()) return null;

		ImageType imageType = ImageType.imageType(FileUtil.getFileExtension(fileHeader.getFileName()));
		if (imageType == null) return null;

		try (InputStream is = archive.getInputStream(fileHeader)) {
			byte[] imageData = readImageData(is);

			String fileName = fileHeader.getFileName();
			int lastIndexOfDot = fileName.lastIndexOf('.');
			if (lastIndexOfDot != -1) fileName = fileName.substring(0, lastIndexOfDot);
			return new Image(null, fileName, fileHeader.getFileName(), imageType.getExtension(), null, -1, imageData, null);
		}
	}

	private byte[] readImageData(InputStream inputStream) throws IOException {
		var byteArrayOutputStream = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int bytesRead;
		while ((bytesRead = inputStream.read(buffer)) != -1) {
			byteArrayOutputStream.write(buffer, 0, bytesRead);
		}
		return byteArrayOutputStream.toByteArray();
	}

}
