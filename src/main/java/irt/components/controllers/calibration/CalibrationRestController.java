package irt.components.controllers.calibration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.script.ScriptException;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import irt.components.beans.irt.AlarmInfo;
import irt.components.beans.irt.CalibrationInfo;
import irt.components.beans.irt.CalibrationRwInfo;
import irt.components.beans.irt.HWInfo;
import irt.components.beans.irt.HomePageInfo;
import irt.components.beans.irt.Info;
import irt.components.beans.irt.MonitorInfo;
import irt.components.beans.irt.calibration.CalibrationMode;
import irt.components.beans.irt.calibration.Diagnostics;
import irt.components.beans.irt.calibration.HPBMRegisterV21;
import irt.components.beans.irt.calibration.HPBMRegisterV31;
import irt.components.beans.irt.calibration.ProfileTableTypes;
import irt.components.beans.irt.calibration.RegisterEmpty;
import irt.components.beans.irt.calibration.RegisterGates;
import irt.components.beans.irt.calibration.RegisterPLL;
import irt.components.beans.irt.calibration.RegisterPm2Fpga;
import irt.components.beans.irt.update.Profile;
import irt.components.beans.irt.update.Soft;
import irt.components.beans.irt.update.Table;
import irt.components.beans.jpa.IrtArray;
import irt.components.beans.jpa.IrtArrayId;
import irt.components.beans.jpa.btr.BtrSetting;
import irt.components.beans.jpa.calibration.CalibrationGainSettings;
import irt.components.beans.jpa.calibration.CalibrationOutputPowerSettings;
import irt.components.beans.jpa.calibration.CalibrationPowerOffsetSettings;
import irt.components.beans.jpa.repository.IrtArrayRepository;
import irt.components.beans.jpa.repository.calibration.BtrSettingRepository;
import irt.components.beans.jpa.repository.calibration.CalibrationGainSettingRepository;
import irt.components.beans.jpa.repository.calibration.CalibrationOutputPowerSettingRepository;
import irt.components.beans.jpa.repository.calibration.CalibrationPowerOffsetSettingRepository;
import irt.components.workers.HtmlParsel;
import irt.components.workers.HttpRequest;
import irt.components.workers.ProfileWorker;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@RestController
@RequestMapping("calibration/rest")
public class CalibrationRestController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.profile.path}")
	private String profileFolder;

	@Value("${irt.flash.file}")
	private File flashFile;

	@Autowired private CalibrationOutputPowerSettingRepository calibrationOutputPowerSettingRepository;
	@Autowired private CalibrationPowerOffsetSettingRepository calibrationPowerOffsetSettingRepository;
	@Autowired private CalibrationGainSettingRepository calibrationGainSettingRepository;
	@Autowired private BtrSettingRepository calibrationBtrSettingRepository;
	@Autowired private IrtArrayRepository	arrayRepository;

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

	@PostMapping(path="irt_array", produces = "application/json;charset=utf-8")
	Optional<IrtArray> irtArray(@RequestParam String name, String id) {
		logger.traceEntry("name: {}; id: {};", name, id);
		IrtArrayId irtArrayId = new IrtArrayId(name, id);
		return arrayRepository.findById(irtArrayId);
	}

	@PostMapping(path="calib_rw_info", produces = "application/json;charset=utf-8")
	CalibrationRwInfo calibrationRwInfo(@RequestParam(required = false) String sn) {
//    	logger.error(sn);
    	return Optional.ofNullable(sn)

    			.map(
    					s->{
    						try {

    							return CalibrationController.getHttpUpdate(s, CalibrationRwInfo.class, new BasicNameValuePair("exec", "calib_rw_info")).get(10, TimeUnit.SECONDS);

    						} catch (MalformedURLException | InterruptedException | ExecutionException e) {
    							logger.catching(new Throwable(sn, e));
    						} catch (TimeoutException e) {
    							logger.catching(Level.DEBUG, new Throwable(sn, e));
							}

    						return null;
    					})
    			.orElse(null);
    }

	@PostMapping("calibration-cgi")
	String calibrationCgi(@RequestParam Map<String, String> map) {
    	logger.error("{}", map);
    	return "";
    }

	public static String diagnosticsReg(String sn, Integer moduleId, final Integer regIndex) throws MalformedURLException, IOException {
		final URL url = new URL("http", sn, "/device_debug_read.cgi");
		final List<NameValuePair> params = new ArrayList<>();
		params.addAll(Arrays.asList(
				new BasicNameValuePair[]{
						new BasicNameValuePair("devid", moduleId.toString()),
						new BasicNameValuePair("command", "regs"),
						new BasicNameValuePair("groupindex", regIndex.toString())}));

		return HttpRequest.postForString(url.toString(), params);
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
    						ops.setName(settings.getName());
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
    						ops.setName(settings.getName());
 
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

    @PostMapping(path="to_profile")
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
    String uploadProfile(@RequestParam String sn, @RequestParam(required = false) String module) throws IOException {

    	final ProfileWorker profileWorker = new ProfileWorker(profileFolder, Optional.ofNullable(module).orElse(sn));
		if(!profileWorker.exists()) return sn + " profile does not exist.";

		final Optional<Path> oPath = profileWorker.getOPath();

		final Path path = oPath.get();
		final Profile profile = new Profile(path);
		HttpRequest.upload(sn, profile);

		return "Wait for the profile to load.";
	}

    @PostMapping("upload-soft")
    String uploadSoft(@RequestParam String sn, @RequestParam(required = false) String module) throws IOException {
    	logger.traceEntry("sn: {}; module: {}", sn, module);

    	final ProfileWorker profileWorker = new ProfileWorker(profileFolder, Optional.ofNullable(module).orElse(sn));
		if(!profileWorker.exists()) return sn + " profile does not exist.";
		final Map<String, String> lines = profileWorker.getProperties(BiasingController.DEVICE_TYPE, BiasingController.DEVICE_REVISION);
		final String type = lines.get(BiasingController.DEVICE_TYPE);
		if(type==null)
			return "The profile does not contain the unit type.";
		final String rev = lines.get(BiasingController.DEVICE_REVISION);
		if(rev==null)
			return "The profile does not contain the unit revision.";
		final String typeRev = type + '.' + rev + ".path";
		final Properties properties = new Properties();
		properties.load(new FileInputStream(flashFile));
		final String softPath = (String) properties.get(typeRev);
		if(softPath==null)
			return "There is no path to the software.";

		final Path path = Paths.get(softPath);
		final Soft soft = new Soft(typeRev, path);

		return "Wait for the software to load.";
	}

    @GetMapping("package")
    ResponseEntity<ByteArrayResource> getPackage(@RequestParam String sn, @RequestParam(required = false) String module) throws IOException {

    	final ProfileWorker profileWorker = new ProfileWorker(profileFolder, Optional.ofNullable(module).orElse(sn));

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

	@GetMapping("profile")
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

    @GetMapping("profile_path")
    String profilePath(@RequestParam String sn) throws IOException {

    	final ProfileWorker profileWorker = new ProfileWorker(profileFolder, sn);

		if(!profileWorker.exists())
			return "Profile does not exists.";

    	return profileWorker.getOPath().get().toString();
	}

    @GetMapping("dumps")
    String dumps(@RequestParam String sn, @RequestParam String devid, @RequestParam String command, @RequestParam(required = false) String groupindex, Model model) throws IOException {

		final URL url = new URL("http", sn, "/device_debug_read.cgi");
		List<NameValuePair> params = new ArrayList<>();

		final BasicNameValuePair[] pairs = Optional.ofNullable(groupindex)

				.map(gi->new BasicNameValuePair[]{new BasicNameValuePair("devid", devid), new BasicNameValuePair("command", command), new BasicNameValuePair("groupindex", gi)})
				.orElse(new BasicNameValuePair[]{new BasicNameValuePair("devid", devid), new BasicNameValuePair("command", command)});

		params.addAll(Arrays.asList(pairs));

		return HttpRequest.postForString(url.toString(), params);
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
    Optional<HomePageInfo> getHomePageInfo(@RequestParam String ip) {
    	logger.traceEntry(ip);
    	try {
			return CalibrationController.getHomePageInfo(ip, 1000);
		} catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
			logger.catching(Level.DEBUG, e);
		}
		return Optional.empty();
    }

    @PostMapping("info")
    Info info(@RequestParam String ip)  {

		try {

	    	final Integer systemIndex = CalibrationController.getSystemIndex(ip);
			final URL url = new URL("http", ip, "/device_debug_read.cgi");
			List<NameValuePair> params = new ArrayList<>();
			params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("devid", systemIndex.toString()), new BasicNameValuePair("command", "info")}));

			return HttpRequest.postForIrtObgect(url.toString(), Info.class, params).get(1, TimeUnit.SECONDS);

		} catch (InterruptedException | ExecutionException | TimeoutException | HttpHostConnectException e) {
			logger.catching(Level.DEBUG, e);

		} catch (IOException | ScriptException e) {
			logger.catching(e);
		}
		return null;
    }

    @PostMapping("calibration_mode")
    CalibrationMode getCalibrationMode(@RequestParam String ip) throws IOException, ScriptException {

		try {

			final URL url = new URL("http", ip, "/device_debug_read.cgi");
			List<NameValuePair> params = new ArrayList<>();
	    	final Integer systemIndex = CalibrationController.getSystemIndex(ip);
			params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("devid", systemIndex.toString()), new BasicNameValuePair("command", "hwinfo"), new BasicNameValuePair("groupindex", "4")}));

			return HttpRequest.postForIrtObgect(url.toString(), CalibrationMode.class, params).get(5, TimeUnit.SECONDS);

		} catch (InterruptedException | ExecutionException | TimeoutException | UnknownHostException e) {
			logger.catching(Level.DEBUG, e);
			return null;
		}
    }

    @PostMapping("calibration_mode_toggle")
    ResponseEntity<String> setCalibrationMode(@RequestParam String ip) throws MalformedURLException {

		final URL url = new URL("http", ip, "/calibration.cgi");
		List<NameValuePair> params = new ArrayList<>();
		params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("en_toggle", "1")}));

		final FutureTask<Object> ft = HttpRequest.postForIrtObgect(url.toString(), Object.class, params);
		try {

			ft.get(5, TimeUnit.SECONDS);

			return new ResponseEntity<>(HttpStatus.OK);

		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.catching(Level.DEBUG, e);

			return new ResponseEntity<>("setCalibrationMode(@RequestParam String ip);\n" + e.getLocalizedMessage(), HttpStatus.GATEWAY_TIMEOUT);
		}
    }

    @PostMapping("btr/setting")
    BtrSetting saveBtrSetting(@RequestBody BtrSetting btrSetting) {
    	logger.traceEntry("{}", btrSetting);

    	final Optional<BtrSetting> oBtrSetting = calibrationBtrSettingRepository.findById(btrSetting.getPartNumber());
    	BtrSetting calibrationBtrSetting;

    	if(oBtrSetting.isPresent())
    		calibrationBtrSetting = oBtrSetting.get();
 
    	else {
    		calibrationBtrSetting = new BtrSetting();
    		calibrationBtrSetting.setPartNumber(btrSetting.getPartNumber());
    	}

    	calibrationBtrSetting.set(btrSetting);
		return calibrationBtrSettingRepository.save(calibrationBtrSetting);
    }

    @PostMapping("hpbm_register_v21")
    SimpleEntry<String, HPBMRegisterV31> hpbmRegisterV21(@RequestParam String sn, @RequestParam String devid) throws MalformedURLException, InterruptedException, ExecutionException{
    	logger.traceEntry("sn: {}, devid: {}", sn, devid);

		final URL url = new URL("http", sn, "/device_debug_read.cgi");
		List<NameValuePair> params = new ArrayList<>();
		final BasicNameValuePair deviceId = new BasicNameValuePair("devid", devid);

		params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("command", "regs"), deviceId, new BasicNameValuePair("groupindex", "20")}));

		final FutureTask<HPBMRegisterV21> postForIrtObgect1 = HttpRequest.postForIrtYaml(url.toString(), HPBMRegisterV21.class, params);

		SimpleEntry<String, HPBMRegisterV31> entry = new AbstractMap.SimpleEntry<>(devid, null);
		try {

			HPBMRegisterV21 v21 = postForIrtObgect1.get(10, TimeUnit.SECONDS);
			final HPBMRegisterV31 ps1 = v21.getPowerSupply1();
			final HPBMRegisterV31 ps2 = v21.getPowerSupply2();
			ps1.setSwitch3(ps2.getSwitch1());
			ps1.setSwitch4(ps2.getSwitch2());

			entry.setValue(ps1);

		} catch (TimeoutException e) {
			logger.catching(Level.DEBUG, e);
		}

		logger.debug(entry);

		return entry;
    }

    @PostMapping("hpbm_register_v31")
    SimpleEntry<String, HPBMRegisterV31> hpbmRegisterV31(@RequestParam String sn, @RequestParam String devid) throws MalformedURLException, InterruptedException, ExecutionException{
    	logger.traceEntry("sn: {}, devid: {}", sn, devid);

		final URL url = new URL("http", sn, "/device_debug_read.cgi");
		List<NameValuePair> params = new ArrayList<>();
		final BasicNameValuePair deviceId = new BasicNameValuePair("devid", devid);

		params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("command", "regs"), deviceId, new BasicNameValuePair("groupindex", "20")}));

		final FutureTask<HPBMRegisterV31> postForIrtObgect1 = HttpRequest.postForIrtObgect(url.toString(), HPBMRegisterV31.class, params);

		SimpleEntry<String, HPBMRegisterV31> entry = null;
		try {

			HPBMRegisterV31 object1 = postForIrtObgect1.get(10, TimeUnit.SECONDS);
//			logger.debug(object1);
			entry = new AbstractMap.SimpleEntry<>(devid, object1);

		} catch (TimeoutException e) {
			logger.catching(Level.DEBUG, e);
		}

		logger.debug(entry);

		return entry;
    }

    @PostMapping("alarm_info")
    Optional<AlarmInfo> alarmInfo(@RequestParam String sn) throws MalformedURLException, InterruptedException, ExecutionException{
//    	logger.error(sn);
    	return Optional.ofNullable(sn)

    			.map(
    					s->{
    						try {

    							return Arrays.stream(CalibrationController.getHttpUpdate(s, AlarmInfo[].class, new BasicNameValuePair("exec", "alarms_info")).get(5, TimeUnit.SECONDS)).filter(a->a.getDevname()!=null).findAny();

    						} catch (MalformedURLException | InterruptedException | ExecutionException e) {
    							logger.catching(new Throwable(sn, e));
    						} catch (TimeoutException e) {
    							logger.catching(Level.DEBUG, new Throwable(sn, e));
							}

    						return null;
    					})
    			.orElse(null);
    }

    @GetMapping("mute")
    String mute(@RequestParam String sn, @RequestParam(required = false) Mute mute){
    	logger.traceEntry("Serial Number: {}; mute: {};", sn, mute);

    	return Optional.ofNullable(sn)

    			.map(
    					s->{
    						try {

    							final URL url = new URL("http", sn, "/control.cgi");
    							List<NameValuePair> params = new ArrayList<>();
    							params.add(new BasicNameValuePair("mute", mute.value));

    							HttpRequest.postForString(url.toString(), params);
    							return "Done";

    						} catch (IOException e) {
    							logger.catching(e);
    							return e.getLocalizedMessage();
    						}

    					})
    			.orElse("No Setial Number.");
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

    @GetMapping("calibration-mode")
    String calibrationMode(@RequestParam String sn) throws URISyntaxException, IOException, InterruptedException, ExecutionException, TimeoutException{

    	final URL url = new URL("http", sn, "/calibration.asp");
    	final FutureTask<String> ft = HttpRequest.getForString( url.toString());
		String str = ft.get(100, TimeUnit.MILLISECONDS);
    	logger.error(str);
		final HtmlParsel htmlParsel = new HtmlParsel("script");
		final List<String> all = htmlParsel.parseAll(str);
    	logger.error(all);
    	return sn;
    }

	@PostMapping("all-modules")
	Map<String, Integer> allModules(@RequestParam String sn) throws IOException, InterruptedException, ExecutionException, TimeoutException, ScriptException {
    	logger.traceEntry("{}", sn);
		return HttpRequest.getAllModules(sn);
    }

    @PostMapping("pll_register")
    RegisterPLL pllRegister(@RequestParam String sn, String addr, String value) throws MalformedURLException, InterruptedException, ExecutionException, ScriptException{
    	logger.traceEntry(sn);
    	return Optional.ofNullable(sn)

    			.map(
    					s->{
    						try {

    							final URL url = new URL("http", sn, "/device_debug_write.cgi");
    				    		List<NameValuePair> params = new ArrayList<>();
    				    		params.addAll(Arrays.asList(
    				    				new BasicNameValuePair[]{
    				    						new BasicNameValuePair("devid", "1"),
    				    						new BasicNameValuePair("command", "regs"),
    				    						new BasicNameValuePair("group", "102"),
    				    						new BasicNameValuePair("address", addr),
    				    						new BasicNameValuePair("value", value)}));
    				    		FutureTask<RegisterPLL> o = HttpRequest.postForIrtObgect(url.toString(), RegisterPLL.class, params);
    				    		final RegisterPLL pllRegister = o.get(5, TimeUnit.SECONDS);
    				    		logger.debug(pllRegister);
								return pllRegister;

    						} catch (IOException e) {
    							logger.catching(new Throwable(sn, e));
    						} catch (InterruptedException | ExecutionException | TimeoutException e) {
    							logger.catching(Level.DEBUG, e);
							}

    						return null;
    					})
    			.orElse(null);
    }

    @PostMapping("pll_registers")
    RegisterPLL pllRegisters(@RequestParam String sn) throws MalformedURLException, InterruptedException, ExecutionException, ScriptException{
    	return diagnostics(RegisterPLL.class, sn, "regs", "1", "102", null, null, PostFor.IRT_OBJECT);
    }

	@PostMapping("module-info")
	Info info(@RequestParam String sn, Integer moduleIndex) {
		return diagnostics(Info.class, sn, "info", moduleIndex.toString(), null, null, null, PostFor.IRT_OBJECT);
    }

	@PostMapping("hw-info")
	HWInfo hwInfo(@RequestParam String sn, Integer moduleIndex) {
		final HWInfo hwInfo = diagnostics(HWInfo.class, sn, "hwinfo", moduleIndex.toString(), "4", null, null, PostFor.IRT_OBJECT);
		Optional.ofNullable(hwInfo).ifPresent(hi->hi.setModuleIndex(moduleIndex)); 
		return hwInfo;
    }

    @GetMapping("register-pm2-fpga")
    RegisterPm2Fpga getRegisterPm2Fpga(@RequestParam String sn, Integer deviceId, @RequestParam(required = false) String address, @RequestParam(required = false) String value){
		return diagnostics(RegisterPm2Fpga.class, sn, "regs", deviceId.toString(), "25", address, value, PostFor.IRT_OBJECT);
    }

    @RequestMapping("register-gates")
    RegisterGates getRegisterDacs(@RequestParam String sn, Integer deviceId, @RequestParam(required = false) String address, @RequestParam(required = false) String value){
		return diagnostics(RegisterGates.class, sn, "regs", deviceId.toString(), "27", address, value, PostFor.STRING);
    }

    @PostMapping("register/write")
    String rwRegister(@RequestParam String sn, String moduleId, String index, String address, String value){
    	diagnostics(RegisterEmpty.class, sn, "regs", moduleId, index, address, value, PostFor.IRT_OBJECT);
    	return sn;
    }

	public static  <T extends Diagnostics> T diagnostics(Class<T> registerClass, String sn, String command, String moduleIndex, String index, String address, String value, PostFor postFor) {
    	logger.traceEntry("registerClass: {}; sn: {}; command: {}; moduleIndex: {}; index: {}; address: {}; value: {}; postFor: {}",  registerClass, sn, command, moduleIndex, index, address, value, postFor);
		return Optional.ofNullable(sn)

    			.map(
    					s->{
    						try {

    				    		List<NameValuePair> params = new ArrayList<>();
    				    		params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("devid", moduleIndex), new BasicNameValuePair("command", command)}));

    				    		final URL url;
    				    		if(address==null || value == null) {
    				    			url = new URL("http", sn, "/device_debug_read.cgi");
    				    			Optional.ofNullable(index).ifPresent(i->params.add(new BasicNameValuePair("groupindex", i)));
    				    		}else {
    				    			url = new URL("http", sn, "/device_debug_write.cgi");
	    							params.add(new BasicNameValuePair("group", index));
	    							params.add(new BasicNameValuePair("address", address));
	    							params.add(new BasicNameValuePair("value", value));
    				    		}
    				    		FutureTask<T> ft = getIrtObject(url, registerClass, params, postFor);
    				    		final T t = ft.get(10, TimeUnit.SECONDS);
    				    		logger.debug(t);
								return t;

    						} catch (InterruptedException | ExecutionException | TimeoutException | HttpHostConnectException e) {
    							logger.catching(Level.DEBUG, e);
							} catch (IOException e) {
    							logger.catching(e);
    						}

    						return null;
    					})
    			.orElse(null);
	}

	private static <T extends Diagnostics> FutureTask<T> getIrtObject(final URL url, Class<T> registerClass, List<NameValuePair> params, PostFor postFor) throws IOException {
		logger.traceEntry("url: {}; registerClass: {}; params: {};postFor postFor: {}", url, registerClass, params, postFor);
		switch(postFor) {
		case IRT_OBJECT:
			return HttpRequest.postForIrtObgect(url.toString(), registerClass, params);
		case IRT_YAML:
			return HttpRequest.postForIrtYaml(url.toString(), registerClass, params);
		case STRING:
			String str = HttpRequest.postForString(url.toString(), params);
			Callable<T> callable = () ->{
				final Constructor<T> constructor = registerClass.getConstructor(String.class);
					return constructor.newInstance(str);
				};
			final FutureTask<T> ft = new FutureTask<>(callable);
			ft.run();
			return ft;
		}
		return null;
	}

    @Getter @Setter @AllArgsConstructor @ToString
    public static class Message{
    	private String content;
    }
    @RequiredArgsConstructor @Getter
    public enum Mute{
    	OFF("Off"),
    	ON("On");
    	private final String value;
    }
    enum PostFor{
    	IRT_OBJECT,
    	IRT_YAML,
    	STRING
    }
}
