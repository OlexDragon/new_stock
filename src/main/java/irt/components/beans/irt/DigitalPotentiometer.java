package irt.components.beans.irt;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class DigitalPotentiometer {

	private Integer index;
	private String name;
	private Integer value;
	private Range range;

	private List<DigitalPotentiometer> vars;
}
