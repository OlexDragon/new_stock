package irt.components.beans.jpa.repository;

import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.CalibrationGainSettings;

public interface CalibrationGainSettingRepository extends CrudRepository<CalibrationGainSettings, String> {
}
