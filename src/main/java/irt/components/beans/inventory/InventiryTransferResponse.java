package irt.components.beans.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class InventiryTransferResponse {

	@JsonProperty("odata.metadata")
	private String url;

	@JsonProperty("value")
	private InventoryTransfer[] inventoryTransfers;
}
