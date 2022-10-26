package irt.components.controllers.calibration;

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
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Level;
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

import irt.components.beans.irt.CalibrationInfo;
import irt.components.beans.irt.Info;
import irt.components.beans.irt.MonitorInfo;
import irt.components.beans.irt.calibration.CalibrationMode;
import irt.components.beans.irt.calibration.HPBMRegister;
import irt.components.beans.irt.calibration.ProfileTableTypes;
import irt.components.beans.irt.update.Profile;
import irt.components.beans.irt.update.Table;
import irt.components.beans.jpa.calibration.CalibrationBtrSetting;
import irt.components.beans.jpa.calibration.CalibrationGainSettings;
import irt.components.beans.jpa.calibration.CalibrationOutputPowerSettings;
import irt.components.beans.jpa.calibration.CalibrationPowerOffsetSettings;
import irt.components.beans.jpa.repository.calibration.CalibrationBtrSettingRepository;
import irt.components.beans.jpa.repository.calibration.CalibrationGainSettingRepository;
import irt.components.beans.jpa.repository.calibration.CalibrationOutputPowerSettingRepository;
import irt.components.beans.jpa.repository.calibration.CalibrationPowerOffsetSettingRepository;
import irt.components.workers.HttpRequest;
import irt.components.workers.ProfileWorker;

@RestController
@RequestMapping("/calibration/rest")
public class CalibrationRestController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.profile.path}")
	private String profileFolder;

	@Autowired private CalibrationOutputPowerSettingRepository calibrationOutputPowerSettingRepository;
	@Autowired private CalibrationPowerOffsetSettingRepository calibrationPowerOffsetSettingRepository;
	@Autowired private CalibrationGainSettingRepository calibrationGainSettingRepository;
	@Autowired private CalibrationBtrSettingRepository calibrationBtrSettingRepository;

	@PostMapping(path="monitorInfo", produces = "application/json;charset=utf-8")
    MonitorInfo monitorInfo(@RequestParam(required = false) String sn) {
//    	logger.error(sn);
    	return Optional.ofNullable(sn)

    			.map(
    					s->{
    						try {

    							return CalibrationController.getHttpUpdate(s, MonitorInfo.class, new BasicNameValuePair("exec", "mon_info")).get(5, TimeUnit.SECONDS);

    						} catch (MalformedURLException | InterruptedException | ExecutionException e) {
    							logger.catching(new Throwable(sn, e));
    						} catch (TimeoutException e) {
    							logger.catching(Level.DEBUG, new Throwable(sn, e));
							}

    						return null;
    					})
    			.orElse(null);
    }

	@PostMapping(path="calibrationInfo", produces = "application/json;charset=utf-8")
    CalibrationInfo calibrationInfo(@RequestParam(required = false) String sn) {
//    	logger.error(sn);
    	return Optional.ofNullable(sn)

    			.map(
    					s->{
    						try {

    							return CalibrationController.getHttpUpdate(s, CalibrationInfo.class, new BasicNameValuePair("exec", "calib_ro_info")).get(10, TimeUnit.SECONDS);

    						} catch (MalformedURLException | InterruptedException | ExecutionException e) {
    							logger.catching(new Throwable(sn, e));
    						} catch (TimeoutException e) {
    							logger.catching(Level.DEBUG, new Throwable(sn, e));
							}

    						return null;
    					})
    			.orElse(null);
    }

	@PostMapping(path="deviceDebug", produces = "application/json;charset=utf-8")
    Object deviceDebug(@RequestParam String sn, @RequestParam String devid, @RequestParam String command, @RequestParam String groupindex, @RequestParam String className) throws MalformedURLException, ClassNotFoundException, InterruptedException, ExecutionException, TimeoutException {
//    	logger.error(sn);

			try {
				return CalibrationController.getHttpDeviceDebug(

						sn,
						Class.forName(className),
						new BasicNameValuePair("devid", devid),
						new BasicNameValuePair("command", command),
						new BasicNameValuePair("groupindex", groupindex))

						.get(10, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				logger.catching(Level.DEBUG, e);
			}
			return null;
    }

    @PostMapping(path="outputpower", consumes = MediaType.APPLICATION_JSON_VALUE)
    String saveCalibrationOutputPowerSettings(@RequestBody CalibrationOutputPowerSettings settings) {

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

    @PostMapping(path="power_offset", consumes = MediaType.APPLICATION_JSON_VALUE)
    String saveCalibrationPowerOffsetSettings(@RequestBody CalibrationPowerOffsetSettings settings) {

    	return calibrationPowerOffsetSettingRepository.findById(settings.getPartNumber())
    			.map(
    					ops->{

    						ops.setStartValue(settings.getStartValue());
    						ops.setStopValue(settings.getStopValue());
 
    						calibrationPowerOffsetSettingRepository.save(ops);

    						return "The setings has been updated.";
    					})
    			.orElseGet(
    					()->{

    						calibrationPowerOffsetSettingRepository.save(settings);

    						return "The setings has been saved.";
    					});
    }

    @PostMapping("gain")
    String saveGainSettings(@RequestParam String partNumber, int startValue, int stopValue) {

    	return calibrationGainSettingRepository.findById(partNumber)
    			.map(
    					ops->{

    						ops.setStartValue(startValue);
    						ops.setStopValue(stopValue);
    						calibrationGainSettingRepository.save(ops);

    						return "The setings has been updated.";
    					})
    			.orElseGet(
    					()->{

    						CalibrationGainSettings settings = new CalibrationGainSettings();
    						settings.setPartNumber(partNumber);
    						settings.setStartValue(startValue);
    						settings.setStopValue(stopValue);
							calibrationGainSettingRepository.save(settings);

    						return "The setings has been saved.";
    					});
    }

    @PostMapping(path="to_profile", consumes = MediaType.APPLICATION_JSON_VALUE)
    String saveToProfile(@RequestBody Table table) throws IOException {
//    	logger.error(table);

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
		}catch(UnknownHostException e) {
			logger.catching(Level.DEBUG, e);
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

    @PostMapping("calibration_mode")
    CalibrationMode getCalibrationMode(@RequestParam String ip) throws InterruptedException, ExecutionException, TimeoutException, MalformedURLException {

		final URL url = new URL("http", ip, "/device_debug_read.cgi");
		List<NameValuePair> params = new ArrayList<>();
		params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("devid", "1"), new BasicNameValuePair("command", "hwinfo"), new BasicNameValuePair("groupindex", "4")}));

		return HttpRequest.postForIrtObgect(url.toString(), CalibrationMode.class, params).get(5, TimeUnit.SECONDS);
    }

    @PostMapping("calibration_mode_toggle")
    void setCalibrationMode(@RequestParam String ip) throws InterruptedException, ExecutionException, TimeoutException, MalformedURLException {

		final URL url = new URL("http", ip, "/calibration.cgi");
		List<NameValuePair> params = new ArrayList<>();
		params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("en_toggle", "1")}));

		HttpRequest.postForIrtObgect(url.toString(), Object.class, params).get(5, TimeUnit.SECONDS);
    }

    @PostMapping("dac")
    void setDac(@RequestParam String sn, int value, String channel) throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException {

		final URL url = new URL("http", sn, "/calibration.cgi");
		List<NameValuePair> params = new ArrayList<>();
		params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("channel", channel), new BasicNameValuePair("index", "2"), new BasicNameValuePair("value", Integer.toString(value))}));

		HttpRequest.postForIrtObgect(url.toString(), Object.class, params).get(5, TimeUnit.SECONDS);
    }

    @PostMapping("btr/setting")
    void saveBtrSetting(@RequestBody CalibrationBtrSetting btrSetting) {
    	logger.traceEntry("{}", btrSetting);

    	final Optional<CalibrationBtrSetting> oBtrSetting = calibrationBtrSettingRepository.findById(btrSetting.getPartNumber());
    	CalibrationBtrSetting calibrationBtrSetting;

    	if(oBtrSetting.isPresent())
    		calibrationBtrSetting = oBtrSetting.get();
 
    	else {
    		calibrationBtrSetting = new CalibrationBtrSetting();
    		calibrationBtrSetting.setPartNumber(btrSetting.getPartNumber());
    	}

    	calibrationBtrSetting.set(btrSetting);
		calibrationBtrSettingRepository.save(calibrationBtrSetting);
    }

    @PostMapping("hpbm_register")
    SimpleEntry<String, HPBMRegister> hpbmRegister(@RequestParam String sn, @RequestParam String devid) throws MalformedURLException, InterruptedException, ExecutionException{

		final URL url = new URL("http", sn, "/device_debug_read.cgi");
		List<NameValuePair> params = new ArrayList<>();
		final BasicNameValuePair deviceId = new BasicNameValuePair("devid", devid);

		params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("command", "regs"), deviceId, new BasicNameValuePair("groupindex", "20")}));

		final FutureTask<HPBMRegister> postForIrtObgect1 = HttpRequest.postForIrtYaml(url.toString(), HPBMRegister.class, params);

		SimpleEntry<String, HPBMRegister> entry = null;
		try {

			HPBMRegister object1 = postForIrtObgect1.get(2, TimeUnit.SECONDS);
			entry = new AbstractMap.SimpleEntry<>(devid, object1);

		} catch (TimeoutException e) {
			logger.catching(Level.DEBUG, e);
		}

//		logger.error(entry);

		return entry;
    }
}
