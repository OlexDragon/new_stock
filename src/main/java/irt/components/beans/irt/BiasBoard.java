package irt.components.beans.irt;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class BiasBoard {

	private String title;

	@JsonProperty("class")
	private String className;

	@JsonProperty("power")
	@JsonAlias("det1")
	private IrtValue power;

	@JsonProperty("refl_power")
	@JsonAlias("det2")
	private IrtValue reflPower;

	private BigDecimal temperature;
}
