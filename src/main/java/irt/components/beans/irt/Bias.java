package irt.components.beans.irt;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class Bias {

	private String title;

	@JsonProperty("class")
	private String className;

	@JsonProperty("power1")
	@JsonAlias({"det1", "power"})
	private IrtValue power1;

	@JsonProperty("power2")
	@JsonAlias({"det2", "refl_power"})
	private IrtValue power2;

	private BigDecimal temperature;
}
