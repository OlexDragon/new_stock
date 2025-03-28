package irt.components.beans.irt.calibration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString  @JsonIgnoreProperties(ignoreUnknown = true)
public class OneCeSection {

	private String serialNumber;
	private String section;
	private String fieldName;
	private String value;

	@JsonProperty("Component")
	private String component;

	@JsonProperty("Setting")
	private String setting;

	@JsonProperty("PCB")
	private String pcb;

	@JsonProperty("SoftwareBuild")
	private String softwareBuild;

	@JsonProperty("FWVersion")
	private String fwVersion;

	@JsonProperty("Profile")
	private String profile;
}
