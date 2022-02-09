package irt.components.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class BomLetter {

	@JsonProperty("Description")
	private String letter;
}
