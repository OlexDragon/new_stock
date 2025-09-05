package irt.components.beans.jpa.calibration;

import java.math.BigDecimal;

import jakarta.persistence.Column;
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
public class CalibrationPowerOffsetSettings {

	@Id private String partNumber;
	@Column(precision = 12, scale = 9) private BigDecimal startValue;
	@Column(precision = 12, scale = 9) private BigDecimal stopValue;
	private String name;

//	@PrePersist
//	@PreUpdate
//	public void precisionConvertion() {
//        this.startValue	.setScale(9, RoundingMode.HALF_UP);
//        this.stopValue	.setScale(9, RoundingMode.HALF_UP);
//	}
}
