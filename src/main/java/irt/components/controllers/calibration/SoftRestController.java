package irt.components.controllers.calibration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import irt.components.beans.irt.update.IrtPackage;
import irt.components.beans.irt.update.Soft;
import irt.components.workers.HttpRequest;
import irt.components.workers.ProfileWorker;

@RestController
@RequestMapping("calibration/rest/soft")
public class SoftRestController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.profile.path}")
	private String profileFolder;

	@Value("${irt.flash.file}")
	private File flashFile;

	@PostMapping("upload")
    String uploadSoft(@RequestParam String sn, @RequestParam(required = false) String moduleSn) throws IOException {

    	final Object tnp = getSoft(sn, moduleSn);

    	if(tnp instanceof String)
    		return (String) tnp;

    	final Soft soft = (Soft) tnp;
		HttpRequest.upload(sn, soft);

		return "Wait for the software to load.";
	}

    @PostMapping("select/upload")
    String selectUpload(@RequestParam String sn, @RequestParam(required = false) String moduleSn, @RequestParam MultipartFile file) throws IOException {
		logger.traceEntry("sn: {}; moduleSn: {};", sn, moduleSn);

		final String fileName = file.getName();
		if(fileName.endsWith(".pkg")) {

			final IrtPackage toUpload = new IrtPackage(file);
			HttpRequest.upload(sn, toUpload);
			return "Wait for the software to load.";
		}
    	final String serialNumber = Optional.ofNullable(moduleSn).orElse(sn);
		final ProfileWorker profileWorker = new ProfileWorker(profileFolder, serialNumber);
		if(!profileWorker.exists())
			return serialNumber + " profile does not exist.";

		final Properties properties = new Properties();
		properties.load(new FileInputStream(flashFile));

		final String typeRev = profileWorker.getProperties("device-type", "device-revision").entrySet().stream().map(es->es.getValue()).collect(Collectors.joining("."));

		final Soft soft = new Soft(typeRev, file.getName(), file.getBytes());
		soft.setModule(Optional.ofNullable(moduleSn).map(m->true).orElse(null));	// Three states: null = System; true = Module; false = Module of Module;

		HttpRequest.upload(sn, soft);

		return "Wait for the software to load.";
	}

    @GetMapping("package")
    ResponseEntity<ByteArrayResource> getSoftPackage(@RequestParam String sn, @RequestParam(required = false) String moduleSn) throws IOException {

    	final Object soft = getSoft(sn, moduleSn);

    	if(soft instanceof String)
    		throw new RuntimeException((String) soft);

	    ByteArrayResource resource = new ByteArrayResource(((Soft)soft).toBytes());

	    return ResponseEntity.ok()
	            .contentType(MediaType.APPLICATION_OCTET_STREAM)
	            .contentLength(resource.contentLength())
	            .header(HttpHeaders.CONTENT_DISPOSITION,
	                    ContentDisposition.attachment()
	                        .filename(sn + ".pkg")
	                        .build().toString())
	            .body(resource);
	}

    @GetMapping("path")
    String profilePath(@RequestParam String sn) throws IOException {
		logger.traceEntry("sn: {};", sn);

    	final ProfileWorker profileWorker = new ProfileWorker(profileFolder, sn);

		if(!profileWorker.exists())
			return "IrtProfile does not exists.";

		final Properties properties = new Properties();
		properties.load(new FileInputStream(flashFile));

		final String typeRev = profileWorker.getProperties("device-type", "device-revision").entrySet().stream().map(es->es.getValue()).collect(Collectors.joining(".")) + ".path";

		return (String) properties.get(typeRev);
	}

	public Object getSoft(String sn, String moduleSn) throws IOException, FileNotFoundException {
		logger.traceEntry("sn: {}; moduleSn: {}", sn, moduleSn);

    	final String serialNumber = Optional.ofNullable(moduleSn).orElse(sn);
		final ProfileWorker profileWorker = new ProfileWorker(profileFolder, serialNumber);
		if(!profileWorker.exists())
			return serialNumber + " profile does not exist.";

		final Properties properties = new Properties();
		properties.load(new FileInputStream(flashFile));

		final String tr = profileWorker.getProperties("device-type", "device-revision").entrySet().stream().map(es->es.getValue()).collect(Collectors.joining("."));
		final String typeRev = tr + ".path";
		final String softPath = (String) properties.get(typeRev);
		if(softPath==null)
			return "There is no path to the software.";

		final Path path = Paths.get(softPath);
		final Soft soft = new Soft(tr, path);
    	soft.setModule(Optional.ofNullable(moduleSn).map(m->true).orElse(null));	// Three states: null = System; true = Module; false = Module of Module;
		return soft;
	}

}
