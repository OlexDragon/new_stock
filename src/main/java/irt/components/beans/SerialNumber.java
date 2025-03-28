package irt.components.beans;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor @Getter @Setter @ToString
public class SerialNumber{

	private Long	id;
	private String 	serialNumber;
	private PartNumber partNumber;
}
