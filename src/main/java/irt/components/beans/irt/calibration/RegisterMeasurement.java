package irt.components.beans.irt.calibration;

import javax.persistence.Convert;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import irt.components.beans.irt.IrtValue;
import irt.components.services.converter.IrtValueConverter;
import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterMeasurement implements Diagnostics{

	@JsonProperty("temperature")
	@JsonAlias("Device temperature")
	@Convert(converter = IrtValueConverter.class)
	IrtValue temperature;

	@JsonProperty("inputPower")
	@JsonAlias("Device input power")
	@Convert(converter = IrtValueConverter.class)
	IrtValue inputPower;

	@JsonProperty("outputPower")
	@JsonAlias("Device output power")
	@Convert(converter = IrtValueConverter.class)
	IrtValue outputPower;

	@JsonProperty("reflectedPower")
	@JsonAlias("Device reflected power")
	@Convert(converter = IrtValueConverter.class)
	IrtValue reflectedPower;
}
