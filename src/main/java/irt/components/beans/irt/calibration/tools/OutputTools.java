package irt.components.beans.irt.calibration.tools;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum OutputTools {

	HP438A("++read:true");

	private final String commands;
}
