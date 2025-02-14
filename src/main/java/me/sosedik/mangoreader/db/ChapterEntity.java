package me.sosedik.mangoreader.db;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.sosedik.mangoreader.conv.EntitiesMapper;
import me.sosedik.mangoreader.domain.Chapter;

import java.util.List;

@Entity
@Table(name = "chapters")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChapterEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	private String storagePath;
	private int chapterNum;
	private int imageCount;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "title_id")
	private TitleEntity titleEntity;

	@OneToMany(mappedBy = "chapterEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private List<ImageEntity> images;

	public Chapter toViewModel() {
		return EntitiesMapper.toViewModel(this);
	}

}
