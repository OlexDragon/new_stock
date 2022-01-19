package irt.components.calibration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import irt.components.beans.calibration.ProfileTableTypes;
import irt.components.beans.calibration.update.Profile;
import irt.components.beans.calibration.update.Table;
import irt.components.jpa.beans.CalibrationOutputPowerSettings;
import irt.components.jpa.repository.CalibrationOutputPowerSettingRepository;
import irt.components.values.units.CalibrationInfo;
import irt.components.values.units.Info;
import irt.components.workers.HttpRequest;
import irt.components.workers.ProfileWorker;

@RestController
@RequestMapping("/calibration/rest")
public class CalibrationRestController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.profile.path}")
	private String profileFolder;

	@Autowired CalibrationOutputPowerSettingRepository calibrationOutputPowerSettingRepository;

	@PostMapping(path="calibrationInfo", produces = "application/json;charset=utf-8")
    CalibrationInfo calibrationInfo(@RequestParam(required = false) String sn) {
//    	logger.error(sn);
    	return Optional.ofNullable(sn)

    			.map(
    					s->{
    						try {

    							return CalibrationController.getHttpUpdate(s, CalibrationInfo.class, new BasicNameValuePair("exec", "calib_ro_info")).get(10, TimeUnit.SECONDS);

    						} catch (MalformedURLException | InterruptedException | ExecutionException | TimeoutException e) {
    							logger.catching(e);
    						}

    						return null;
    					})
    			.orElse(null);
    }

    @PostMapping(path="outputpower", consumes = MediaType.APPLICATION_JSON_VALUE)
    String calibrationOutputPowerSettings(@RequestBody CalibrationOutputPowerSettings settings) {

    	return calibrationOutputPowerSettingRepository.findById(settings.getPartNumber())
    			.map(
    					ops->{

    						ops.setStartValue(settings.getStartValue());
    						ops.setStopValue(settings.getStopValue());
    						calibrationOutputPowerSettingRepository.save(ops);

    						return "The setings has been updated.";
    					})
    			.orElseGet(
    					()->{

    						calibrationOutputPowerSettingRepository.save(settings);

    						return "The setings has been saved.";
    					});
    }

    @PostMapping(path="to_profile", consumes = MediaType.APPLICATION_JSON_VALUE)
    String saveToProfile(@RequestBody Table table) throws IOException {

    	ProfileWorker profileWorker = new ProfileWorker(profileFolder, table.getSerialNumber());

    	if(!profileWorker.exists())
    		return "The profile does not exist.";

    	return profileWorker.scanForTable(table.getName()).filter(pt->pt.getType()!=ProfileTableTypes.UNKNOWN)

    			.map(pt->profileWorker.saveToProfile(pt, table.getValues()) ? "The table has been saved." : "Something went wrong. The table has not been saved.").orElse("The table was not found.");

	}

    @PostMapping("upload")
    String uploadProfile(@RequestParam String sn) throws IOException {

    	final ProfileWorker profileWorker = new ProfileWorker(profileFolder, sn);

		if(!profileWorker.exists())
			return sn + " profile does not exist.";

		final Optional<Path> oPath = profileWorker.getOPath();

		final Path path = oPath.get();
		final Profile profile = new Profile(path);
		HttpRequest.upload(sn, profile);

		return "Wait for the profile to load.";
	}

    @PostMapping("login")
    String login(@RequestParam String sn) throws IOException {

    	final URL url = new URL("http", sn, "/hidden.cgi");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();	
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);

		try(	OutputStream outputStream = connection.getOutputStream();
				OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);){

			outputStreamWriter.write("pwd=jopa");
			outputStreamWriter.flush();

			try(	final InputStream inputStream = connection.getInputStream();
					final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
					final BufferedReader reader = new BufferedReader(inputStreamReader);){

				String line;
				while ((line = reader.readLine()) != null)
					logger.debug(line);
				
			}
		}

		connection.disconnect();
    	return "Authorized on  " + sn;
    }

    @PostMapping("scan")
    String scanIP(@RequestParam String ip) throws UnknownHostException, IOException {
    	final InetAddress byName = InetAddress.getByName(ip);
		return Optional.of(byName.isReachable(1000)).filter(b->b).map(b->byName.getCanonicalHostName()).orElse("");
    }

    @PostMapping("info")
    Info info(@RequestParam String ip) throws InterruptedException, ExecutionException, TimeoutException, MalformedURLException {

		final URL url = new URL("http", ip, "/device_debug_read.cgi");
		List<NameValuePair> params = new ArrayList<>();
		params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("devid", "1"), new BasicNameValuePair("command", "info")}));

		return HttpRequest.postForIrtObgect(url.toString(), Info.class, params).get(5, TimeUnit.SECONDS);
    }
}
