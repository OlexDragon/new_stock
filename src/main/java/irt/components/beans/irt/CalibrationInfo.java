package irt.components.beans.irt;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class CalibrationInfo {

	private Bias bias;
	private Boards boards;
	private Epsu epsu;

	// For Old BIAS Board
	private IrtValue power;
	private BigDecimal temperature;
	@Setter
	private IrtValue hss1;
	@Setter
	private IrtValue hss2;
}
