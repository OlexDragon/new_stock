package irt.components.beans.irt.calibration;

import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class HPBMRegisterV21 {

	@JsonProperty("switch1")
	@JsonAlias("ePSU_2 42V")
	Properties switch1;

	@JsonProperty("switch2")
	@JsonAlias("ePSU_3 42V")
	Properties switch2;
}
