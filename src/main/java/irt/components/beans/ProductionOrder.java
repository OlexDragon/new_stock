package irt.components.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class ProductionOrder {

	@JsonProperty("Ref_Key")
	private String key;

	@JsonProperty("Number")
	private String number;

	@JsonProperty("SalesOrder")
	private String salesOrder;

	@JsonProperty("Start")
	private String start;

	@JsonProperty("Finish")
	private String finish;

	@JsonProperty("StructuralUnit_Key")
	private String structuralUnitKey;

	@JsonProperty("Comment")
	private String comment;

	@JsonProperty("OrderState_Key")
	private String orderStateKey;

	@JsonProperty("Closed")
	private Boolean closed;

	@JsonProperty("Author_Key")
	private String authorKey;

	@JsonProperty("ProductsList")
	private String productsList;

	@JsonProperty("Priority_Key")
	private String priorityKey;

	@JsonProperty("Specification_Key")
	private String specificationKey;
}
