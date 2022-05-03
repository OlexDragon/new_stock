package irt.components.beans.irt.calibration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import irt.components.services.BytesToStringSerializer;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class Command {

	private String command;
	private boolean getAnswer;
	@JsonSerialize(using = BytesToStringSerializer.class)
	private byte[] answer;
}
