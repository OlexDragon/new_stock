package irt.components.beans.irt;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class CalibrationRwInfo {

	@JsonProperty("fcmDacs")
	@JsonAlias("fcm_dacs")
	private DigitalPotentiometers fcmDacs;
	@JsonProperty("digitalPotentiometers")
	@JsonAlias("dp")
	private DigitalPotentiometers dp;
	private List<UnitModule> linearizer;
	private List<UnitModule> fan;
}
