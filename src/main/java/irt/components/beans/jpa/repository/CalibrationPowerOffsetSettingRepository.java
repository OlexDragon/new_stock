package irt.components.beans.jpa.repository;

import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.CalibrationPowerOffsetSettings;

public interface CalibrationPowerOffsetSettingRepository extends CrudRepository<CalibrationPowerOffsetSettings, String> {
}
