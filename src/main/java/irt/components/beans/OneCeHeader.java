package irt.components.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor @Getter @Setter @ToString
public class OneCeHeader{

	@JsonProperty("Product")
	private String	product;
	@JsonProperty("Description")
	private String 	description;
	@JsonProperty("Notes")
	private String notes;
	@JsonProperty("SerialNumberPrefix")
	private String	serialNumberPrefix;
	@JsonProperty("CSACertified")
	private String 	csaCertified;
	@JsonProperty("Redundancy")
	private String redundancy;
	@JsonProperty("OEM")
	private String	oem;
	@JsonProperty("Switch")
	private String 	hasSwitch;
	@JsonProperty("SalesSKU")
	private String salesSKU;
}
