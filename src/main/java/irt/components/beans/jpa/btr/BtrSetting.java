package irt.components.beans.jpa.btr;

import irt.components.services.converter.JSonDoubleArrayConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
