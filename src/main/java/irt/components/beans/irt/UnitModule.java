package irt.components.beans.irt;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class UnitModule {

	private String name;
	private Integer index;
	@JsonProperty("saveEnable")
	@JsonAlias("save_enable")
	private Boolean saveEnable;
	private List<DigitalPotentiometer> vars;
}
