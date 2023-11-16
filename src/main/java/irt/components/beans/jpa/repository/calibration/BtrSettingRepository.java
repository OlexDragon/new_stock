package irt.components.beans.jpa.repository.calibration;

import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.btr.BtrSetting;

public interface BtrSettingRepository extends CrudRepository<BtrSetting, String> {
}
