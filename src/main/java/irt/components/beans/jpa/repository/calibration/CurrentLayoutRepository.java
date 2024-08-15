package irt.components.beans.jpa.repository.calibration;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.calibration.CurrentLayout;
import irt.components.beans.jpa.calibration.CurrentLayoutKey;

public interface CurrentLayoutRepository extends CrudRepository<CurrentLayout, CurrentLayoutKey> {

	List<CurrentLayout> findByTopIdAndModuleIdOrderByCreationDateDesc(String topId, String moduleId);
}
