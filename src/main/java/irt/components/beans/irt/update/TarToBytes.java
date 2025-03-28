package irt.components.beans.irt.update;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

public abstract class TarToBytes implements ToUpload {

	protected final String fileName;
	protected final byte[] bytes;

	public TarToBytes(Path path) throws IOException {

		fileName = path.getFileName().toString();

		final File file = path.toFile();
		if(file.exists() && file.isFile())
			bytes = Files.readAllBytes(path);
		else
			throw new IllegalArgumentException(fileName + " IrtProfile file does not exists.");
	}

	public TarToBytes(String fileName, byte[] bytes) {
		this.fileName = fileName;
		this.bytes = Objects.requireNonNull(bytes);
	}

	/**
	 * @return TAR file as bytes
	 * @throws IOException
	 */
	@Override
	public byte[] toBytes() throws IOException {

		try(	ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				TarArchiveOutputStream tarArchiveOutputStream = new TarArchiveOutputStream(byteArrayOutputStream);){


			// IrtProfile
			 final TarArchiveEntry ieSoft = new TarArchiveEntry(fileName);
			 ieSoft.setSize(bytes.length);
			 tarArchiveOutputStream.putArchiveEntry(ieSoft);
			 tarArchiveOutputStream.write(bytes);
			 tarArchiveOutputStream.closeArchiveEntry();

			// setup.info
			final SetupInfo si = getSetupInfo(fileName);
			byte[] setupInfo = si.toString().getBytes();
			final TarArchiveEntry ieSetupInfo = new TarArchiveEntry("setup.info");
			ieSetupInfo.setSize(setupInfo.length);
			tarArchiveOutputStream.putArchiveEntry(ieSetupInfo);
			tarArchiveOutputStream.write(setupInfo);
			tarArchiveOutputStream.closeArchiveEntry();

			// setup.md5
			final String setupInfoMd5 = new Md5(setupInfo).toString();
			StringBuilder sbSetupMd5 = new StringBuilder(setupInfoMd5).append(" *").append("setup.info").append('\n');

			final String profileMd5 = new Md5(bytes).toString();
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

	protected abstract SetupInfo getSetupInfo(String fileName);
}
