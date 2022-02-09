package irt.components.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class ComponentQuantityResponse {

	@JsonProperty("odata.metadata")
	private String metadata;

	@JsonProperty("value")
	private ComponentQuantity[] componentQuantities;
}
