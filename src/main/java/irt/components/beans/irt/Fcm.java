package irt.components.beans.irt;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class Fcm {

	@JsonProperty("calMode")
	@JsonAlias("enable")
	private Boolean calMode;
	@JsonProperty("dacs")
	@JsonAlias({"dac", "list"})
	List<DigitalPotentiometer> dacs;
}
