package me.sosedik.mangoreader.service;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

				processTitle(file);
			});
		} catch (IOException e) {
			log.warn("Couldn't process path at {}", startPath, e);
		}
	}

	private void processTitle(File titleDir) {
		log.info("Processing title {}", titleDir.toPath());

		var title = new Title(null, titleDir.getName(), titleDir.getAbsolutePath(), 0);
		TitleEntity titleEntity = this.titleRepository.findByStoragePath(title.storagePath()).orElse(null);
		if (titleEntity != null)
			title = titleEntity.toViewModel();

		int chapterCount = 0;
		try (Stream<Path> walk = Files.walk(titleDir.toPath(), 1).sorted()) {
			for (Path path : walk.toList()) {
				File chapterFile = path.toFile();
				if (!chapterFile.isFile()) continue;

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
					List<ImageEntity> imageEntities = processChapterImages(chapterFile); // TODO this eats quite some RAM storage, better way?
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

	private @Nullable Chapter processChapter(File chapterFile, int chapterCount) {
		if (!isChapterFile(chapterFile)) return null;

		return new Chapter(null, chapterFile.getName(), chapterFile.getAbsolutePath(), chapterCount + 1);
	}

	private boolean isChapterFile(File file) {
		if (!file.isFile()) return false;

		String fileType = FileUtil.getFileExtension(file);
		return switch (fileType) {
			case "cbz", "pdf" -> true;
			default -> false;
		};
	}

	private List<ImageEntity> processChapterImages(File file) throws IOException {
		List<ImageEntity> chapterEntities = new ArrayList<>();
		try (var zipIn = new ZipInputStream(new FileInputStream(file))) {
			ZipEntry entry;
			while ((entry = zipIn.getNextEntry()) != null) {
				Image image = processImageEntry(zipIn, entry);
				if (image != null) {
					ImageEntity imageEntity = image.toEntity();
					chapterEntities.add(imageEntity);
				}

				zipIn.closeEntry();
			}
		}
		return chapterEntities;
	}

	private @Nullable Image processImageEntry(ZipInputStream zipIn, ZipEntry entry) throws IOException {
		if (entry.isDirectory()) return null;

		ImageType imageType = ImageType.imageType(FileUtil.getFileExtension(entry.getName()));
		if (imageType == null) return null;

		byte[] imageData = readImageData(zipIn);
//		byte[] webpData = imageType == ImageType.WEBP ? null : WebpUtil.convertToWebP(imageData, 0.8F); // TODO generating webps (separate job?), causes segfault randomly
		byte[] webpData = null;

		return new Image(null, entry.getName(), entry.getName(), imageType.getExtension(), -1, imageData, webpData);
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
