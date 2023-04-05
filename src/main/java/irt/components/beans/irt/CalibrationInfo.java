package irt.components.beans.irt;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class CalibrationInfo {

	private Bias bias;
	private Boards boards;

	// For Old BIAS Board
	private IrtValue power;
	private BigDecimal temperature;
	private IrtValue hss1;
	private IrtValue hss2;
}
