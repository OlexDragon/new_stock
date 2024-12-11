package irt.components.beans.jpa.repository.btr;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.btr.BtrMeasurements;

public interface BtrMeasurementsRepository extends CrudRepository<BtrMeasurements, Long> {

	List<BtrMeasurements> findBySerialNumberId(Long serialNumberId);
}
