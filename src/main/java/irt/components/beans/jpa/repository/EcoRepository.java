package irt.components.beans.jpa.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.eco.Eco;

public interface EcoRepository extends CrudRepository<Eco, Long> {

	Eco findByPartNumber(String partNumber);

	List<Eco> findByEcoNumberStartsWith (String desired);

	List<Eco> findByEcoNumberContainingOrderByEcoNumberDesc				(String desired, Pageable page);
	List<Eco> findByEcoNumberContainingAndStatusOrderByEcoNumberDesc	(String desired, Eco.Status status, Pageable page);

	List<Eco> findByPartNumberContainingOrderByEcoNumberDesc			(String desired, Pageable page);
	List<Eco> findByPartNumberContainingAndStatusOrderByEcoNumberDesc	(String desired, Eco.Status status, Pageable page);

	List<Eco> findByDescriptionContainingOrderByEcoNumberDesc			(String desired, Pageable page);
	List<Eco> findByDescriptionContainingAndStatusOrderByEcoNumberDesc	(String desired, Eco.Status status, Pageable page);
}
