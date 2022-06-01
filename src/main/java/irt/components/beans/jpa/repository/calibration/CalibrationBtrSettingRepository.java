package irt.components.beans.jpa.repository.calibration;

import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.calibration.CalibrationBtrSetting;

public interface CalibrationBtrSettingRepository extends CrudRepository<CalibrationBtrSetting, String> {
}
