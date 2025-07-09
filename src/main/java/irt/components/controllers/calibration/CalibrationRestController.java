package irt.components.controllers.calibration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.CharBuffer;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.script.ScriptException;

import org.apache.http.NameValuePair;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.server.ResponseStatusException;

import irt.components.beans.OneCeHeader;
import irt.components.beans.OneCeUrl;
import irt.components.beans.PartNumber;
import irt.components.beans.SerialNumber;
import irt.components.beans.irt.AlarmInfo;
import irt.components.beans.irt.CalibrationInfo;
import irt.components.beans.irt.CalibrationRwInfo;
import irt.components.beans.irt.Etc;
import irt.components.beans.irt.HWInfo;
import irt.components.beans.irt.HomePageInfo;
import irt.components.beans.irt.Info;
import irt.components.beans.irt.MonitorInfo;
import irt.components.beans.irt.calibration.CalibrationMode;
import irt.components.beans.irt.calibration.Diagnostics;
import irt.components.beans.irt.calibration.HPBMRegisterV21;
import irt.components.beans.irt.calibration.HPBMRegisterV31;
import irt.components.beans.irt.calibration.NameIndexPair;
import irt.components.beans.irt.calibration.RegisterEmpty;
import irt.components.beans.irt.calibration.RegisterGates;
import irt.components.beans.irt.calibration.RegisterMeasurement;
import irt.components.beans.irt.calibration.RegisterPLL;
import irt.components.beans.irt.calibration.RegisterPm2Fpga;
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
import irt.components.workers.HttpRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("calibration/rest")
public class CalibrationRestController {
	private final static Logger logger = LogManager.getLogger();

	@Autowired private OneCeUrl oneCeApiUrl;

	@Autowired private CalibrationOutputPowerSettingRepository calibrationOutputPowerSettingRepository;
	@Autowired private CalibrationPowerOffsetSettingRepository calibrationPowerOffsetSettingRepository;
	@Autowired private CalibrationGainSettingRepository calibrationGainSettingRepository;
	@Autowired private BtrSettingRepository calibrationBtrSettingRepository;
	@Autowired private IrtArrayRepository	arrayRepository;

	@RequestMapping(path="monitorInfo", produces = "application/json;charset=utf-8")
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
    	logger.traceEntry("sn: {}", sn);

    	final CalibrationInfo info = Optional.ofNullable(sn)

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
    	logger.debug(info);
    	return info;
    }

	@PostMapping(path="irt_array", produces = "application/json;charset=utf-8")
	Optional<IrtArray> irtArray(@RequestParam String name, String id) {
		logger.traceEntry("name: {}; id: {};", name, id);
		IrtArrayId irtArrayId = new IrtArrayId(name, id);
		return arrayRepository.findById(irtArrayId);
	}

	@PostMapping(path="irt_array/save", produces = "application/json;charset=utf-8")
	IrtArray irtArraySave(@RequestParam String name, @RequestParam String id, @RequestParam String value) {
		logger.traceEntry("name: {}; id: {}; value: {}", name, id, value);
		IrtArrayId irtArrayId = new IrtArrayId(name, id);
		IrtArray irtArray = new IrtArray(irtArrayId, value);
		return arrayRepository.save(irtArray);
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
    	logger.traceEntry("{}", map);
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
//    	logger.err.traceEntry;

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
    	logger.traceEntry("settings: {}", settings);

		try {

			final String serialNumber = settings.getPartNumber();	// Serial Number set by java script
			final Function<String, String> consumer = partNumber->{

				return calibrationOutputPowerSettingRepository.findById(partNumber)
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

		    						settings.setPartNumber(partNumber);
		    						calibrationOutputPowerSettingRepository.save(settings);

		    						return "The setings has been saved.";
		    					});
			};

			return getPartNumber(serialNumber, consumer);

		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.catching(e);
		}

		return "Something went wrong.";
    }

    @GetMapping("power_offset")
    CalibrationPowerOffsetSettings getCalibrationPowerOffsetSettings(@RequestParam String sn) throws InterruptedException, ExecutionException, TimeoutException {
    	AtomicReference<CalibrationPowerOffsetSettings> ar = new AtomicReference<>();
    	getPartNumber(sn,
    			pn->{
    				calibrationPowerOffsetSettingRepository.findById(pn)
    				.ifPresent(ar::set);
    				return "";
    			});
		return ar.get();
    }

    @PostMapping(path="power_offset", consumes = MediaType.APPLICATION_JSON_VALUE)
    String saveCalibrationPowerOffsetSettings(@RequestBody CalibrationPowerOffsetSettings settings) {
    	logger.traceEntry("settings: {}", settings);

		try {

			final String serialNumber = settings.getPartNumber();	// Serial Number set by java script
			final Function<String, String> consumer = partNumber->{

				return calibrationPowerOffsetSettingRepository.findById(partNumber)
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
									settings.setPartNumber(partNumber);
									calibrationPowerOffsetSettingRepository.save(settings);

									return "The setings has been saved.";
								});
			};

			return getPartNumber(serialNumber, consumer);

		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.catching(e);
		}

		return "Something went wrong.";
    }

	public String getPartNumber(String serialNumber, Function<String, String> consumer) throws InterruptedException, ExecutionException, TimeoutException {

		return Optional.ofNullable(OneCeRestController.getOneCHeader(oneCeApiUrl, serialNumber).get(10, TimeUnit.SECONDS))

				.map(
						och->{
							final CalibrationGainSettings gainSettings = calibrationGainSettingRepository.findById(och.getProduct()).orElseGet(()->calibrationGainSettingRepository.findById(och.getSalesSKU()).orElse(null));
							final String partNumber = Optional.ofNullable(gainSettings).map(CalibrationGainSettings::getLocalPn).map(local->local ? och.getProduct() : och.getSalesSKU()).orElseGet(()->och.getSalesSKU());
							return consumer.apply(partNumber);
						})
				.orElseGet(
						()->{
							try {

								final PartNumber partNumber = BtrController.getSerialNumber(serialNumber).get(10, TimeUnit.SECONDS).getPartNumber();
								return consumer.apply(partNumber.getPartNumber());

							} catch (InterruptedException | ExecutionException | TimeoutException e) {
								logger.catching(e);
							}
							return "Something went wrong...";
						});
	}

    @PostMapping("gain")
    String saveGainSettings(@RequestParam String serialNumber, int startValue, int stopValue, int fields, boolean p1dB, boolean localPn) {

    	OneCeHeader oneCeHeader = null;
		try {
			oneCeHeader = OneCeRestController.getOneCHeader(oneCeApiUrl, serialNumber).get(5, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.catching(e);
		}
    	final String partNumber = Optional.ofNullable(oneCeHeader)

    			.map(och->localPn ? och.getProduct() : och.getSalesSKU())
    			.orElseGet(
    					()->{
    						try {

    							return BtrController.getSerialNumber(serialNumber).get(10, TimeUnit.SECONDS).getPartNumber().getPartNumber();
 
    						} catch (InterruptedException | ExecutionException | TimeoutException e) {
    							logger.catching(e);
    						}
    						return null;
    					});

    	if(partNumber==null)
    		return "The part number cannot be found out.";

    	if(!localPn && oneCeHeader!=null) 
    		calibrationGainSettingRepository.findById(oneCeHeader.getProduct())
    		.ifPresent(calibrationGainSettingRepository::delete);

    	final boolean isLocal = oneCeHeader!=null && localPn;
    	return calibrationGainSettingRepository.findById(partNumber)
    			.map(
    					ops->{

    						ops.setStartValue(startValue);
    						ops.setStopValue(stopValue);
    						ops.setFields(fields);
    						ops.setP1dB(p1dB);
    						ops.setLocalPn(isLocal);
    						calibrationGainSettingRepository.save(ops);

    						return "The setings has been updated.";
    					})
    			.orElseGet(
    					()->{

    						CalibrationGainSettings settings = new CalibrationGainSettings();
    						settings.setPartNumber(partNumber);
    						settings.setStartValue(startValue);
    						settings.setStopValue(stopValue);
    						settings.setFields(fields);
    						settings.setP1dB(p1dB);
    						settings.setLocalPn(isLocal);
							calibrationGainSettingRepository.save(settings);

    						return "The setings has been saved.";
    					});
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

    @GetMapping("sn")
    SerialNumber getSerialNumber(String sn){
    	final FutureTask<SerialNumber> ft = BtrController.getSerialNumber(sn);
    	try {
			return ft.get(10, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.catching(Level.DEBUG, e);
		}
		return null;
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

				CharBuffer chb = CharBuffer.allocate(2000);
				reader.read(chb);
				
			}
		}catch(Exception e) {
			logger.catching(Level.DEBUG, e);
			connection.disconnect();

			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getLocalizedMessage());
		}

		final int responseCode = connection.getResponseCode();
		connection.disconnect();

		logger.debug("responseCode: {}", responseCode);
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

    @PostMapping("calibration-mode")
    CalibrationMode calibrationMode(@RequestParam String sn, CalibrationMode.Status status) throws IOException, ScriptException {
    	logger.traceEntry("sn: {}; status: {}", sn, status);

		try {
			final URL url = new URL("http", sn, "/device_debug_read.cgi");
			logger.debug(url);
	    	final Integer systemIndex = CalibrationController.getSystemIndex(sn);
			final List<NameValuePair> params = new ArrayList<>();
	    	final BasicNameValuePair devId = new BasicNameValuePair("devid", systemIndex.toString());
			params.add(devId);

			Optional.ofNullable(status).map(Enum::ordinal)

					.map(
							ordinal->{
								params.add(new BasicNameValuePair("command", "regs"));
								params.add(new BasicNameValuePair("group", "51"));
								params.add(new BasicNameValuePair("address", "0"));
								params.add(new BasicNameValuePair("value", "" + ordinal));
								return HttpRequest.postForCode(url, params);
							})
					.ifPresent(
							ft->{
								try {

									params.clear();
									params.add(devId);

									final Integer statusCode = ft.get(5, TimeUnit.SECONDS);
									logger.debug(statusCode);

								} catch (InterruptedException | ExecutionException | TimeoutException e) {
									logger.catching(Level.DEBUG, e);
								}
					});

	    	//	Get status
	    	params.add(new BasicNameValuePair("command", "hwinfo"));
	    	params.add(new BasicNameValuePair("groupindex", "4"));

			return HttpRequest.postForIrtObgect(url.toString(), CalibrationMode.class, params).get(5, TimeUnit.SECONDS);

		} catch (InterruptedException | ExecutionException | TimeoutException | UnknownHostException e) {
			logger.catching(Level.DEBUG, e);
			return null;
		}
    }

    @PostMapping("calibration-mode-toggle")
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

			final String localizedMessage = e.getLocalizedMessage();
			if(logger.getLevel().compareTo(Level.DEBUG)<0)
				logger.warn(localizedMessage);

			return new ResponseEntity<>("setCalibrationMode(@RequestParam String ip);\n" + localizedMessage, HttpStatus.GATEWAY_TIMEOUT);
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

	@RequestMapping("all-modules")
	Map<String, Integer> allModules(@RequestParam String sn) throws IOException, InterruptedException, ExecutionException, TimeoutException, ScriptException {
    	logger.traceEntry("{}", sn);
		return HttpRequest.getAllModules(sn);
    }

    @PostMapping("pll_register")
    RegisterPLL pllRegister(@RequestParam String sn, String addr, String value, String regIndex) throws MalformedURLException, InterruptedException, ExecutionException, ScriptException{
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
    				    						new BasicNameValuePair("group", regIndex), // 102 or 103
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

    @GetMapping("diagnostic")
    String diagnostic(@RequestParam String sn, Integer devid, String command, Integer groupindex) throws IOException {
    	logger.traceEntry("sn: {}, devid: {}, command: {}, groupindex: {}", sn, devid, command, groupindex);
   		List<NameValuePair> params = new ArrayList<>();
		params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("devid", devid.toString()), new BasicNameValuePair("command", command)}));
		Optional.ofNullable(groupindex).ifPresent(gi->params.add(new BasicNameValuePair("groupindex", gi.toString())));
		URL url = new URL("http", sn, "/device_debug_read.cgi");
		return HttpRequest.postForString(url.toString(), params);
    }


    @PostMapping("pll_registers")
    RegisterPLL pllRegisters(@RequestParam String sn, String regIndex) throws MalformedURLException, InterruptedException, ExecutionException, ScriptException{
    	logger.traceEntry("sn: {}; regIndex: {}", sn, regIndex);
    	return diagnostics(RegisterPLL.class, sn, "regs", "1", regIndex, null, null, PostFor.IRT_OBJECT);
    }

	@PostMapping("module-info")
	Info info(@RequestParam String sn, Integer moduleIndex) {
		return diagnostics(Info.class, sn, "info", moduleIndex.toString(), null, null, null, PostFor.IRT_OBJECT);
    }

	@GetMapping("module-info")
	String infoString(@RequestParam String sn, String moduleIndex) throws IOException {
		final String url = new URL("http", sn, "/device_debug_read.cgi").toString();
		List<NameValuePair> params = new ArrayList<>();
		params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("devid", moduleIndex), new BasicNameValuePair("command", "info")}));
		return HttpRequest.postForString(url, params);
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

    @RequestMapping("measurement")
    RegisterMeasurement measurement(@RequestParam String sn, Integer moduleIndex){
		return diagnostics(RegisterMeasurement.class, sn, "status", moduleIndex.toString(), null, null, null, PostFor.IRT_OBJECT);
    }

    @PostMapping("sticker")
    Map<String, Map<String, String>> getSticker(@RequestParam String sn, Integer devid, Integer groupindex) throws IOException, InterruptedException, ExecutionException, TimeoutException, ScriptException{
    	logger.traceEntry("sn: {}; devid: {}, groupindex: {};", sn, devid, groupindex);

    	if(devid!=null) {
    		logger.error("devid: {}; groupindex: {}", devid);
    	}
    	final Map<String, Map<String, String>> map = new TreeMap<>();

    	HttpRequest.getAllModules(sn).entrySet()
    	.forEach(es->{
			try {

				final Map<String, String> values = new HashMap<>();
				// Info
				final BasicNameValuePair paieDevId = new BasicNameValuePair("devid", es.getValue().toString());
				final FutureTask<Info> ftInfo = CalibrationController.getHttpDeviceDebug(sn, Info.class, paieDevId, new BasicNameValuePair("command", "info"));

				final URL url = new URL("http", sn, "/device_debug_read.cgi");
				List<NameValuePair> list = new ArrayList<>();
				list.add(paieDevId);
				list.add(new BasicNameValuePair("command", "hwinfo"));
				// CPU Info
				final BasicNameValuePair groupIndex = new BasicNameValuePair("groupindex", "0");
				list.add(groupIndex);
				final String cpuInfo = HttpRequest.postForString(url.toString(), list);
				values.put("cpu", cpuInfo);

				// Help
				list.remove(groupIndex);
				list.add(new BasicNameValuePair("groupindex", "100"));
				final String help = HttpRequest.postForString(url.toString(), list);

				final Info info = ftInfo.get(10, TimeUnit.SECONDS);
				if(info==null)
					return;

				if(!help.contains("ETC")) {
					map.put(info.getSerialNumber(), values);
					return;
				}

				try(Scanner scanner = new Scanner(help);){
					while(scanner.hasNextLine()) {
						final String line = scanner.nextLine();
						if(line.contains("ETC")) {
							Arrays.stream(line.split("\\s+")).parallel().map(s->s.replaceAll("\\D", "")).filter(s->!s.isEmpty()).findAny()
							.ifPresent(
									index->{
										try {

											final String u = new URL("http", "irt-2415015", "/device_debug_read.cgi").toString();
											final List<NameValuePair> params = new ArrayList<>();
											params.add(new BasicNameValuePair("devid", "1002"));
											params.add(new BasicNameValuePair("command", "regs"));
											params.add(new BasicNameValuePair("groupindex", "28"));
											final Etc etc = HttpRequest.postForIrtObgect(u, Etc.class, params).get(10, TimeUnit.SECONDS);

											Optional.ofNullable(etc)
											.ifPresent(
													e->{
														values.put("sticker", etc.getEtc());
													});

											map.put(info.getSerialNumber(), values);

										} catch (MalformedURLException | InterruptedException | ExecutionException | TimeoutException e) {
											logger.catching(e);
										}
									});
						}
					}
				}

			} catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
				logger.catching(e);
			}
    	});

    	return map;
    }

    @PostMapping("diagnostic")
    String diagnostic(@RequestParam String sn, @RequestParam String moduleIndex, @RequestParam String command, String index, String address, String value) throws IOException{
    	logger.traceEntry("sn: {}; moduleIndex: {}; command: {}; index: {}; address: {}; value: {}", sn, moduleIndex, command, index, address, value);

    	URL url = new URL("http", sn, "/device_debug_read.cgi");
		List<NameValuePair> params = new ArrayList<>();
		params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("devid", moduleIndex), new BasicNameValuePair("command", command)}));

		Optional.ofNullable(index).ifPresent(i->params.add(new BasicNameValuePair("groupindex", i)));
		Optional.ofNullable(address).ifPresent(a->params.add(new BasicNameValuePair("address", a)));
		Optional.ofNullable(value).ifPresent(v->params.add(new BasicNameValuePair("value", v)));
		return HttpRequest.postForString(url.toString(), params);
    }

    @PostMapping("register/write")
    String rwRegister(@RequestParam String sn, String moduleId, String index, String address, String value){
    	diagnostics(RegisterEmpty.class, sn, "regs", moduleId, index, address, value, PostFor.IRT_OBJECT);
    	return sn;
    }

    @RequestMapping("open")
    Boolean open(@RequestParam String url, @RequestParam String path) throws InterruptedException, ExecutionException, TimeoutException{
    	logger.traceEntry("url: {}; path: {}", url, path);
    	List<NameValuePair> params = new ArrayList<>();
    	params.add(new BasicNameValuePair("path", path));
    	return HttpRequest.postForObgect(url + "/open", Boolean.class, params).get(1, TimeUnit.SECONDS);
    }

    @GetMapping("converter-info")
    NameIndexPair[] converterInfo(@RequestParam String sn) throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException{

    	return CalibrationController.getAllIndex(sn);
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
