package irt.components.beans.irt.update;

import java.io.IOException;
import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class Soft extends TarToBytes{

	private final String typeRev;
	private Boolean module;	// Three states: null = System; false = Module of Module; true = Module

	public Soft(String typeRev, Path path) throws IOException {
		super(path);
		this.typeRev = typeRev;
	}

	public Soft(String typeRev, String fileName, byte[] bytes) {
		super(fileName, bytes);
		this.typeRev = typeRev;
	}

	@Override
	protected SetupInfo getSetupInfo(String fileName) {
		final SetupInfoSoft setupInfoSoft = new SetupInfoSoft(typeRev, fileName);
		return setupInfoSoft;
	}

	@Override
	public byte[] toBytes() throws IOException {

		if(module!=null && module) {
			final byte[] bytes = super.toBytes();
			final String fn = fileName.split("\\.")[0] + ".pkg";
			return new Soft(typeRev, fn, bytes).toBytes();
		}
		return super.toBytes();
	}
}
