package irt.components.beans;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class RmaRequest {

	private String name;
	private String email;
	private String sn;
	private String cause;
}
