package me.sosedik.mangoreader.db;

import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.sosedik.mangoreader.conv.EntitiesMapper;
import me.sosedik.mangoreader.domain.Image;

@Entity
@Table(name = "images")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	private String mapping;
	private String extension;
	private @Nullable String storagePath;
	private Integer imageNum;

	@Lob
	private @Nullable byte[] rawData;
	@Lob
	private @Nullable byte[] webpData;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "chapter_id")
	private ChapterEntity chapterEntity;

	public Image toViewModel() {
		return EntitiesMapper.toViewModel(this);
	}

}
