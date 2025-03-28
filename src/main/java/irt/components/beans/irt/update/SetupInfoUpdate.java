package irt.components.beans.irt.update;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor @Getter @Setter
public class SetupInfoUpdate implements SetupInfo {

	public final static String setupInfoPathern = "package any.any.any.%s { upgrade  { path { %s }}}";

	private final String serialNumber;
	private final String fileName;

	@Override
	public String toString() {

		return String.format(setupInfoPathern,  serialNumber, fileName);
	}
}
