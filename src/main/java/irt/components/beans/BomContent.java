package irt.components.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class BomContent {

	@JsonProperty("Quantity")
	private Integer quantity;

	@JsonProperty("IRT_Reference")
	private String reference;

	@JsonProperty("Products")
	private Component component;

	@JsonProperty("IRT_SchematicLetter")
	private BomLetter bomLetter;
}
