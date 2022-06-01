package irt.components.beans.jpa.calibration;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import irt.components.services.JSonConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString
public class CalibrationBtrSetting {

	@Id private String partNumber;

	@Convert(converter = JSonConverter.class)
	private Double[] frequencies;

	@Convert(converter = JSonConverter.class)
	private Double[] temperatures;

	@Convert(converter = JSonConverter.class)
	private Double[] detectors;

	@Column(name = "has_p1db")
	private boolean hasP1db;

	public void set(CalibrationBtrSetting btrSetting) {
		setFrequencies(btrSetting.frequencies);
		setTemperatures(btrSetting.temperatures);
		setDetectors(btrSetting.detectors);
		setHasP1db(btrSetting.hasP1db);
	}
}
