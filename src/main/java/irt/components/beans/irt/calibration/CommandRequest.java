package irt.components.beans.irt.calibration;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class CommandRequest {

	private String hostName;
	private String spName;
	private List<Command> commands;
}
