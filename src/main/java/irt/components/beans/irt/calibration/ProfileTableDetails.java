package irt.components.beans.irt.calibration;

import java.util.Arrays;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor @Getter @ToString
public enum ProfileTableDetails {

	OUTPUT_POWER("Output Power"			, new String[]{"out-power", "power-out", "power"}),
	POWER_OFFSET("Power Offset"			, new String[]{"power-out-freq", "frequency"}),
	TEMPERATURE	("Gain over temperature", new String[]{"temperature"});

	private final String description;
	private final String[] names;

	public static Optional<ProfileTableDetails> valueByDescription(String description) {
		return Arrays.stream(values()).parallel().filter(ptd->ptd.description.equals(description)).findAny();
	}
}
