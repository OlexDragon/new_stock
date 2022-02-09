package irt.components.beans;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor @Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class InventoriesTransfer {

	@JsonProperty("SerialNumbers")
	private final List<String> serialNumbers = new ArrayList<>();

	@JsonProperty("Comment")
	private String comment;

	@JsonProperty("Inventory")
	private final List<Inventory> inventories = new ArrayList<>();

	@JsonProperty("Ref_Key")
	private String transferKey;

	@JsonProperty("DeletionMark")
	private boolean deleted = false;

	@JsonProperty("Number")
	private String number;

	@JsonProperty("StructuralUnit_Key")
	private String fromKey = "49d8d057-81e1-11eb-b0b4-04d4c452793b"; // Stock

	@JsonProperty("StructuralUnitPayee_Key")
	private String toKey = "0221fb27-7df3-11ec-b0bd-04d4c452793b"; // Production

	@JsonProperty("OperationKind")
	private String operation = "Transfer";

	public InventoriesTransfer(String unitSerialNumber, String comment, Inventory inventory) {
		serialNumbers.add(unitSerialNumber);
		this.comment = comment;
		inventories.add(inventory);
	}
}
