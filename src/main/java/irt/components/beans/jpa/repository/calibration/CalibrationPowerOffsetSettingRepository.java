package irt.components.beans.jpa.repository.calibration;

import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.calibration.CalibrationPowerOffsetSettings;

public interface CalibrationPowerOffsetSettingRepository extends CrudRepository<CalibrationPowerOffsetSettings, String> {
}
