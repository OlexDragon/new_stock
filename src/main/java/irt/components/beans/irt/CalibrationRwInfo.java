package irt.components.beans.irt;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class CalibrationRwInfo {

	// 250.31 Controller

	@JsonProperty("digitalPotentiometers")
	@JsonAlias("dp")
	private List<DigitalPotentiometers> digitalPotentiometers;
	private List<UnitModule> linearizer;
	private List<UnitModule> fan;
	@JsonProperty("fcm")
	@JsonAlias("fcm_dacs")
	private Fcm fcm;

	// 100.21 Controller
	@JsonProperty("calMode")
	@JsonAlias("enable")
	private Boolean calMode;
	@JsonProperty("calibrationRwInfo")
	@JsonAlias("gates")
	private CalibrationRwInfo calibrationRwInfo;
	@JsonProperty("dacs")
	@JsonAlias("dac")
	DacsDP dacs;
}
