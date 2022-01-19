package irt.components.jpa.repository;

import org.springframework.data.repository.CrudRepository;

import irt.components.jpa.beans.CalibrationOutputPowerSettings;

public interface CalibrationOutputPowerSettingRepository extends CrudRepository<CalibrationOutputPowerSettings, String> {
}
