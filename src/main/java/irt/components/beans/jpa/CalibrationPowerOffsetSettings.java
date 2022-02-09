package irt.components.beans.jpa;

import java.math.BigDecimal;

import javax.persistence.Column;
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
public class CalibrationPowerOffsetSettings {

	@Id private String partNumber;
	@Column(precision = 12, scale = 9) private BigDecimal startValue;
	@Column(precision = 12, scale = 9) private BigDecimal stopValue;

//	@PrePersist
//	@PreUpdate
//	public void precisionConvertion() {
//        this.startValue	.setScale(9, RoundingMode.HALF_UP);
//        this.stopValue	.setScale(9, RoundingMode.HALF_UP);
//	}
}
