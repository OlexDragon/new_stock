package irt.components.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class ComponentsResponse {

	@JsonProperty("odata.metadata")
	private String url;

	@JsonProperty("value")
	private Component[] components;
}
