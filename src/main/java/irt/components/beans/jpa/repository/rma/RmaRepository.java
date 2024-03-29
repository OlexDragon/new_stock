package irt.components.beans.jpa.repository.rma;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.rma.Rma;
import irt.components.beans.jpa.rma.RmaCountByStatus;

public interface RmaRepository extends CrudRepository<Rma, Long> {

	List<Rma> findByRmaNumberStartsWith				(String string);

	List<Rma> findByRmaNumberContaining				(String desired, Pageable page);
	List<Rma> findByRmaNumberContainingAndStatus	(String value, Rma.Status status, Pageable page);
	List<Rma> findByRmaNumberContainingAndStatusNot	(String value, Rma.Status status, Pageable page);

	List<Rma> findBySerialNumberContaining				(String desired, Pageable page);
	List<Rma> findBySerialNumberContainingAndStatus		(String desired, Rma.Status status, Pageable page);
	List<Rma> findBySerialNumberContainingAndStatusNot	(String desired, Rma.Status status, Pageable page);

	List<Rma> findByDescriptionContaining				(String desired, Pageable page);
	List<Rma> findByDescriptionContainingAndStatus		(String desired, Rma.Status status, Pageable page);
	List<Rma> findByDescriptionContainingAndStatusNot	(String desired, Rma.Status status, Pageable page);

	List<Rma> findDistinctByRmaComments_CommentContaining			(String value, PageRequest of);
	List<Rma> findDistinctByRmaComments_CommentContainingAndStatus	(String desired, Rma.Status status, Pageable page);
	List<Rma> findDistinctByRmaComments_CommentContainingAndStatusNot(String desired, Rma.Status status, Pageable page);

	Optional<Rma> findBySerialNumberAndStatus					(String serialNumber, Rma.Status status);
	Optional<Rma> findBySerialNumberAndStatusNot				(String serialNumber, Rma.Status status);

	@Query("SELECT new irt.components.beans.jpa.rma.RmaCountByStatus(status, COUNT(*)) FROM Rma r WHERE r.status != 1 GROUP By r.status")
	List<RmaCountByStatus> countByStatus();
}
