package irt.components.values.units;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class BiasBoard {

	@JsonProperty("title")
	private String title;

	@JsonProperty("class")
	private String className;

	private Value power;

	@JsonProperty("refl_power")
	private Value reflPower;

	private BigDecimal temperature;
}
