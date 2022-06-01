package irt.components.beans.jpa.repository.calibration;

import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.calibration.CalibrationGainSettings;

public interface CalibrationGainSettingRepository extends CrudRepository<CalibrationGainSettings, String> {
}
