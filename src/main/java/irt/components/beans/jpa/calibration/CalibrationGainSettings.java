package irt.components.beans.jpa.calibration;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString
public class CalibrationGainSettings {

	@Id private String partNumber;
	private Integer startValue;
	private Integer stopValue;
	private Integer fields;
	private Boolean p1dB;
	private Boolean localPn;
}
