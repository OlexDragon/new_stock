package irt.components.beans.irt.calibration;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class HPBMRegisterV21 {

	@JsonProperty("powerSupply1")
	@JsonAlias("ePSU_2 42V")
	HPBMRegisterV31 powerSupply1;

	@JsonProperty("powerSupply2")
	@JsonAlias("ePSU_3 42V")
	HPBMRegisterV31 powerSupply2;
}
