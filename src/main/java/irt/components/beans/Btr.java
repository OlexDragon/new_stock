package irt.components.beans;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class Btr {

	private String serialNumber;
	private Map<String, String> data;
}
