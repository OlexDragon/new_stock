package irt.components.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class ComponentQuantity {

	@JsonProperty("Products")
	private Component component;

	@JsonProperty("StructuralUnit")
	private Location location;

	@JsonProperty("QuantityBalance")
	private Integer qty;
}
