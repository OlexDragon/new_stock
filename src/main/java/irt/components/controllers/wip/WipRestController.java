package irt.components.controllers.wip;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("wip/rest")
public class WipRestController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.wip.directory}")
	private String wipDirectory;

	@PostMapping("files")
	public List<SimpleEntry<String, Long>> getWipFiles() throws IOException {
		logger.traceEntry(wipDirectory);

		final File directory = Paths.get(wipDirectory).toFile();
		final File[] listFiles = directory.listFiles((d,fn)->fn.toLowerCase().startsWith("wip"));
		final List<AbstractMap.SimpleEntry<String, Long>> toSend = new ArrayList<>();

		for(int i=0; i<listFiles.length; i++) {
			File f = listFiles[i];
			toSend.add(new AbstractMap.SimpleEntry<>(f.getName(), Files.readAttributes(f.toPath(), BasicFileAttributes.class).lastModifiedTime().toMillis()));
		}

		return toSend;
	}
}
