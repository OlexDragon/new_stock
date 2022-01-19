package irt.components.values;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class Component {

	@JsonProperty("Ref_Key")
	private String key;

	@JsonProperty("ProductsType")
	private String productsType;

	@JsonProperty("SKU")
	private String partNumber;

	@JsonProperty("MfrPNs")
	private String mfrPN;

	@JsonProperty("Description")
	private String description;
}
