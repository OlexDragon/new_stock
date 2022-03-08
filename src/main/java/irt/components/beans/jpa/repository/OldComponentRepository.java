package irt.components.beans.jpa.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import irt.components.beans.jpa.old.OldComponent;

public interface OldComponentRepository extends CrudRepository<OldComponent, Long> {

	List<OldComponent> findByPartNumberLikeOrderByPartNumber(String desired, Pageable page);
	List<OldComponent> findByManufPartNumberContainingOrderByPartNumber(String desired, Pageable page);
	List<OldComponent> findByDescriptionContainingOrderByPartNumber(String desired, Pageable page);

	@Query(value="SELECT c.part_number FROM components c WHERE CONVERT(SUBSTRING_INDEX(c.part_number, '-', 1), UNSIGNED INTEGER) >= :first AND CONVERT(SUBSTRING_INDEX(c.part_number, '-', 1), UNSIGNED INTEGER) <= :last", nativeQuery = true)
	List<String> getLastIndex(@Param("first") Long first, @Param("last") Long last);
}
