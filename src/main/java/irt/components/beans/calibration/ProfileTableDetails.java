package irt.components.beans.calibration;

import java.util.Arrays;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum ProfileTableDetails {

	OUTPUT_POWER("Output Power", new String[]{"out-power", "power"});

	private final String description;
	private final String[] names;

	public static Optional<ProfileTableDetails> valueByDescription(String description) {
		return Arrays.stream(values()).parallel().filter(ptd->ptd.description.equals(description)).findAny();
	}
}
