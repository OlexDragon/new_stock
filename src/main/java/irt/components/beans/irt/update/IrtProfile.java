package irt.components.beans.irt.update;

import java.io.IOException;
import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class IrtProfile extends TarToBytes{

	private boolean module;

	public IrtProfile(Path path) throws IOException {
		super(path);
	}

	@Override
	protected SetupInfo getSetupInfo(String fileName) {

		final String serialNumber = fileName.split("\\.")[0];
		final SetupInfoProfile si = new SetupInfoProfile(serialNumber);
		si.setModule(module);
		return si;
	}
}
