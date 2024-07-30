package irt.components.beans.irt.update;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Soft implements ToUpload{
	private final Logger logger = LogManager.getLogger();

	private final Path path;
	private final String typeRev;

	public Soft(String typeRev, Path path) {

		if(!path.toFile().isFile())
			throw new IllegalArgumentException(path.getFileName() + " software file does not exists.");

		this.path = path;
		this.typeRev = typeRev;
	}

	/**
	 * @return TAR file as bytes
	 * @throws IOException
	 */
	@Override
	public byte[] toBytes() throws IOException {

		try(	ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				TarArchiveOutputStream tarArchiveOutputStream = new TarArchiveOutputStream(byteArrayOutputStream);){

			final String fileName = path.getFileName().toString();

			// Profile
			 final byte[] soft = Files.readAllBytes(path);
			 final TarArchiveEntry ieProfile = new TarArchiveEntry(fileName);
			 ieProfile.setSize(soft.length);
			 tarArchiveOutputStream.putArchiveEntry(ieProfile);
			 tarArchiveOutputStream.write(soft);
			 tarArchiveOutputStream.closeArchiveEntry();

			// setup.info
			final SetupInfoSoft si = new SetupInfoSoft(typeRev);
			logger.debug(si);
			byte[] setupInfo = si.toString().getBytes();
			final TarArchiveEntry ieSetupInfo = new TarArchiveEntry("setup.info");
			ieSetupInfo.setSize(setupInfo.length);
			tarArchiveOutputStream.putArchiveEntry(ieSetupInfo);
			tarArchiveOutputStream.write(setupInfo);
			tarArchiveOutputStream.closeArchiveEntry();

			// setup.md5
			final String setupInfoMd5 = new Md5(setupInfo).toString();
			StringBuilder sbSetupMd5 = new StringBuilder(setupInfoMd5).append(" *").append("setup.info").append('\n');

			final String profileMd5 = new Md5(soft).toString();
			sbSetupMd5.append(profileMd5).append(" *").append(fileName);

			final byte[] setupMd5 = sbSetupMd5.toString().getBytes();
			final TarArchiveEntry ieSetupMd5 = new TarArchiveEntry("setup.md5");
			ieSetupMd5.setSize(setupMd5.length);
			tarArchiveOutputStream.putArchiveEntry(ieSetupMd5);
			tarArchiveOutputStream.write(setupMd5);
			tarArchiveOutputStream.closeArchiveEntry();

			return byteArrayOutputStream.toByteArray();
		}
	}
}
