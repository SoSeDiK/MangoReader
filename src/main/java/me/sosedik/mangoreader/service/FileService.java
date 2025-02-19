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
import me.sosedik.mangoreader.misc.ArchiveType;
import me.sosedik.mangoreader.misc.ImageType;
import me.sosedik.mangoreader.util.FileNameSorterUtil;
import me.sosedik.mangoreader.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
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

	/**
	 * Processes the contents of the root storage
	 *
	 * @param rootPath root folder path
	 */
	public void processPath(Path rootPath) {
		if (!Files.exists(rootPath)) {
			log.warn("Root dir {} not found, skipping", rootPath);
			return;
		}
		if (!Files.isDirectory(rootPath)) {
			log.info("Root dir {} is not a directory, skipping", rootPath);
			return;
		}

		log.info("Processing root dir {}", rootPath);

		Stack<Path> stack = new Stack<>();
		stack.push(rootPath);

		List<ChapterEntity> chapterEntities = new ArrayList<>();
		while (!stack.isEmpty()) {
			Path currentPath = stack.pop();

			chapterEntities.clear();

			TitleEntity titleEntity = TitleEntity.builder()
				.name(currentPath.getFileName().toString())
				.storagePath(currentPath.toString())
				.build();

			try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentPath)) {
				for (Path entry : stream) {
					if (Files.isDirectory(entry)) {
						stack.push(entry);

						boolean alreadyCached = this.chapterRepository.findByStoragePath(entry.toString()).isPresent();
						if (alreadyCached) {
							log.info("Chapter {} is already cached, skipping", entry);
							continue;
						}

						ChapterEntity chapterEntity = processChapterDir(entry);
						if (chapterEntity == null) continue;

						chapterEntity.setTitleEntity(titleEntity);
						chapterEntities.add(chapterEntity);

						continue;
					}

					boolean alreadyCached = this.titleRepository.findByStoragePath(entry.toString()).isPresent();
					if (alreadyCached) {
						log.info("Title {} is already cached, skipping", entry);
						continue;
					}
					alreadyCached = this.chapterRepository.findByStoragePath(entry.toString()).isPresent();
					if (alreadyCached) {
						log.info("Chapter {} is already cached, skipping", entry);
						continue;
					}

					processArchive(entry, titleEntity, chapterEntities);
				}
			} catch (IOException e) {
				log.warn("Couldn't process path at {}", currentPath, e);
				continue;
			}

			save(titleEntity, chapterEntities);
		}
	}

	private void save(TitleEntity titleEntity, List<ChapterEntity> chapterEntities) {
		if (chapterEntities.isEmpty()) return;

		titleEntity.setChapters(chapterEntities);
		titleEntity.setChapterCount(chapterEntities.size());
		this.titleRepository.save(titleEntity);

		chapterEntities.sort(Comparator.comparing(ChapterEntity::getName, FileNameSorterUtil.COMPARATOR));

		for (int i = 0; i < chapterEntities.size(); i++) {
			ChapterEntity chapterEntity = chapterEntities.get(i);
			chapterEntity.setChapterNum(i + 1);
			log.info("Processed chapter {} with {} entries", chapterEntity.getStoragePath(), chapterEntity.getImageCount());
		}

		this.chapterRepository.saveAll(chapterEntities);

		chapterEntities.forEach(chapterEntity -> {
			List<ImageEntity> imageEntities = chapterEntity.getImages();
			imageEntities.sort(Comparator.comparing(ImageEntity::getName, FileNameSorterUtil.COMPARATOR));

			for (int i = 0; i < imageEntities.size(); i++) {
				ImageEntity imageEntity = imageEntities.get(i);
				imageEntity.setImageNum(i + 1);
				imageEntity.setMapping(titleEntity.getId() + "/" + chapterEntity.getChapterNum() + "/" + imageEntity.getImageNum());
			}

			this.imageRepository.saveAll(imageEntities);
		});

		log.info("Processed title {} with {} chapters", titleEntity.getStoragePath(), titleEntity.getChapterCount());
	}

	private ChapterEntity processChapterDir(Path chapterDir) {
		List<ImageEntity> imageEntities = null;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(chapterDir)) {
			for (Path entry : stream) {
				if (!Files.isRegularFile(entry)) continue;

				ImageEntity imageEntity = processImage(entry);
				if (imageEntity == null) continue;

				if (imageEntities == null) imageEntities = new ArrayList<>();
				imageEntities.add(imageEntity);
			}
		} catch (IOException e) {
			log.error("Error reading directory", e);
			return null;
		}
		if (imageEntities == null) return null;

		return ChapterEntity.builder()
			.name(chapterDir.getFileName().toString())
			.storagePath(chapterDir.toString())
			.images(imageEntities)
			.imageCount(imageEntities.size())
			.build();
	}

	private @Nullable ImageEntity processImage(Path imagePath) {
		String extension = FileUtil.getFileExtension(imagePath);
		ImageType imageType = ImageType.imageType(extension);
		if (imageType == null) return null;

		String fileName = imagePath.getFileName().toString();
		return ImageEntity.builder()
				.name(FileUtil.getFileNameWithoutExtension(fileName))
				.mapping(imagePath.getFileName().toString())
				.extension(extension)
				.storagePath(imagePath.toString())
				.imageNum(-1)
				.build();
	}

	private void processArchive(Path archivePath, TitleEntity parentTitleEntity, List<ChapterEntity> parentChapterEntities) throws IOException {
		ArchiveType archiveType = ArchiveType.archiveType(archivePath);
		if (archiveType == null) return;

		Map<String, List<ImageEntity>> chapters = new HashMap<>(); // TODO optimize heap size (potentially loads GBs of images)
		switch (archiveType) {
			case ZIP -> {
				try (var zipIn = new ZipInputStream(new FileInputStream(archivePath.toFile()))) {
					ZipEntry entry;
					while ((entry = zipIn.getNextEntry()) != null) {
						if (entry.isDirectory()) {
							zipIn.closeEntry();
							continue;
						}

						ImageEntity imageEntity = processZipImageEntry(zipIn, entry);
						if (imageEntity == null) {
							zipIn.closeEntry();
							continue;
						}

						String folderPath;
						int lastSlashIndex = entry.getName().lastIndexOf('/');
						if (lastSlashIndex != -1) {
							folderPath = entry.getName().substring(0, lastSlashIndex);
						} else {
							folderPath = "";
						}

						chapters.computeIfAbsent(folderPath, k -> new ArrayList<>()).add(imageEntity);

						zipIn.closeEntry();
					}
				}
			}
			case RAR -> {
				try (var archive = new Archive(archivePath.toFile())) {
					FileHeader fileHeader;
					while ((fileHeader = archive.nextFileHeader()) != null) {
						if (fileHeader.isDirectory()) continue;

						ImageEntity imageEntity = processRarImageEntry(archive, fileHeader);
						if (imageEntity == null) continue;

						String folderPath;
						int lastSlashIndex = fileHeader.getFileName().lastIndexOf('/');
						if (lastSlashIndex != -1) {
							folderPath = fileHeader.getFileName().substring(0, lastSlashIndex);
						} else {
							folderPath = "";
						}

						chapters.computeIfAbsent(folderPath, k -> new ArrayList<>()).add(imageEntity);
					}
				} catch (RarException e) {
					throw new IOException("Couldn't read RAR archive", e);
				}
			}
		}

		if (chapters.isEmpty()) return;

		String fileName = FileUtil.getFileNameWithoutExtension(archivePath.getFileName().toString());

		boolean soleChapter = chapters.size() == 1;
		if (soleChapter) {
			List<ImageEntity> imageEntities = chapters.values().iterator().next();
			ChapterEntity chapterEntity = ChapterEntity.builder()
				.titleEntity(parentTitleEntity)
				.name(fileName)
				.storagePath(archivePath.toString())
				.images(imageEntities)
				.imageCount(imageEntities.size())
				.build();
			parentChapterEntities.add(chapterEntity);
			return;
		}

		if (chapters.get("") != null) {
			log.warn("Skipped archive with invalid structure {}", archivePath);
			return;
		}

		TitleEntity titleEntity = TitleEntity.builder()
			.name(fileName)
			.storagePath(archivePath.toString())
			.build();

		List<ChapterEntity> chapterEntities = new ArrayList<>();
		chapters.forEach((folder, imageEntities) -> {
			ChapterEntity chapterEntity = ChapterEntity.builder()
				.titleEntity(titleEntity)
				.name(folder)
				.storagePath(archivePath + "/" + folder)
				.images(imageEntities)
				.imageCount(imageEntities.size())
				.build();
			chapterEntities.add(chapterEntity);
		});

		save(titleEntity, chapterEntities);
	}

	private @Nullable ImageEntity processZipImageEntry(ZipInputStream zipIn, ZipEntry entry) throws IOException {
		if (entry.isDirectory()) return null;

		String extension = FileUtil.getFileExtension(entry.getName());
		ImageType imageType = ImageType.imageType(extension);
		if (imageType == null) return null;

		byte[] imageData = readImageData(zipIn);

		String fileName = entry.getName();
		return ImageEntity.builder()
				.name(FileUtil.getFileNameWithoutExtension(fileName))
				.mapping(fileName)
				.extension(extension)
				.rawData(imageData)
				.build();
	}

	private @Nullable ImageEntity processRarImageEntry(Archive archive, FileHeader fileHeader) throws IOException {
		if (fileHeader.isDirectory()) return null;

		String extension = FileUtil.getFileExtension(fileHeader.getFileName());
		ImageType imageType = ImageType.imageType(extension);
		if (imageType == null) return null;

		try (InputStream is = archive.getInputStream(fileHeader)) {
			byte[] imageData = readImageData(is);

			String fileName = fileHeader.getFileName();
			return ImageEntity.builder()
					.name(FileUtil.getFileNameWithoutExtension(fileName))
					.mapping(fileName)
					.extension(extension)
					.rawData(imageData)
					.build();
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
