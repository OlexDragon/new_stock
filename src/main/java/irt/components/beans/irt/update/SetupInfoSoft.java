package irt.components.beans.irt.update;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor @Getter @Setter
public class SetupInfoSoft {

	public final static String setupInfoPathern = "system %2$s.any { image  { path { image.bin }}}";

	private final String typeRev;

	@Override
	public String toString() {
		return String.format(setupInfoPathern, typeRev);
	}
}
