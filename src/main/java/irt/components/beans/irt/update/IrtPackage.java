package irt.components.beans.irt.update;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class IrtPackage extends TarToBytes{
	private final static Logger logger = LogManager.getLogger();

	public IrtPackage(MultipartFile file) {
		super(file.getName(), toBytes(file));
	}

	@Override
	public byte[] toBytes() throws IOException {
		return bytes;
	}

	private static byte[] toBytes(MultipartFile file) {

		try {

			return file.getBytes();

		} catch (IOException e) {
			logger.catching(e);
			return null;
		}
	}

	@Override
	protected SetupInfo getSetupInfo(String fileName) {
		return null;
	}
}
