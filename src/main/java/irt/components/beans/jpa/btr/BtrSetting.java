package irt.components.beans.jpa.btr;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import irt.components.services.JSonDoubleArrayConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString
public class BtrSetting {

	@Id private String partNumber;

	@Convert(converter = JSonDoubleArrayConverter.class)
	private Double[] frequencies;

	@Convert(converter = JSonDoubleArrayConverter.class)
	private Double[] temperatures;

	@Convert(converter = JSonDoubleArrayConverter.class)
	private Double[] detectors;

	@Column(name = "has_p1db")
	private boolean hasP1db;

	public void set(BtrSetting btrSetting) {
		setFrequencies(btrSetting.frequencies);
		setTemperatures(btrSetting.temperatures);
		setDetectors(btrSetting.detectors);
		setHasP1db(btrSetting.hasP1db);
	}
}
