package irt.components.beans.irt;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class DacsDP {
	Range range;
	private List<DigitalPotentiometer> list;
}
