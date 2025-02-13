package me.sosedik.mangoreader.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<ImageEntity, Long> {

	Optional<ImageEntity> findByMapping(String mapping);

	List<ImageEntity> findByMappingStartingWithOrderByImageNumAsc(String prefix);

}
