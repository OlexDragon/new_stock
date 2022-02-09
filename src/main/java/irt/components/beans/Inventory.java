package irt.components.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class Inventory {

	@JsonProperty("Products_Key")
	private String productKey;

	@JsonProperty("Quantity")
	private int qty;

	@JsonProperty("SerialNumbers")
	private String serialNumbers;

	@JsonProperty("Ref_Key")
	private String inventoryKey;
}
