package irt.components.values;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class RelatedFile {

	@JsonProperty("Ref_Key")
	private String key;

	@JsonProperty("Author")
	private String authorKey;

	@JsonProperty("FileOwner_Key")
	private String fileOwnerKey;

	@JsonProperty("PathToFile")
	private String pathToFile;
}
