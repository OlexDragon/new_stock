package irt.components.beans.irt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class MonitorInfo {

	private String name;
	private Monitor data;

}
