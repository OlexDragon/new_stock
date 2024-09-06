package irt.components.beans.irt.update;

import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor @Getter @Setter
public class SetupInfoSoft implements SetupInfo {

	public final static String setupInfoPathern = "%s %s.any { %s { path { %s }}}";

	private final String typeRev;
	private final String fileName;
	private Boolean module;	// Three states: null = System; false = Module of Module; true = Module

	@Override
	public String toString() {

		final String system;
		final String image;

		if(fileName.endsWith(".pkg")) {

			system = "package";
			image = "upgrade";

		}else if(module==null) {

			system = "system";
			image = "image";

		}else {
			system = "package";
			image = "upgrade";
		}

		final String orElse = Optional.ofNullable(typeRev).orElse("any.any");

		return String.format(setupInfoPathern, system, orElse, image, fileName);
	}
}
