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
import java.util.function.Consumer;

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
import org.springframework.web.multipart.MultipartFile;

import irt.components.controllers.eco.EcoController;
import irt.components.services.RmaService;
import irt.components.services.RmaServiceLocal;
import irt.components.services.RmaServiceWeb;
import irt.components.workers.ThreadRunner;
import net.coobird.thumbnailator.Thumbnails;

@RestController
@RequestMapping("files")
public class FileRestController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.eco.files.path}") private String ecoFilesPath;
	@Value("${irt.host}") 			private String host;

	@Autowired private SmbSessionFactory smbSessionFactory;
	@Autowired private RmaServiceLocal	local;
	@Autowired private RmaServiceWeb	web;
	

	@PostConstruct
	public void postConstruct() {
        try {

    		// get system name
        	String sName = InetAddress.getLocalHost().getHostName();

        	// Change the directory if server runs on my computer
        	if(sName.equals("oleksandr")) 
           		ecoFilesPath = EcoController.TEST_PATH_TO_ECO_FILES;

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
		final String name = new File(path).getName();

		final MediaType mediaType;
		if(name.toLowerCase().endsWith(".pdf")) {

			headers.add("Content-Disposition", "inline; filename=\"" + name + "\"");
			mediaType = MediaType.APPLICATION_PDF;

		}else {

			headers.add("Content-Disposition", "attachment; filename=\"" + name + "\"");
			mediaType = MediaType.APPLICATION_OCTET_STREAM;
		}

		return ResponseEntity.ok()
				.headers(headers)
				.contentType(mediaType)
				.body(body);
	}

	private HttpHeaders getHeader() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");
		return headers;
	}

	@GetMapping("/rma/{commentID}/{fileName}/{onWeb}")
	public ResponseEntity<Resource> rmaFile(@PathVariable  String commentID, @PathVariable String fileName, @PathVariable Boolean onWeb) throws IOException{

		final RmaService rmaService = onWeb ? web : local;
		final Path path = Paths.get(rmaService.getPathToRmaFiles(), commentID, fileName);
		final File file = path.toFile();

		HttpHeaders headers = getHeader();
		headers.add("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");

		if(!file.exists())
			return ResponseEntity.notFound()
					.headers(headers).build();

		final InputStream is = new FileInputStream(file);

		return ResponseEntity.ok()
				.headers(headers)
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(new InputStreamResource(is));
	}

	@GetMapping("/rma/thumbnails/{commentID}/{fileName}/{onWeb}")
	public ResponseEntity<Resource> rmaThumbnails(@PathVariable  String commentID, @PathVariable String fileName, @PathVariable Boolean onWeb) throws IOException{
		logger.traceEntry("commentID: {}; fileName: {}; onWeb: {}", commentID, fileName, onWeb);

		final RmaService	rmaService = onWeb ? web : local;
		return getThumbnails(rmaService.getPathToRmaFiles(), commentID, fileName);
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

		if(file.exists() && isImage(file))
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

	public static Consumer<? super MultipartFile> saveFile(String rmaFilesPath, Long commentId) {
		return mpFile->{

			if(mpFile.isEmpty())
				return;

			final Path p = Paths.get(rmaFilesPath, commentId.toString());
			p.toFile().mkdirs();	//create a directory
			String originalFilename = mpFile.getOriginalFilename();
			Path path = Paths.get(p.toString(), originalFilename);

			try {

				mpFile.transferTo(path);

			} catch (IllegalStateException | IOException e) {
				logger.catching(e);
			}
		};
	}

	public static void saveImage(File file, byte[] imageByte) throws FileNotFoundException, IOException {
		try(final FileOutputStream os = new FileOutputStream(file);){
			os.write(imageByte);
		}
	}
}
