package irt.components.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class Location {

	@JsonProperty("Ref_Key")
	private String key;

	@JsonProperty("Description")
	private String description;

	@JsonProperty("PredefinedDataName")
	private String dataName;
}
