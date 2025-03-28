package irt.components.beans.jpa.repository.btr;

import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.btr.BtrPowerDetector;

public interface BtrPowerDetectorRepository extends CrudRepository<BtrPowerDetector, Long> {

	BtrPowerDetector findBySerialNumberId(Long serialNumberId);
}
