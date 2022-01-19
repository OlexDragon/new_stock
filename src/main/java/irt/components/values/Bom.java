package irt.components.values;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class Bom {

	@JsonProperty("Ref_Key")
	private String key;

	@JsonProperty("Owner")
	private Component owner;

	@JsonProperty("Status")
	private String status;

	@JsonProperty("Description")
	private String description;

	@JsonProperty("Content")
	private BomContent[] bomContents;
}
