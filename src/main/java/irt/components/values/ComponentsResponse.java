package irt.components.values;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.ToString;

@ToString
public class ComponentsResponse {

	@JsonProperty("odata.metadata")
	private String url;

	@JsonProperty("value")
	private Component[] components;
}
