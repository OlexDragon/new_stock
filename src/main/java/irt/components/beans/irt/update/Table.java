package irt.components.beans.irt.update;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor @AllArgsConstructor @Getter @ToString
public class Table {

	private String serialNumber;
	private String name;
	private List<TableValue> values;
}
