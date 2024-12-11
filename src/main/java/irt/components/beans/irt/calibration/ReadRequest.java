package irt.components.beans.irt.calibration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import irt.components.beans.Baudrate;
import irt.components.beans.SerialPortData;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class ReadRequest implements SerialPortData {

	private String spName;
	private short addr;
	/** baudrate - This variable is not used with NI GPIB  */
	private Baudrate baudrate;
	private byte[] answer;
	private int timeout = 100;
	private String errorMessage;
}
