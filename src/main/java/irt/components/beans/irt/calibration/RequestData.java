package irt.components.beans.irt.calibration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import irt.components.beans.Baudrate;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class RequestData {

	private String hostName;
	private String spName;
	/** baudrate - This variable is not used with NI GPIB  */
	private Baudrate baudrate;
	private int timeout = 100;
}
