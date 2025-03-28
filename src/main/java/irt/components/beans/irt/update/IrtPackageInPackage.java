package irt.components.beans.irt.update;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class IrtPackageInPackage extends TarToBytes{

	private final String serialNumber;

	public IrtPackageInPackage(String sn, MultipartFile file) throws IOException {
		super(file.getOriginalFilename(), file.getBytes());
		serialNumber = sn;
	}

	@Override
	protected SetupInfo getSetupInfo(String fileName) {

		return new SetupInfoUpdate(serialNumber, fileName);
	}
}
