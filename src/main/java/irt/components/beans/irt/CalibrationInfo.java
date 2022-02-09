package irt.components.beans.irt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class CalibrationInfo {

	@JsonProperty("bias")
	private BiasBoard biasBoard;
}
