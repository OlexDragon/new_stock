package irt.components.values;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class RelatedFilesResponse {

	@JsonProperty("odata.metadata")
	private String metadata;

	@JsonProperty("value")
	private RelatedFile[] relatedFiles;
}
