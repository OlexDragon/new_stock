package irt.components.beans.jpa.repository.rma;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.rma.Rma;
import irt.components.beans.jpa.rma.Rma.Status;
import irt.components.beans.jpa.rma.RmaCountByStatus;

public interface RmaRepository extends CrudRepository<Rma, Long> {

	List<Rma> findByRmaNumberStartsWith				(String string);

	List<Rma> findByStatus	(Rma.Status status);

	List<Rma> findByRmaNumberContaining				(String desired, Pageable page);
	List<Rma> findByRmaNumberContainingAndStatusIn	(String value, Pageable page, Rma.Status... status);
	List<Rma> findByRmaNumberContainingAndStatusNot	(String value, Rma.Status status, Pageable page);

	List<Rma> findBySerialNumberContaining				(String desired, Pageable page);
	List<Rma> findBySerialNumberContainingAndStatus		(String desired, Pageable page, Rma.Status... status);
	List<Rma> findBySerialNumberContainingAndStatusIn	(String desired, Pageable page, Rma.Status... status);
	List<Rma> findBySerialNumberContainingAndStatusNotIn(String desired, Pageable page, Rma.Status... status);

	List<Rma> findByDescriptionContaining				(String desired, Pageable page);
	List<Rma> findByDescriptionContainingAndStatusIn	(String desired, Pageable page, Rma.Status... status);
	List<Rma> findByDescriptionContainingAndStatusNotIn	(String desired, Pageable page, Rma.Status... status);

	List<Rma> findDistinctByRmaCommentsCommentContaining				(String value, Pageable page);
	List<Rma> findDistinctByRmaCommentsCommentContainingAndStatusIn		(String desired, Pageable page, Rma.Status... status);
	List<Rma> findDistinctByRmaCommentsCommentContainingAndStatusNotIn	(String desired, Pageable page, Rma.Status... status);

	List<Rma> findBySerialNumberAndStatusIn					(String serialNumber, Rma.Status... status);
	List<Rma> findBySerialNumberAndStatusNotIn				(String serialNumber, Rma.Status... status);

	@Query("SELECT new irt.components.beans.jpa.rma.RmaCountByStatus(status, COUNT(*)) FROM Rma r WHERE r.status != 1 GROUP By r.status")
	List<RmaCountByStatus> countByStatus();

	boolean existsBySerialNumberAndStatusIn		(String sn, Status... status);
	boolean existsBySerialNumberAndStatusNotIn	(String sn, Status... status);
}
