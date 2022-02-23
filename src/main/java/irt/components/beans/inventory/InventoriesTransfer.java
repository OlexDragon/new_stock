package irt.components.beans.inventory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor @Getter @Setter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class InventoriesTransfer {

	@JsonProperty("Ref_Key")
	private String transferKey;

//	@JsonProperty("Number")
//	private String number;
//
//	@JsonProperty("Comment")
//	private String comment;

	@JsonProperty("Inventory")
	private List<Inventory> inventories;
//
//	@JsonProperty("DeletionMark")
//	private boolean deleted = true;
//
//	@JsonProperty("StructuralUnit_Key")
//	private final String fromKey = "49d8d057-81e1-11eb-b0b4-04d4c452793b"; // Stock
//
//	@JsonProperty("StructuralUnitPayee_Key")
//	private final String toKey = "0221fb27-7df3-11ec-b0bd-04d4c452793b"; // Production
//
//	@JsonProperty("OperationKind")
//	private String operation = "Transfer";
//
//	@JsonProperty("Date")
//	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
//	private Date date = new Date();

	public void addInventory(Inventory inventory) {

		if(inventories==null)
			inventories = new ArrayList<>();

		inventories.add(inventory);
	}

//	public InventoriesTransfer(String unitSerialNumber, String comment, Inventory inventory) {
//		serialNumbers.add(unitSerialNumber);
//		this.comment = comment;
//		inventories.add(inventory);
//	}
}
