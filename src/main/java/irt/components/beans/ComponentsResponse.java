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

	@JsonProperty("Parent\\@navigationLinkUrl")
	private String parentUrl;

	@JsonProperty("MeasurementUnit\\@navigationLinkUrl")
	private String measurementUnitUrl;

	@JsonProperty("IRT_DefaultSchematicLetter\\@navigationLinkUrl")
	private String schematicLetterUrl;

	@JsonProperty("ReportUOM\\@navigationLinkUrl")
	private String reportUOM_Url;
}
