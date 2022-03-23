package irt.components.beans.irt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class ConverterInfo {

	@JsonProperty("Frequency")
	private String loFrequency;

	@JsonProperty("Attenuation")
	private String attenuation;

	@JsonProperty("Mute")
	private String muteStatus;
}
