package irt.components.beans.irt;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import irt.components.beans.irt.calibration.Diagnostics;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class HWInfo implements Diagnostics{

	@Setter
	private Integer moduleIndex;
	@JsonProperty("sequence")
	@JsonAlias("Bias sequence")
	private String sequence;

	@JsonProperty("calMode")
	@JsonAlias("Calibration mode")
	private Status calMode;

	@JsonProperty("switchLO")
	@JsonAlias("Switch LO")
	private Status switchLO;

	public enum Status{
		OFF,
		ON,
		NotAplicable;
	}
}
