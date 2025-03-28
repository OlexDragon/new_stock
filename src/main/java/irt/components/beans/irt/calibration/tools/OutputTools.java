package irt.components.beans.irt.calibration.tools;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum OutputTools {

	HP438A		("?"),	// "AP" - Sensor A; "BP" - Sensor B
	HP_EMP_441A	("meas?");

	private final String commands;
}
