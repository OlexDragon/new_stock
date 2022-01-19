package irt.components.values.units;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class Info {

	@JsonProperty("Product name")
	private String name;

	@JsonProperty("Serial number")
	private String serialNumber;

	@JsonProperty("Part number")
	private String partNumber;

	@JsonProperty("Device ID")
	private String deviceId;

	@JsonProperty("Software version")
	private String softVertion;

	@JsonProperty("Build")
	private String buildDate;

	@JsonProperty("Uptime")
	private String uptimeCounter;
}
