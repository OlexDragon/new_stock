package irt.components.beans.irt.calibration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class CommandBytesRequest extends RequestData {

	private byte[] toSend;
	private byte[] answer;
	private Byte termination;
	private boolean getAnswer;
	private String errorMessage;
	private int timeout = 200;
}
