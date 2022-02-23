package irt.components.beans.inventory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor @Getter @Setter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class Inventory {

	@JsonProperty("Ref_Key")
	private String transferKey;

	@JsonProperty("LineNumber")
	private Integer lineNumber;

	@JsonProperty("Products_Key")
	private String productKey;

	@JsonProperty("Quantity")
	private int qty;

	@JsonProperty("MeasurementUnit")
	private final String measurementUnitKey = "b34663a4-4d2f-11ec-b0b9-04d4c452793b";

//	@JsonProperty("MeasurementUnit_Type")
//	private final String measurementUnitType = "StandardODATA.Catalog_UOMClassifier";
//
//	@JsonProperty("BusinessLine_Key")
//	private final String businessLineKey = "553993af-81e1-11eb-b0b4-04d4c452793b";
//
//	@JsonProperty("InventoryGLAccount_Key")
//	private final String inventoryGLAccountKey = "d2c21f6e-542c-11e8-878d-b06ebfcc6763";
//
//	@JsonProperty("InventoryToGLAccount_Key")
//	private final String inventoryToGLAccountKey = "d2c21f6e-542c-11e8-878d-b06ebfcc6763";
//
//	@JsonProperty("InventoryReceivedGLAccount_Key")
//	private final String inventoryReceivedGLAccountKey = "e99ea6b7-46fa-11e9-87a5-b06ebfbbdfac";
}
