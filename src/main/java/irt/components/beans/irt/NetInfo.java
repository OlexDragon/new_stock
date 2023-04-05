package irt.components.beans.irt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class NetInfo {

	private String mac;
	private String type;
	private String isup;
	private String addr;
	private String mask;
	private String gw;
}
