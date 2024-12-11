package irt.components.beans.irt.calibration;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter @Setter @ToString(callSuper = true) @JsonIgnoreProperties(ignoreUnknown = true)
public class CommandRequest extends RequestData {

	private List<Command> commands;
	@Accessors(chain = true)
	private String error;
	private int addr;
}
