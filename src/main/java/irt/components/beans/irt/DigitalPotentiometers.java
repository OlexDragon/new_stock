package irt.components.beans.irt;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class DigitalPotentiometers {

	// 250.31 Controller
	@JsonProperty("calMode")
	@JsonAlias("enable")
	private Boolean calMode;
	private List<UnitModule> list;
	private List<DigitalPotentiometer> vars;

	// 100.21 Controller
	Range range;
}
