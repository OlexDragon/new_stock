package irt.components.controllers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.activation.MimetypesFileTypeMap;
import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.smb.session.SmbSession;
import org.springframework.integration.smb.session.SmbSessionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import irt.components.controllers.eco.EcoController;
import irt.components.controllers.rma.RmaController;
import irt.components.workers.ThreadRunner;
import net.coobird.thumbnailator.Thumbnails;

@RestController
@RequestMapping("files")
public class FileRestController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.rma.files.path}") private String rmaFilesPath;
	@Value("${irt.eco.files.path}") private String ecoFilesPath;
	@Value("${irt.host}") 			private String host;
	@Autowired SmbSessionFactory smbSessionFactory;

	@PostConstruct
	public void postConstruct() {
        try {

    		// get system name
        	String sName = InetAddress.getLocalHost().getHostName();

        	// Change the directory if server runs on my computer
        	if(sName.equals("oleksandr")) {
        		rmaFilesPath = RmaController.TEST_PATH_TO_RMA_FILES;
        		ecoFilesPath = EcoController.TEST_PATH_TO_ECO_FILES;
        	}

        } catch (UnknownHostException e) {
			logger.catching(e);
		}
	}

	@GetMapping
	public ResponseEntity<Resource> download(@RequestParam String path) throws IOException{
		logger.traceEntry(path);

		//Bad file descriptor exception when using auto close.
		final SmbSession session = smbSessionFactory.getSession();
		final InputStream is = session.readRaw(path);
		final InputStreamResource body = new InputStreamResource(is);
		final HttpHeaders headers = getHeader();
		headers.add("Content-Disposition", "attachment; filename=\"" + new File(path).getName() + "\"");

		return ResponseEntity.ok()
				.headers(headers)
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(body);
	}

	private HttpHeaders getHeader() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");
		return headers;
	}

	@GetMapping("/rma/{commentID}/{fileName}")
	public ResponseEntity<Resource> rmaFile(@PathVariable  String commentID, @PathVariable String fileName) throws IOException{

		final Path path = Paths.get(rmaFilesPath, commentID, fileName);
		final File file = path.toFile();

		HttpHeaders headers = getHeader();
		headers.add("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
		final InputStream is = new FileInputStream(file);

		return ResponseEntity.ok()
				.headers(headers)
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(new InputStreamResource(is));
	}

	@GetMapping("/eco/{ecoID}/{fileName}")
	public ResponseEntity<Resource> ecoFile(@PathVariable  String ecoID, @PathVariable String fileName) throws IOException{

		final Path path = Paths.get(ecoFilesPath, ecoID, fileName);
		final File file = path.toFile();

		HttpHeaders headers = getHeader();
		headers.add("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
		final InputStream is = new FileInputStream(file);

		return ResponseEntity.ok()
				.headers(headers)
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(new InputStreamResource(is));
	}

	@GetMapping("/rma/thumbnails/{commentID}/{fileName}")
	public ResponseEntity<Resource> rmaThumbnails(@PathVariable  String commentID, @PathVariable String fileName) throws IOException{

		return getThumbnails(rmaFilesPath, commentID, fileName);
	}

	@GetMapping("/eco/thumbnails/{ecoID}/{fileName}")
	public ResponseEntity<Resource> ecoThumbnails(@PathVariable  String ecoID, @PathVariable String fileName) throws IOException{

		return getThumbnails(ecoFilesPath, ecoID, fileName);
	}

	public ResponseEntity<Resource> getThumbnails(String filesPath, String id, String fileName) throws FileNotFoundException, IOException {
		logger.traceEntry("filesPath: {}; id: {}; fileName:{}", filesPath, id, fileName);

		final Path thPath = Paths.get(filesPath, id, "thumbnails", fileName);
		final File thFile = thPath.toFile();

		if(thFile.exists()) {
			final FileInputStream inputStream = new FileInputStream(thFile);
			return ResponseEntity.ok()
					.headers(getHeader())
					.contentType(MediaType.APPLICATION_OCTET_STREAM)
					.body(new InputStreamResource(inputStream));
		}

		final Path path = Paths.get(filesPath, id, fileName);
		final File file = path.toFile();

		if(isImage(file))
			try(final ByteArrayOutputStream os = new ByteArrayOutputStream();){

				Thumbnails.of(file)
				.size(200, 200)
				.toOutputStream(os);

				final byte[] byteArray = os.toByteArray();

				ThreadRunner.runThread(
						()->{
							final File parentFile = thFile.getParentFile();
							if(!parentFile.exists())
								parentFile.mkdir();

							try (FileOutputStream fOutputStream = new FileOutputStream(thFile)) {

								fOutputStream.write(byteArray);

							} catch (IOException e) {
								logger.catching(e);
							}
						});

				final ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArray);

				return ResponseEntity.ok()
							.headers(getHeader())
							.contentType(MediaType.APPLICATION_OCTET_STREAM)
							.body(new InputStreamResource(inputStream));
			}
		return null;
	}

	private boolean isImage(File file) {
		
		final String contentType = new MimetypesFileTypeMap().getContentType(file);
		logger.debug(contentType);

		if(contentType.startsWith("image"))
			return true;

		final String lowerCase = file.getName().toLowerCase();

		return lowerCase.endsWith(".png") || lowerCase.endsWith(".bmp");
	}
}
