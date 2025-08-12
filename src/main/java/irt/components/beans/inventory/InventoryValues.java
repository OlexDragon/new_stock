package irt.components.beans.inventory;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@RequiredArgsConstructor @Getter @Setter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryValues {

	@JsonProperty("odata.metadata")
	private final String metadata;

	@JsonProperty("Inventory")
	private final List<Inventory> inventories = new ArrayList<>();
}
