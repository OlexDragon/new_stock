package irt.components.beans.calibration;

import java.util.List;

import irt.components.beans.calibration.update.TableValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor @Getter @Setter
public class CalibrationTable {

	private String serialNumber;
	private List<TableValue> table;
}
