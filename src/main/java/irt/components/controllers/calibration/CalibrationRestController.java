package irt.components.controllers.calibration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import irt.components.beans.CurrentOffset;
import irt.components.beans.irt.AlarmInfo;
import irt.components.beans.irt.CalibrationInfo;
import irt.components.beans.irt.Info;
import irt.components.beans.irt.MonitorInfo;
import irt.components.beans.irt.calibration.CalibrationMode;
import irt.components.beans.irt.calibration.HPBMRegister;
import irt.components.beans.irt.calibration.PLLRegister;
import irt.components.beans.irt.calibration.ProfileTableTypes;
import irt.components.beans.irt.update.Profile;
import irt.components.beans.irt.update.Table;
import irt.components.beans.jpa.btr.BtrSetting;
import irt.components.beans.jpa.calibration.CalibrationGainSettings;
import irt.components.beans.jpa.calibration.CalibrationOutputPowerSettings;
import irt.components.beans.jpa.calibration.CalibrationPowerOffsetSettings;
import irt.components.beans.jpa.repository.calibration.BtrSettingRepository;
import irt.components.beans.jpa.repository.calibration.CalibrationGainSettingRepository;
import irt.components.beans.jpa.repository.calibration.CalibrationOutputPowerSettingRepository;
import irt.components.beans.jpa.repository.calibration.CalibrationPowerOffsetSettingRepository;
import irt.components.workers.HtmlParsel;
import irt.components.workers.HttpRequest;
import irt.components.workers.ProfileWorker;
import irt.components.workers.ThreadRunner;
import javafx.util.Pair;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@RestController
@RequestMapping("/calibration/rest")
public class CalibrationRestController {
	private final static Logger logger = LogManager.getLogger();
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	@Value("${irt.profile.path}")
	private String profileFolder;

	@Value("${irt.perl.path}")
	private String perlPath;

	@Value("${irt.perl.script.path}")
	private String perlScriptPath;

	@Value("${irt.perl.script.argument}")
	private String perlScriptArgument;

	@Autowired private CalibrationOutputPowerSettingRepository calibrationOutputPowerSettingRepository;
	@Autowired private CalibrationPowerOffsetSettingRepository calibrationPowerOffsetSettingRepository;
	@Autowired private CalibrationGainSettingRepository calibrationGainSettingRepository;
	@Autowired private BtrSettingRepository calibrationBtrSettingRepository;

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

		if(!profileWorker.exists())
			return sn + " profile does not exist.";

		final Optional<Path> oPath = profileWorker.getOPath();

		final Path path = oPath.get();
		final Profile profile = new Profile(path);
		HttpRequest.upload(sn, profile);

		return "Wait for the profile to load.";
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

    @PostMapping("current_offset")
    List<CurrentOffset> currentOffset(@RequestParam String sn, @RequestParam Boolean local) throws UnknownHostException {

    	final InetAddress byName = InetAddress.getByName(sn);
    	final byte[] bytes = byName.getAddress();
    	final int length = bytes.length;
    	final List<CurrentOffset> offsets = new ArrayList<>();

    	if(length!=4) {
    		final CurrentOffset co = new CurrentOffset("ERROR");
    		co.getOffsets().add("Unable to get an IP address.");
			offsets.add(co);
    		return offsets;
    	}

    	final String ipAddress = IntStream.range(0, length).map(index->bytes[index]&0xff).mapToObj(Integer::toString).collect(Collectors.joining("."));

    	 try {

    		 List<String> commands = new ArrayList<>();
    		 commands.add(perlPath);
    		 commands.add(perlScriptPath);
    		 commands.add(perlScriptArgument + ipAddress);
    		 if(local)
    			 commands.add("--local=1");

    		 final ProcessBuilder builder = new ProcessBuilder(commands);
    		 builder.redirectErrorStream(true);

    		 final Process process = builder.start();

    		 final FutureTask<Void> ft = new FutureTask<>(()->null);

    		 ThreadRunner.runThread(
    				 ()->{
    					 final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    			            String line;
    			            try {

    			            	CurrentOffset offset = null;
    			            	while ((line = reader.readLine()) != null) {
    			            		logger.debug(line);
    			            		if(!ft.isDone())
    			            			ThreadRunner.runThread(ft);

    			            		if(line.startsWith("[psu_offset.pl->psu_offset.pl")) {
    			            			if(line.endsWith(".bin:")) {
    			            				final CurrentOffset co = Optional.of(line.split("\\s+")).filter(arr->arr.length==2).map(arr->arr[1].substring(0, arr[1].length()-1)).map(CurrentOffset::new).orElse(null);
    			            				if(co!=null) {
    			            					offset = co;
    			            					offsets.add(co);
    			            				}
    			            			}
    			            		}else if(line.startsWith("pm-vmon-offset")) {
    			            			final String l = line;
    			            			Optional.ofNullable(offset).ifPresent(os->os.getOffsets().add(l));
    			            		}
								}

    			            } catch (IOException e) {
								logger.catching(e);
							}
    				 });

    		 // Wait for script to start
    		 try {

    			 ft.get(5, TimeUnit.SECONDS);

    		 } catch (ExecutionException | TimeoutException e) {
				logger.catching(Level.DEBUG, e);
			}


    		 try(final OutputStream os = process.getOutputStream();){
    			 while(process.isAlive()) {
    				 os.write(("\n").getBytes());
    				 os.flush();
    	    		 Thread.sleep(3000);
    			 }
    		 }

    		 return offsets.stream().filter(os->!os.getOffsets().isEmpty()).collect(Collectors.toList());

    	 } catch (IOException | InterruptedException e) {
			logger.catching(e);
			final CurrentOffset co = new CurrentOffset("ERROR");
			offsets.add(co);
			final String localizedMessage = e.getLocalizedMessage();

			if(localizedMessage.isEmpty()) {
	    		co.getOffsets().add(e.getClass().getSimpleName());
	    		return offsets;
			}else {
	    		co.getOffsets().add(localizedMessage);
	    		return offsets;
			}
		}
	}

    @PostMapping(path = "save_current_offset", consumes = MediaType.APPLICATION_JSON_VALUE)
    Pair<String, String> saveCurrentOffset(@RequestBody CurrentOffset offset) {

    	final File file = new File(offset.getPath());
    	final String name = file.getName();

    	if(!file.exists())
    		return new Pair<>(name, "File isn't exists.");

    	StringBuilder sb = new StringBuilder();
    	try(Scanner scanner = new Scanner(file);) {

    		while(scanner.hasNextLine()) {

    			final String line = scanner.nextLine();

    			if(line.startsWith("pm-vmon-offset"))
    				continue;

    			sb.append(line).append(LINE_SEPARATOR);
    		}

    		offset.getOffsets().forEach(os->sb.append(os).append(LINE_SEPARATOR));

    	} catch (FileNotFoundException e) {
			logger.catching(e);
    		return new Pair<>(name, e.getLocalizedMessage());
		}

		try {
			Files.write(file.toPath(), sb.toString().getBytes());
		} catch (IOException e) {
			logger.catching(e);
    		return new Pair<>(name, e.getLocalizedMessage());
		}

		return new Pair<>(name, "Saved");
    }

    @GetMapping("profile")
    String getProfile(@RequestParam String sn, @RequestParam(required = false) Integer moduleId) throws IOException, URISyntaxException {
    	logger.traceEntry("{}; {};", sn, moduleId);

    	final URIBuilder builder;

    	if(moduleId==null) {

    		final URL url = new URL("http", sn, "/diagnostics.asp");
        	builder = new URIBuilder(url.toString()).setParameter("profile", "1");
        	String str = HttpRequest.getForString(builder.build().toString());
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
    String scanIP(@RequestParam String ip) throws UnknownHostException, IOException {
    	final InetAddress byName = InetAddress.getByName(ip);
		return Optional.of(byName.isReachable(400)).filter(b->b).map(b->byName.getCanonicalHostName()).orElse("");
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

		} catch (IOException e) {
			logger.catching(e);
		}
		return null;
    }

    @PostMapping("calibration_mode")
    CalibrationMode getCalibrationMode(@RequestParam String ip) throws IOException {

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

    @PostMapping("pll_registers")
    PLLRegister pllRegisters(@RequestParam String sn, @RequestParam String index) throws MalformedURLException, InterruptedException, ExecutionException, ScriptException{
//    	logger.error(sn);
    	return Optional.ofNullable(sn)

    			.map(
    					s->{
    						try {

    							final URL url = new URL("http", sn, "/device_debug_read.cgi");
    				    		List<NameValuePair> params = new ArrayList<>();
    				    		params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("devid", "1"), new BasicNameValuePair("command", "regs"), new BasicNameValuePair("groupindex", index)}));
    				    		FutureTask<PLLRegister> o = HttpRequest.postForIrtObgect(url.toString(), PLLRegister.class, params);
    				    		final PLLRegister pllRegister = o.get(5, TimeUnit.SECONDS);
//    				    		logger.error(pllRegister);
								return pllRegister;

    						} catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
    							logger.catching(new Throwable(sn, e));
    						}

    						return null;
    					})
    			.orElse(null);
    }

    @PostMapping("pll_register")
    PLLRegister pllRegister(@RequestParam String sn, String addr, String value) throws MalformedURLException, InterruptedException, ExecutionException, ScriptException{
    	logger.error(sn);
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
    				    		FutureTask<PLLRegister> o = HttpRequest.postForIrtObgect(url.toString(), PLLRegister.class, params);
    				    		final PLLRegister pllRegister = o.get(5, TimeUnit.SECONDS);
    				    		logger.error(pllRegister);
								return pllRegister;

    						} catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
    							logger.catching(new Throwable(sn, e));
    						}

    						return null;
    					})
    			.orElse(null);
    }

    @Getter @Setter @AllArgsConstructor @ToString
    public class Message{
    	private String content;
    }
}
