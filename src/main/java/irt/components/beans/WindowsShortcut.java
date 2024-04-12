package irt.components.beans;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class WindowsShortcut {

	private final File originalFile;
	private boolean isDirectory;
	private boolean isLocal;
	private Path path;

	public WindowsShortcut(File file) throws FileNotFoundException, IOException, ParseException {

		this.originalFile = file;
		final byte[] bytes = toBytes(file);
		parse(bytes);
	}

	private byte[] toBytes(File file) throws IOException, FileNotFoundException {

		byte[] byteArray;

		try(	InputStream in = new FileInputStream(file);
		        ByteArrayOutputStream bout = new ByteArrayOutputStream();){

			while(true) {

				final byte[] buff = new byte[256];
				int n = in.read(buff);

				if (n == -1) 
					break;

				bout.write(buff);
			}

			byteArray = bout.toByteArray();
		}
		return byteArray;
	}

	private void parse(byte[] bytes) throws ParseException {

		try {
			// get the flags byte
			byte flags = bytes[0x14];

			// get the file attributes byte
			final int file_atts_offset = 0x18;
			byte file_atts = bytes[file_atts_offset];
			byte is_dir_mask = (byte) 0x10;
			if ((file_atts & is_dir_mask) > 0) {
				isDirectory = true;
			} else {
				isDirectory = false;
			}

			// if the shell settings are present, skip them
			final int shell_offset = 0x4c;
			final byte has_shell_mask = (byte) 0x01;
			int shell_len = 0;
			if ((flags & has_shell_mask) > 0) {
				// the plus 2 accounts for the length marker itself
				shell_len = bytesToWord(bytes, shell_offset) + 2;
			}

			// get to the file settings
			int file_start = 0x4c + shell_len;

			final int file_location_info_flag_offset_offset = 0x08;
			int file_location_info_flag = bytes[file_start + file_location_info_flag_offset_offset];
			isLocal = (file_location_info_flag & 2) == 0;
			// get the local volume and local system values
			// final int localVolumeTable_offset_offset = 0x0C;
			final int basename_offset_offset = 0x10;
			final int networkVolumeTable_offset_offset = 0x14;
			final int finalname_offset_offset = 0x18;
			int finalname_offset = bytes[file_start + finalname_offset_offset] + file_start;
			String finalname = getNullDelimitedString(bytes, finalname_offset);

			if (isLocal) {

				int basename_offset = bytes[file_start + basename_offset_offset] + file_start;
				String basename = getNullDelimitedString(bytes, basename_offset);

				path = Paths.get(basename + finalname);

			} else {

				int networkVolumeTable_offset = bytes[file_start + networkVolumeTable_offset_offset] + file_start;
				int shareName_offset_offset = 0x08;
				int shareName_offset = bytes[networkVolumeTable_offset + shareName_offset_offset]
						+ networkVolumeTable_offset;
				String shareName = getNullDelimitedString(bytes, shareName_offset);

				path = Paths.get(shareName + "\\" + finalname);
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ParseException("Could not be parsed, probably not a valid WindowsShortcut", 0);
		}
	}

	private static String getNullDelimitedString(byte[] bytes, int off) {

		int index = off;

		// count bytes until the null character (0)
		while (index < bytes.length) {

			if (bytes[index] == 0)
				break;

			index++;
		}
		return new String(bytes, off, index-off);
	}

    /*
     * convert two bytes into a short note, this is little endian because it's
     * for an Intel only OS.
     */
    private static int bytesToWord(byte[] bytes, int off) {
        return ((bytes[off + 1] & 0xff) << 8) | (bytes[off] & 0xff);
    }
}
