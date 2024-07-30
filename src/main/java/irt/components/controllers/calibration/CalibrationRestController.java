package irt.components.controllers.calibration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.springframework.data.util.Pair;
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
import irt.components.beans.irt.CalibrationRwInfo;
import irt.components.beans.irt.Info;
import irt.components.beans.irt.MonitorInfo;
import irt.components.beans.irt.calibration.CalibrationMode;
import irt.components.beans.irt.calibration.HPBMRegisterV21;
import irt.components.beans.irt.calibration.HPBMRegisterV31;
import irt.components.beans.irt.calibration.InitializeSetting;
import irt.components.beans.irt.calibration.ProfileTableTypes;
import irt.components.beans.irt.calibration.Register;
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
import irt.components.services.InitializeSettingConverter;
import irt.components.workers.HtmlParsel;
import irt.components.workers.HttpRequest;
import irt.components.workers.ProfileWorker;
import irt.components.workers.ThreadRunner;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@RestController
@RequestMapping("calibration/rest")
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

	@PostMapping(path="calibration-rw-info", produces = "application/json;charset=utf-8")
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

	private final int dac1 = 0x10000;
	private final String dac1Str = "DAC1:";
	@PostMapping("initialize/reg-addr-val")
	Object initialize(@RequestParam String sn, Integer moduleId, String deviceId) throws IOException {
    	logger.traceEntry("sn: {}; moduleId: {}; deviceId: {}", sn, moduleId, deviceId);

    	final Optional<IrtArray> oIrtArray = arrayRepository.findById(new IrtArrayId("initialize", deviceId));
    	if(!oIrtArray.isPresent())
    		return "There are no settings for this device.\nCall Roman to fix this.";

    	final String description = oIrtArray.get().getDescription();
    	if(description==null || description.isEmpty()) {
    		logger.warn("THe data is empty.");
    		return "";
    	}
		final InitializeSetting setting = new InitializeSettingConverter().convertToEntityAttribute(description);
		final Integer regIndex = setting.getRegIndex();
		if(setting.getRegIndex()==null || regIndex<0)
			return "";

		final String str = diagnosticsReg(sn, moduleId, regIndex);
    	final Set<Entry<String, Integer>> entrySet = setting.getNameValue().entrySet();
    	final List<Pair<Integer, Integer>> addrVal = new ArrayList<>();

    	final AtomicInteger reg = new AtomicInteger();
    	try(Scanner scanner = new Scanner(str)){
    		while(scanner.hasNextLine()) {
    			final String nextLine = scanner.nextLine().trim();
    			if(nextLine.startsWith(dac1Str))
    				reg.set(dac1);
    			entrySet.parallelStream().filter(e->nextLine.startsWith(e.getKey())).findAny()
    			.ifPresent(e->{
    				final int split = Integer.parseInt(nextLine.split("0x", 2)[1].split("\\)", 2)[0], 16);
    				addrVal.add(Pair.of(reg.get() + split, e.getValue()));
    			});
    		}
    		Map<String, Object> map = new HashMap<>();
    		map.put("regIndex", setting.getRegIndex());
    		map.put("addrVal", addrVal);
			return map;
    	}
     }

	private String diagnosticsReg(String sn, Integer moduleId, final Integer regIndex) throws MalformedURLException, IOException {
		final URL url = new URL("http", sn, "/device_debug_read.cgi");
		final List<NameValuePair> params = new ArrayList<>();
		params.addAll(Arrays.asList(
				new BasicNameValuePair[]{
						new BasicNameValuePair("devid", moduleId.toString()),
						new BasicNameValuePair("command", "regs"),
						new BasicNameValuePair("groupindex", regIndex.toString())}));

		return HttpRequest.postForString(url.toString(), params);
	}

	@PostMapping("initialize/save")
	Message initializeSave(@RequestBody InitializeSetting setting) {
    	logger.traceEntry("setting: {}", setting);

    	if(setting.getDeviceId()==null || setting.getDeviceId().isEmpty())
        	return new Message("Values ​​cannot be saved.\nDevice ID is missing.");

    	final IrtArrayId irtArrayId = new IrtArrayId("initialize", setting.getDeviceId());
    	final IrtArray irtArray = arrayRepository.findById(irtArrayId).orElseGet(()->new IrtArray(irtArrayId, ""));
    	irtArray.setDescription(new InitializeSettingConverter().convertToDatabaseColumn(setting));
    	arrayRepository.save(irtArray);
    	return new Message("");
    }

	@PostMapping("initialize/data")
	Map<String, Object> initializeData(@RequestParam String sn, Integer moduleId, String deviceId) {
    	logger.traceEntry("sn: {}; moduleId: {}; deviceId: {}", sn, moduleId, deviceId);

    	final Map<String, Object> map = new HashMap<>();
    	arrayRepository.findById(new IrtArrayId("initialize", deviceId)).map(IrtArray::getDescription).map(d->new InitializeSettingConverter().convertToEntityAttribute(d))
    	.ifPresent(
    			setting->{
    				map.put("setting", setting);
    				Optional.ofNullable(setting.getRegIndex())
    				.ifPresent(regIndex->{
    					try {

    						final String str = diagnosticsReg(sn, moduleId, regIndex);
							map.put("regs", str);

    					} catch (IOException e) {
    						if(logger.getLevel().compareTo(Level.ERROR)>0)
    							logger.warn(e.getLocalizedMessage());
    						else
    							logger.catching(e);
						}
    				});
    			});
    	return map;
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

    @PostMapping("current_offset")
    List<CurrentOffset> currentOffset(@RequestParam String sn, @RequestParam Boolean local) throws UnknownHostException {
    	logger.traceEntry("Serial Number: {}; local: {}", sn, local);

    	final InetAddress byName = InetAddress.getByName(sn);
    	final byte[] bytes = byName.getAddress();
    	final int length = bytes.length;
    	final List<CurrentOffset> offsets = new ArrayList<>();

    	if(length!=4) {
    		final CurrentOffset co = new CurrentOffset("ERROR");
    		co.getOffsets().add("Unable to get an IP address.");
    		logger.warn("{} - Unable to get an IP address. {}", sn, bytes);
			offsets.add(co);
    		return offsets;
    	}

    	final String ipAddress = IntStream.range(0, length).map(index->bytes[index]&0xff).mapToObj(Integer::toString).collect(Collectors.joining("."));
		logger.debug("ipAddress: {}", ipAddress);

    	 try {

    		 List<String> commands = new ArrayList<>();
    		 commands.add(perlPath);
    		 commands.add(perlScriptPath);
    		 commands.add(perlScriptArgument + ipAddress);
    		 if(local)
    			 commands.add("--local=1");

    		 logger.debug("{}", commands);
 
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
    			            		}else if(line.startsWith("pm-vmon-offset") || line.startsWith("device-vmon")) {
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
    	logger.traceEntry("{}", offset);

    	final File file = new File(offset.getPath());
    	final String name = file.getName();

    	if(!file.exists())
    		return Pair.of(name, "File isn't exists.");

    	StringBuilder sb = new StringBuilder();
    	try(Scanner scanner = new Scanner(file);) {

    		while(scanner.hasNextLine()) {

    			final String line = scanner.nextLine();

    			if(line.startsWith("pm-vmon-offset") || (line.startsWith("device-vmon") && hasIndex(line))) {
    				continue;
    			}

    			sb.append(line).append(LINE_SEPARATOR);
    		}

    		offset.getOffsets().forEach(os->sb.append(os).append(LINE_SEPARATOR));

    	} catch (FileNotFoundException e) {
			logger.catching(e);
    		return Pair.of(name, e.getLocalizedMessage());
		}

		try {
			Files.write(file.toPath(), sb.toString().getBytes());
		} catch (IOException e) {
			logger.catching(e);
    		return Pair.of(name, e.getLocalizedMessage());
		}

		return Pair.of(name, "Saved");
    }

    private final static String[] indexis = new String[] {"101", "103", "105", "107", "109", "111"};
    private boolean hasIndex(String line) {

    	final String[] split = line.split("\\s+");
    	if(split.length<5)
    		return false;

    	return Arrays.stream(indexis).filter(split[2]::equals).findAny().map(i->true).orElse(false);
	}

	@GetMapping("profile")
    String profile(@RequestParam String sn, @RequestParam(required = false) Integer moduleId) throws IOException, URISyntaxException {
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
    boolean setDac(@RequestParam String sn, Integer channel, Integer index, Integer value, Boolean save){
    	logger.traceEntry("sn: {}; channel: {}; index: {}; value: {}", sn, channel, index, value);

		try {

			final URL url = new URL("http", sn, "/calibration.cgi");
			List<NameValuePair> params = new ArrayList<>();
			final List<BasicNameValuePair> asList = new ArrayList<>(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("channel", channel.toString()), new BasicNameValuePair("index", index.toString())}));
			Optional.ofNullable(save).ifPresent(s->asList.add(new BasicNameValuePair("save_dp", "1")));
			Optional.ofNullable(value).ifPresent(s->asList.add(new BasicNameValuePair("value", value.toString())));
			params.addAll(asList);

			HttpRequest.postForIrtObgect(url.toString(), Object.class, params).get(5, TimeUnit.SECONDS);
			return true;

		} catch (MalformedURLException | InterruptedException | ExecutionException | TimeoutException e) {
			logger.catching(e);
			return false;
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
    SimpleEntry<String, HPBMRegisterV21> hpbmRegisterV21(@RequestParam String sn, @RequestParam String devid) throws MalformedURLException, InterruptedException, ExecutionException{
    	logger.traceEntry("sn: {}, devid: {}", sn, devid);

		final URL url = new URL("http", sn, "/device_debug_read.cgi");
		List<NameValuePair> params = new ArrayList<>();
		final BasicNameValuePair deviceId = new BasicNameValuePair("devid", devid);

		params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("command", "regs"), deviceId, new BasicNameValuePair("groupindex", "20")}));

		final FutureTask<HPBMRegisterV21> postForIrtObgect1 = HttpRequest.postForIrtYaml(url.toString(), HPBMRegisterV21.class, params);

		SimpleEntry<String, HPBMRegisterV21> entry = null;
		try {

			HPBMRegisterV21 object1 = postForIrtObgect1.get(2, TimeUnit.SECONDS);
//			logger.debug(object1);
			entry = new AbstractMap.SimpleEntry<>(devid, object1);

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

    @PostMapping("pll_registers")
    RegisterPLL pllRegisters(@RequestParam String sn) throws MalformedURLException, InterruptedException, ExecutionException, ScriptException{
//    	logger.error(sn);
    	return unitRegister(RegisterPLL.class, sn, "1", "102", null, null, PostFor.IRT_OBJECT);
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
    				    		logger.error(pllRegister);
								return pllRegister;

    						} catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
    							logger.catching(new Throwable(sn, e));
    						}

    						return null;
    					})
    			.orElse(null);
    }

    @GetMapping("register-pm2-fpga")
    RegisterPm2Fpga getRegisterPm2Fpga(@RequestParam String sn, Integer deviceId, @RequestParam(required = false) String address, @RequestParam(required = false) String value){
    	logger.traceEntry("Serial Number: {}; deviceId: {}; address: {}; value: {}", sn, deviceId, address, value);
		return unitRegister(RegisterPm2Fpga.class, sn, deviceId.toString(), "25", address, value, PostFor.IRT_OBJECT);
    }

    @GetMapping("register-gates")
    RegisterGates getRegisterDacs(@RequestParam String sn, Integer deviceId, @RequestParam(required = false) String address, @RequestParam(required = false) String value){
    	logger.traceEntry("Serial Number: {}; deviceId: {}; address: {}; value: {}", sn, deviceId, address, value);
		return unitRegister(RegisterGates.class, sn, deviceId.toString(), "27", address, value, PostFor.STRING);
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
    String calibrationMode(@RequestParam String sn) throws URISyntaxException, IOException{

    	final URL url = new URL("http", sn, "/calibration.asp");
    	String str = HttpRequest.getForString( url.toString());
    	logger.error(str);
		final HtmlParsel htmlParsel = new HtmlParsel("script");
		final List<String> all = htmlParsel.parseAll(str);
    	logger.error(all);
    	return sn;
    }

    @PostMapping("register/write")
    String rwRegister(@RequestParam String sn, @RequestParam String moduleId, @RequestParam String index, @RequestParam String address, @RequestParam String value){
    	logger.traceEntry("snL {}; moduleId: {}; index: {}; address: {}; value: {}", sn, moduleId, index, address, value);
    	Register o = unitRegister(RegisterEmpty.class, sn, moduleId, index, address, value, PostFor.IRT_OBJECT);
    	return sn;
    }

	private <T extends Register> T unitRegister(Class<T> registerClass, String sn, String deviceId, String index, String address, String value, PostFor postFor) {
		return Optional.ofNullable(sn)

    			.map(
    					s->{
    						try {

    				    		List<NameValuePair> params = new ArrayList<>();
    				    		params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("devid", deviceId), new BasicNameValuePair("command", "regs")}));

    				    		final URL url;
    				    		if(address==null || value == null) {
    				    			url = new URL("http", sn, "/device_debug_read.cgi");
	    							params.add(new BasicNameValuePair("groupindex", index));
    				    		}else {
    				    			url = new URL("http", sn, "/device_debug_write.cgi");
	    							params.add(new BasicNameValuePair("group", index));
	    							params.add(new BasicNameValuePair("address", address));
	    							params.add(new BasicNameValuePair("value", value));
    				    		}

    				    		FutureTask<T> ft = getIrtObject(url, registerClass, params, postFor);
    				    		return ft.get(5, TimeUnit.SECONDS);

    						} catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
    							logger.catching(new Throwable(sn, e));
    						}

    						return null;
    					})
    			.orElse(null);
	}

	private <T extends Register> FutureTask<T> getIrtObject(final URL url, Class<T> registerClass, List<NameValuePair> params, PostFor postFor) throws IOException {
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
    public class Message{
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
