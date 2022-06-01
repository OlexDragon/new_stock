package irt.components.beans.irt.calibration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class CalibrationMode {

	@JsonProperty("Calibration mode")
	private Status status;

	public enum Status{
		OFF,
		ON;
	}
}
