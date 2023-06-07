package irt.components.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

import irt.components.beans.inventory.Inventory;
import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class ProductionOrderResponse {

	@JsonProperty("odata.metadata")
	private String metadata;

	@JsonProperty("value")
	private ProductionOrder[] productionOrders;

	@JsonProperty("Inventory")
	private Inventory[] inventorys;
}
