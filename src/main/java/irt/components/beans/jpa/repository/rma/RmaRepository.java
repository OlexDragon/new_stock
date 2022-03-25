package irt.components.beans.jpa.repository.rma;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.rma.Rma;

public interface RmaRepository extends CrudRepository<Rma, String> {

	List<Rma> findByRmaNumberStartsWith				(String string);

	List<Rma> findByRmaNumberContaining				(String desired, Pageable page);
	List<Rma> findByRmaNumberContainingAndShipped	(String value, boolean shipped, Pageable page);

	List<Rma> findBySerialNumberContaining			(String desired, Pageable page);
	List<Rma> findBySerialNumberContainingAndShipped	(String desired, boolean shipped, Pageable page);

	List<Rma> findByDescriptionContainingOrderBySerialNumber	(String desired, Pageable page);
	Optional<Rma> findBySerialNumberAndShipped					(String serialNumber, boolean shipped);
}
