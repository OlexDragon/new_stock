package irt.components.beans.irt.calibration;

import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class HPBMRegister {

	@JsonProperty("ePSU_2 42V")
	Properties ePSU_2_42V;

	@JsonProperty("ePSU_3 42V")
	Properties ePSU_3_42V;

}
