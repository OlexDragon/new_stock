package irt.components.beans.irt.calibration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString  @JsonIgnoreProperties(ignoreUnknown = true)
public class OneCeSectionTosave {

	private String serialNumber;
	private String section;
	private String body;
}
