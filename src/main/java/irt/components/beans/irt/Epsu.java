package irt.components.beans.irt;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class Epsu {

	private String title;

	@JsonProperty("class")
	private String className;

	private Integer visible;
	private List<PowerSuply> data;
}
