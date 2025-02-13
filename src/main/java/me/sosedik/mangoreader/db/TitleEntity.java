package me.sosedik.mangoreader.db;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.sosedik.mangoreader.conv.EntitiesMapper;
import me.sosedik.mangoreader.domain.Title;

import java.util.List;

@Entity
@Table(name = "titles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TitleEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	private String storagePath;
	private int chapterCount;

	@OneToMany(mappedBy = "titleEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private List<ChapterEntity> chapters;

	public Title toViewModel() {
		return EntitiesMapper.toViewModel(this);
	}

}
