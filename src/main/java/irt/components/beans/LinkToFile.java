package irt.components.beans;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @ToString
public class LinkToFile {

	@Setter
	private static String host;

	private final String fileName;
	private final URI uri;

	public LinkToFile(String pathToFile) throws MalformedURLException, URISyntaxException, UnsupportedEncodingException {

		final File file = new File(host + "\\files", pathToFile);
		uri = file.toURI();
		fileName = file.getName();
	}
}
