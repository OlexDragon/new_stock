package irt.components.beans.jpa.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.eco.Eco;

public interface EcoRepository extends CrudRepository<Eco, Long> {

	Eco findByPartNumber(String partNumber);

	List<Eco> findByEcoNumberStartsWith (String desired);

	List<Eco> findByEcoNumberContainingOrderByEcoNumber			(String desired, Pageable page);
	List<Eco> findByPartNumberContainingOrderByEcoNumber		(String desired, Pageable page);
	List<Eco> findByDescriptionContainingOrderByEcoNumber		(String desired, Pageable page);
}
