package irt.components.controllers.calibration;

import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import irt.components.beans.IrtMessage;
import irt.components.beans.irt.calibration.ProfileTableTypes;
import irt.components.beans.irt.update.IrtPackageInPackage;
import irt.components.beans.irt.update.IrtProfile;
import irt.components.beans.irt.update.Table;
import irt.components.workers.HtmlParsel;
import irt.components.workers.HttpRequest;
import irt.components.workers.ProfileWorker;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("calibration/rest/profile")
public class ProfileRestController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.profile.path}")
	private String profileFolder;

	@GetMapping
    String profile(@RequestParam String sn, @RequestParam(required = false) Integer moduleId) throws IOException, URISyntaxException, InterruptedException, ExecutionException, TimeoutException {
    	logger.traceEntry("{}; {};", sn, moduleId);

    	final URIBuilder builder;

    	if(moduleId==null) {

    		final URL url = new URL("http", sn, "/diagnostics.asp");
        	builder = new URIBuilder(url.toString()).setParameter("profile", "1");
			String str = HttpRequest.getForString(builder.build().toString(), 10, TimeUnit.SECONDS);
        	logger.debug(str);
        	try(final StringReader reader = new StringReader(str);){
 
        		final HtmlParsel htmlParsel = new HtmlParsel("textarea");
        		return Optional.ofNullable(htmlParsel.parseFirst(str)).map(s->s.substring(s.indexOf('>') + 1).trim()).orElse(str);
        	}

    	}else {

    		final URL url = new URL("http", sn, "/device_debug_read.cgi");
    		List<NameValuePair> params = new ArrayList<>();
    		params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("devid", moduleId.toString()), new BasicNameValuePair("command", "profile")}));
       		return HttpRequest.postForString(url.toString(), params);
    	}
	}

    @GetMapping("path")
    Map<String, Object> profilePath(@RequestParam String sn, HttpServletRequest request) throws IOException {

    	final Map<String, Object> response = new HashMap<>();
    	final String remoteAddr = "http://" + request.getRemoteAddr() + ":8088";
    	final FutureTask<Boolean> forString = HttpRequest.getForObgect(remoteAddr + "/ping", Boolean.class);


    	final ProfileWorker profileWorker = new ProfileWorker(profileFolder, sn);

		if(!profileWorker.exists()) {
			response.put("message", "IrtProfile does not exists.");
			return response;
		}

		try {

			response.put("remoteAddr", remoteAddr);
			response.put("serial-exists", forString.get(1, TimeUnit.MICROSECONDS));

		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.catching(Level.DEBUG, e);
		}

		response.put("message", profileWorker.getOPath().get().toString());

		return response;
	}

    @GetMapping("dir")
    Map<String, Object> profileDir(@RequestParam String sn, HttpServletRequest request) throws IOException {

    	final Map<String, Object> response = new HashMap<>();
    	final String remoteAddr = "http://" + request.getRemoteAddr() + ":8088";
    	final FutureTask<Boolean> forString = HttpRequest.getForObgect(remoteAddr + "/ping", Boolean.class);

    	final ProfileWorker profileWorker = new ProfileWorker(profileFolder, sn);

		if(!profileWorker.exists()) {
			response.put("message", "The directory does not exists.");
			return response;
		}

		try {

			response.put("remoteAddr", remoteAddr);
			response.put("serial-exists", forString.get(1, TimeUnit.MICROSECONDS));

		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.catching(Level.DEBUG, e);
		}

		response.put("message", profileWorker.getOPath().get().getParent().toString());

    	return response;
	}

    @GetMapping("by-property")
    String profileByProperty(@RequestParam String sn, String property) throws IOException{
    	logger.traceEntry("Serial Number: {}; property: {}", sn, property);

    	final ProfileWorker profileWorker = new ProfileWorker(profileFolder, sn);

		if(!profileWorker.exists())
			return sn + " profile does not exist.";

		final Map<String, String> linesStartsWith = profileWorker.getLinesStartsWith(property);
		return linesStartsWith.get(property);
    }

    @PostMapping("save")
    IrtMessage saveToProfile(@RequestBody Table table) throws IOException {
    	logger.traceEntry("{}", table);

    	ProfileWorker profileWorker = new ProfileWorker(profileFolder, table.getSerialNumber()); 

    	if(!profileWorker.exists())
    		return new IrtMessage("The profile does not exist.");

    	final String content = profileWorker.scanForTable(table.getName()).filter(pt->pt.getType()!=ProfileTableTypes.UNKNOWN)
    			.map(pt->profileWorker.saveToProfile(pt, table.getValues()) ? "The table has been saved." : "Something went wrong. The table has not been saved.")
    			.orElse("The table was not found.");

		return new IrtMessage(content);
	}

    @PostMapping("save/property")
    ProfileChangeMessage saveProperty(@RequestParam String sn, @RequestParam String property, @RequestParam String value) throws IOException {
    	logger.traceEntry("sn: {}; property: {}; value: {};", sn, property, value);

    	ProfileWorker profileWorker = new ProfileWorker(profileFolder, sn); 

    	if(!profileWorker.exists())
    		return new ProfileChangeMessage("The profile for " + sn + " does not exist.", false);

    	if(profileWorker.saveProperty(property, value))
    		return new ProfileChangeMessage("The propery '" + property +" " + value + "' has been saved.", true);

    	return new ProfileChangeMessage("Something went wrong.", false);
	}

    @PostMapping("upload")
    String uploadProfile(@RequestParam String sn, @RequestParam(required = false) String moduleSn) throws IOException {
    	logger.traceEntry("sn: {}; moduleSn: {}", sn, moduleSn);

    	final ProfileWorker profileWorker = new ProfileWorker(profileFolder, Optional.ofNullable(moduleSn).orElse(sn));
		if(!profileWorker.exists()) return sn + " profile does not exist.";

		final Optional<Path> oPath = profileWorker.getOPath();

		final Path path = oPath.get();
		final IrtProfile profile = new IrtProfile(path);
		profile.setModule(moduleSn!=null && !moduleSn.equals(sn));
		HttpRequest.upload(sn, profile);

		return "Wait for the profile to load.";
	}

    @GetMapping("package")
    ResponseEntity<ByteArrayResource> getPackage(@RequestParam String sn, @RequestParam(required = false) String moduleSn) throws IOException {

    	final ProfileWorker profileWorker = new ProfileWorker(profileFolder, Optional.ofNullable(moduleSn).orElse(sn));

		if(!profileWorker.exists())
			return null;

		final Optional<Path> oPath = profileWorker.getOPath();

		final Path path = oPath.get();
		final IrtProfile profile = new IrtProfile(path);
		profile.setModule(moduleSn!=null && !moduleSn.equals(sn));
	    ByteArrayResource resource = new ByteArrayResource(profile.toBytes());

	    return ResponseEntity.ok()
	            .contentType(MediaType.APPLICATION_OCTET_STREAM)
	            .contentLength(resource.contentLength())
	            .header(HttpHeaders.CONTENT_DISPOSITION,
	                    ContentDisposition.attachment()
	                        .filename(sn + ".pkg")
	                        .build().toString())
	            .body(resource);
	}

    @GetMapping(path = "download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    FileSystemResource downloadProfile(@RequestParam String sn, HttpServletResponse response) throws IOException {

    	final ProfileWorker profileWorker = new ProfileWorker(profileFolder, sn);

		if(!profileWorker.exists())
			return null;

		final Path path = profileWorker.getOPath().get();
		response.setHeader("Content-Disposition", "attachment; filename=" + path.getFileName());

    	return new FileSystemResource(path);
	}

    @RequiredArgsConstructor @Getter
    public class ProfileChangeMessage{
    	private final String message;
    	private final boolean changeDon;
    }

    @PostMapping("package-package")
    String packageInPackage(@RequestParam String sn, @RequestParam(required = false) String moduleSn, @RequestParam MultipartFile file) throws IOException {
		logger.traceEntry("sn: {}; moduleSn: {};", sn, moduleSn);
		

		final String toUpdate = Optional.ofNullable(moduleSn).orElse(sn);
		final String fileName = file.getOriginalFilename();
		if(!fileName.endsWith(".pkg"))
			return "Only files with extension '.pkg' are accepted";

		final IrtPackageInPackage p = new IrtPackageInPackage(toUpdate, file);

//		try (FileOutputStream fos = new FileOutputStream("C:\\Users\\Alex\\Desktop\\package.pkg")) {
//			   fos.write(p.toBytes());
//		}
		HttpRequest.upload(sn, p);

		return "Wait for the package to load.";
	}
}
