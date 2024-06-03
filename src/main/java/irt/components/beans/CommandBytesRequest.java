package irt.components.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class CommandBytesRequest implements SerialPortData {

	private String spName;
	/** baudrate - This variable is not used with NI GPIB  */
	private Baudrate baudrate = Baudrate.BAUDRATE_115200;
	private byte[] toSend;
	private byte[] answer;
	private Byte termination = TerminationSymbol.IRT.getSymbol();
	private int timeout = 100;
	private boolean getAnswer = true;
	private String errorMessage;
}
