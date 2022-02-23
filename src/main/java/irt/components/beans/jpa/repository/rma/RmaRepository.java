package irt.components.beans.jpa.repository.rma;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.rma.Rma;

public interface RmaRepository extends CrudRepository<Rma, Long> {

	List<Rma> findBySerialNumberContainingOrderBySerialNumber(String desired, Pageable page);
	List<Rma> findByDescriptionContainingOrderBySerialNumber(String desired, Pageable page);
}
