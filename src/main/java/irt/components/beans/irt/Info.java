package irt.components.beans.irt;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import irt.components.beans.irt.calibration.Diagnostics;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class Info implements Diagnostics{

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

	private Integer moduleId;

	public int getDeviceType() {
		return getDeviceIdPart(0);
	}

	public int getTypeVersion() {
		return getDeviceIdPart(1);
	}

	private int getDeviceIdPart(int index) {
		return Optional.ofNullable(deviceId).map(id->id.split("\\.")).filter(split->split.length>index).map(id->id[index].replaceAll("\\D", "")).filter(id->!id.isEmpty()).map(Integer::parseInt).orElse(-1);
	}
}
