package irt.components.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class Invertory {

	@JsonProperty("Ref_Key")
	private String key;

	@JsonProperty("Products_Key")
	private String productsKey;
}
