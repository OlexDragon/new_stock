package irt.components.beans.irt.calibration;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class InitializeSetting {

	private String deviceId;
	private Integer regIndex;
	private Map<String, Integer> nameValue;
}
