package irt.components.beans.irt.update;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor @Getter @Setter
public class SetupInfoProfile {

	public final static String setupInfoPathern = "%1$s any.any.any.%2$s { profile { path { %2$s.bin }}}";

	private final String serialNumber;
	private boolean module;

	@Override
	public String toString() {

		String loadTo = module ? "file" : "system";

		return String.format(setupInfoPathern, loadTo, serialNumber);
	}
}
