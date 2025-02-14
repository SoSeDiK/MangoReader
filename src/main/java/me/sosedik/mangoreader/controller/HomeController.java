package me.sosedik.mangoreader.controller;

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
import me.sosedik.mangoreader.misc.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

@Controller
public class HomeController {

	@Autowired
	private TitleRepository titleRepository;
	@Autowired
	private ChapterRepository chapterRepository;
	@Autowired
	private ImageRepository imageRepository;

	@GetMapping("/")
	public String home(Model model) {
		List<Title> titles = this.titleRepository.findAll().stream()
			.map(TitleEntity::toViewModel)
			.toList();
		model.addAttribute("titles", titles);
		return "home";
	}

	@GetMapping("/library")
	public RedirectView redirect() {
		return new RedirectView("/");
	}

	@GetMapping("/library/{titleId}")
	public String titleView(
		@PathVariable("titleId") long titleId,
		Model model
	) {
		TitleEntity titleEntity = this.titleRepository.findByIdWithChapters(titleId)
			.orElseThrow(() -> new ResourceNotFoundException("Title with ID " + titleId + " not found"));

		List<Chapter> chapters = titleEntity.getChapters()
			.stream()
			.map(ChapterEntity::toViewModel)
			.toList();

		model.addAttribute("title", titleEntity.toViewModel());
		model.addAttribute("chapters", chapters);

		return "title";
	}

	@GetMapping("/library/{titleId}/{chapterNum}")
	public String titleView(
		@PathVariable("titleId") long titleId,
		@PathVariable("chapterNum") int chapterNum,
		Model model
	) {
		ChapterEntity chapterEntity = this.chapterRepository.findByTitleIdAndChapterNumWithTitle(titleId, chapterNum)
			.orElseThrow(() -> new ResourceNotFoundException("Chapter with num " + chapterNum + " for title with ID " + titleId + " could not found"));
		TitleEntity titleEntity = chapterEntity.getTitleEntity();

		String prefix = titleId + "/" + chapterNum + "/";
		List<Image> images = this.imageRepository.findByMappingStartingWithOrderByImageNumAsc(prefix).stream()
			.map(ImageEntity::toViewModel)
			.toList();

		model.addAttribute("title", titleEntity.toViewModel());
		model.addAttribute("chapter", chapterEntity.toViewModel());
		model.addAttribute("imageDatas", images);

		return "reader";
	}

	@GetMapping("/thumbnail/{titleId}")
	public ResponseEntity<ByteArrayResource> titleThumbnail(
		@RequestHeader(HttpHeaders.ACCEPT) String acceptHeader,
		@PathVariable("titleId") long titleId
	) { // TODO Generating real thumbnails?
		return chapterThumbnail(acceptHeader, titleId, 1);
	}

	@GetMapping("/thumbnail/{titleId}/{chapterNum}")
	public ResponseEntity<ByteArrayResource> chapterThumbnail(
		@RequestHeader(HttpHeaders.ACCEPT) String acceptHeader,
		@PathVariable("titleId") long titleId,
		@PathVariable("chapterNum") int chapterNum
	) { // TODO Generating real thumbnails?
		return getImage(acceptHeader, titleId, chapterNum, 1);
	}

	@GetMapping("/library/{titleId}/{chapterNum}/{imageNum}")
	public ResponseEntity<ByteArrayResource> getImage(
		@RequestHeader(HttpHeaders.ACCEPT) String acceptHeader,
		@PathVariable("titleId") long titleId,
		@PathVariable("chapterNum") int chapterNum,
		@PathVariable("imageNum") int imageNum
	) {
		String mapping = titleId + "/" + chapterNum + "/" + imageNum;
		ImageEntity imageEntity = this.imageRepository.findByMapping(mapping)
			.orElseThrow(() -> new ResourceNotFoundException("Couldn't find image " + mapping));

		ImageType imageType = ImageType.imageType(imageEntity.getExtension());
		if (imageType == null) imageType = ImageType.JPEG; // Huh?
		byte[] imageData;

		if (shouldServeWebp(acceptHeader, imageType) && imageEntity.getWebpData() != null) {
			imageType = ImageType.WEBP;
			imageData = imageEntity.getWebpData();
		} else if (imageType == ImageType.JXL && !acceptHeader.contains(ImageType.JXL.getMediaTypeValue())) {
			// TODO serve as PNG?
			imageData = imageEntity.getRawData();
		} else {
			imageData = imageEntity.getRawData();
		}

		return ResponseEntity.ok()
				.contentType(imageType.getMediaType())
				.body(new ByteArrayResource(imageData));
	}

	private boolean shouldServeWebp(String acceptHeader, ImageType imageType) {
		if (!acceptHeader.contains(ImageType.WEBP.getMediaTypeValue())) return false;

		// If not supporting JPEG XL
		if (imageType == ImageType.JXL && !acceptHeader.contains(ImageType.JXL.getMediaTypeValue())) return true;

		return !imageType.isAnimated() && imageType != ImageType.JXL;
	}

}
