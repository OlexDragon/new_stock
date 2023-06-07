package irt.components.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class SalesOrder {

	@JsonProperty("Ref_Key")
	private String key;

	@JsonProperty("Number")
	private String number;

	@JsonProperty("Date")
	private String date;

	@JsonProperty("Start")
	private String start;

	@JsonProperty("ShipmentDate")
	private String shipmentDate;

	@JsonProperty("Posted")
	private Boolean posted;

	@JsonProperty("Author_Key")
	private String authorKey;

	@JsonProperty("OperationKind")
	private String operationKind;

	@JsonProperty("Contract_Key")
	private String contractKey;

	@JsonProperty("Closed")
	private Boolean closed;

	@JsonProperty("OrderState_Key")
	private String orderStateKey;

	@JsonProperty("Comment")
	private String comment;

	@JsonProperty("Company_Key")
	private String companyKey;
}
