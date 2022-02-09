package irt.components.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class LocationResponse {

	@JsonProperty("odata.metadata")
	private String url;

	@JsonProperty("value")
	private Location[] locations;
}
