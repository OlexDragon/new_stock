package irt.components.beans.calibration.update;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public class SetupInfo {

	public final static String setupInfoPathern = "system any.any.any.%1$s { profile { path {%1$s.bin}}}";

	private final String serialNumber;

	@Override
	public String toString() {
		return String.format(setupInfoPathern, serialNumber);
	}
}
