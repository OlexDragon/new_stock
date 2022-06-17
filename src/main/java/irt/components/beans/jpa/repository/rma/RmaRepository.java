package irt.components.beans.jpa.repository.rma;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.rma.Rma;

public interface RmaRepository extends CrudRepository<Rma, Long> {

	List<Rma> findByRmaNumberStartsWith				(String string);

	List<Rma> findByRmaNumberContaining				(String desired, Pageable page);
	List<Rma> findByRmaNumberContainingAndStatus	(String value, Rma.Status shipped, Pageable page);

	List<Rma> findBySerialNumberContaining			(String desired, Pageable page);
	List<Rma> findBySerialNumberContainingAndStatus	(String desired, Rma.Status shipped, Pageable page);

	List<Rma> findByDescriptionContainingOrderBySerialNumber	(String desired, Pageable page);

	Optional<Rma> findBySerialNumberAndStatus					(String serialNumber, Rma.Status status);
	Optional<Rma> findBySerialNumberAndStatusNot				(String serialNumber, Rma.Status shipped);
}
