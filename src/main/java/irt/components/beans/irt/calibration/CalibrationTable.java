package irt.components.beans.irt.calibration;

import java.util.List;

import irt.components.beans.irt.update.TableValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor @Getter @Setter @ToString
public class CalibrationTable {

	private String serialNumber;
	private List<TableValue> table;
}
