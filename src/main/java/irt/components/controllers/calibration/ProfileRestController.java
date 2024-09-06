package irt.components.controllers.calibration;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
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

import irt.components.beans.irt.calibration.ProfileTableTypes;
import irt.components.beans.irt.update.Profile;
import irt.components.beans.irt.update.Table;
import irt.components.controllers.calibration.CalibrationRestController.Message;
import irt.components.workers.HtmlParsel;
import irt.components.workers.HttpRequest;
import irt.components.workers.ProfileWorker;

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
        	final FutureTask<String> ft = HttpRequest.getForString(builder.build().toString());
			String str = ft.get(5, TimeUnit.SECONDS);
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
    String profilePath(@RequestParam String sn) throws IOException {

    	final ProfileWorker profileWorker = new ProfileWorker(profileFolder, sn);

		if(!profileWorker.exists())
			return "Profile does not exists.";

    	return profileWorker.getOPath().get().toString();
	}

    @GetMapping("profile/by-property")
    String profileByProperty(@RequestParam String sn, String property) throws IOException{
    	logger.traceEntry("Serial Number: {}; property: {}", sn, property);

    	final ProfileWorker profileWorker = new ProfileWorker(profileFolder, sn);

		if(!profileWorker.exists())
			return sn + " profile does not exist.";

		final Map<String, String> linesStartsWith = profileWorker.getLinesStartsWith(property);
		return linesStartsWith.get(property);
    }

    @PostMapping("save")
    Message saveToProfile(@RequestBody Table table) throws IOException {
//    	logger.error(table);

    	ProfileWorker profileWorker = new ProfileWorker(profileFolder, table.getSerialNumber());

    	if(!profileWorker.exists())
    		return new Message("The profile does not exist.");

    	final String content = profileWorker.scanForTable(table.getName()).filter(pt->pt.getType()!=ProfileTableTypes.UNKNOWN)
    			.map(pt->profileWorker.saveToProfile(pt, table.getValues()) ? "The table has been saved." : "Something went wrong. The table has not been saved.")
    			.orElse("The table was not found.");

    	return new Message(content);

	}

    @PostMapping("upload")
    String uploadProfile(@RequestParam String sn, @RequestParam(required = false) String moduleSn) throws IOException {

    	final ProfileWorker profileWorker = new ProfileWorker(profileFolder, Optional.ofNullable(moduleSn).orElse(sn));
		if(!profileWorker.exists()) return sn + " profile does not exist.";

		final Optional<Path> oPath = profileWorker.getOPath();

		final Path path = oPath.get();
		final Profile profile = new Profile(path);
		profile.setModule(moduleSn!=null);
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
		final Profile profile = new Profile(path);
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
}
