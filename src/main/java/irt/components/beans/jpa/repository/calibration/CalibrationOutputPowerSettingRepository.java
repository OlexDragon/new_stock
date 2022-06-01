package irt.components.beans.jpa.repository.calibration;

import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.calibration.CalibrationOutputPowerSettings;

public interface CalibrationOutputPowerSettingRepository extends CrudRepository<CalibrationOutputPowerSettings, String> {
}
