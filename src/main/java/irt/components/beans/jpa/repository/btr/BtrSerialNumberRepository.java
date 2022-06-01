package irt.components.beans.jpa.repository.btr;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.btr.BtrSerialNumber;

public interface BtrSerialNumberRepository extends CrudRepository<BtrSerialNumber, Long> {

	Optional<BtrSerialNumber> findBySerialNumber(String sn);
	List<BtrSerialNumber> findBySerialNumberContainingOrderBySerialNumberDesc(String value);
	List<BtrSerialNumber> findByDescriptionContainingOrderBySerialNumberDesc(String value);
}
