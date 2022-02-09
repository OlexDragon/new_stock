package irt.components.beans.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import irt.components.beans.jpa.old.OldComponent;

public interface OldComponentRepository extends CrudRepository<OldComponent, Long> {

	List<OldComponent> findByPartNumberContainingOrderByPartNumber(String desired, Pageable page);
	List<OldComponent> findByManufPartNumberContainingOrderByPartNumber(String desired, Pageable page);
	List<OldComponent> findByDescriptionContainingOrderByPartNumber(String desired, Pageable page);

	@Query(value="SELECT MAX(CONVERT(SUBSTRING_INDEX(c.`part_number`, '-', -1), UNSIGNED INTEGER)) FROM components c WHERE c.part_number LIKE :start%", nativeQuery = true)
	Optional<Long> getLastIndex(@Param("start") String pn);
}
