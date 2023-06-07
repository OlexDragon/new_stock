package irt.components.controllers.calibration;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.NameValuePair;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import irt.components.beans.HttpSerialPortServersCollector;
import irt.components.beans.irt.Bias;
import irt.components.beans.irt.CalibrationInfo;
import irt.components.beans.irt.ConverterInfo;
import irt.components.beans.irt.Dacs;
import irt.components.beans.irt.HomePageInfo;
import irt.components.beans.irt.Info;
import irt.components.beans.irt.IrtFrequency;
import irt.components.beans.irt.IrtValue;
import irt.components.beans.irt.Monitor;
import irt.components.beans.irt.MonitorInfo;
import irt.components.beans.irt.SysInfo;
import irt.components.beans.irt.calibration.CalibrationTable;
import irt.components.beans.irt.calibration.NameIndexPair;
import irt.components.beans.irt.calibration.PowerDetectorSource;
import irt.components.beans.irt.calibration.ProfileTableDetails;
import irt.components.beans.jpa.btr.BtrSerialNumber;
import irt.components.beans.jpa.calibration.CalibrationGainSettings;
import irt.components.beans.jpa.calibration.CalibrationOutputPowerSettings;
import irt.components.beans.jpa.calibration.CalibrationPowerOffsetSettings;
import irt.components.beans.jpa.repository.btr.BtrSerialNumberRepository;
import irt.components.beans.jpa.repository.calibration.CalibrationBtrSettingRepository;
import irt.components.beans.jpa.repository.calibration.CalibrationGainSettingRepository;
import irt.components.beans.jpa.repository.calibration.CalibrationOutputPowerSettingRepository;
import irt.components.beans.jpa.repository.calibration.CalibrationPowerOffsetSettingRepository;
import irt.components.workers.HttpRequest;
import irt.components.workers.ProfileWorker;
import irt.components.workers.ThreadRunner;

@Controller
@RequestMapping("calibration")
public class CalibrationController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.profile.path}")
	private String profileFolder;

	@Autowired private HttpSerialPortServersCollector			 httpSerialPortServersCollector;
	@Autowired private CalibrationOutputPowerSettingRepository	 calibrationOutputPowerSettingRepository;
	@Autowired private CalibrationPowerOffsetSettingRepository	 calibrationPowerOffsetSettingRepository;
	@Autowired private CalibrationGainSettingRepository			 calibrationGainSettingRepository;
	@Autowired private CalibrationBtrSettingRepository			 calibrationBtrSettingRepository;
	@Autowired private BtrSerialNumberRepository				 serialNumberRepository;

	@GetMapping
    String calibration(@RequestParam(required = false) String sn, Model model) {
//    	logger.error(sn);

		final Map<String, String> httpSerialPortServers = httpSerialPortServersCollector.getHttpSerialPortServers();
		model.addAttribute("serialPortServers", httpSerialPortServers);

		Optional.ofNullable(sn)
    	.filter(s->!s.isEmpty())
    	.ifPresent(
    			s->{
    				model.addAttribute("serialNumber", s);
    				try {

     					final Integer devid = getSystemIndex(sn);
    					final Info info = getHttpDeviceDebug(sn, Info.class, new BasicNameValuePair("devid", devid.toString()), new BasicNameValuePair("command", "info")).get(10, TimeUnit.SECONDS);
    					model.addAttribute("info", info);
    					model.addAttribute("ip", sn);
    					model.addAttribute("devid", devid);

    					return;

    				} catch (TimeoutException | UnknownHostException | HttpHostConnectException e) {
						logger.catching(Level.DEBUG, e);

					} catch (Exception e) {
						logger.catching(e);
					}

    				try {
    					getHomePageInfo(sn)
    					.ifPresent(home->{
    						final SysInfo sysInfo = home.getSysInfo();
    						final Info info = new Info();
    						info.setSerialNumber(sysInfo.getSn());
    						info.setName(sysInfo.getDesc());
    						info.setPartNumber(sysInfo.getHw_id());
    						info.setSoftVertion(sysInfo.getFw_version());
    						model.addAttribute("info", info);
    						model.addAttribute("ip", home.getNetInfo().getAddr());
    					});
    				} catch (IOException e) {
    					logger.catching(e);
    				}
    			});

		return "calibration/calibration";
    }

    @GetMapping("output_power")
    String outputPower(@RequestParam String sn, @RequestParam String pn, Model model) {
    	logger.traceEntry(sn);

    	Optional.ofNullable(sn)
    	.filter(s->!s.isEmpty())
    	.ifPresent(
    			s->{
    				try {

    					final FutureTask<CalibrationInfo> httpUpdate = getHttpUpdate(s, CalibrationInfo.class, new BasicNameValuePair("exec", "calib_ro_info"));
    					// Get settings from DB
    					final CalibrationOutputPowerSettings settings = calibrationOutputPowerSettingRepository.findById(pn).orElseGet(()->new CalibrationOutputPowerSettings(pn, 30, 46));

//    					logger.error("calibrationInfo: {}", calibrationInfo);
    					model.addAttribute("serialNumber", s);
    					model.addAttribute("settings", settings);

    					final CalibrationInfo calibrationInfo = httpUpdate.get(10, TimeUnit.SECONDS);
    					final IrtValue power = Optional.ofNullable(calibrationInfo).map(CalibrationInfo::getBias).map(Bias::getPower).orElseGet(()->new IrtValue());
						model.addAttribute("power", power);

    				} catch (MalformedURLException | InterruptedException | ExecutionException | TimeoutException e) {
						throw new RuntimeException("Unable to connect to Unit.", e);
					}
    			});
        return "calibration/output_power :: outputPower";
    }

    @GetMapping("output_power/by_gain")
    String outputPowerByGain(@RequestParam String sn, @RequestParam String pn, Model model) {
    	logger.error(sn);

    	outputPower(sn, pn, model);
    	return "calibration/output_power_auto :: byGain";
    }

    @GetMapping("output_power/by_input")
    String outputPowerByInput(@RequestParam String sn, @RequestParam String pn, Model model) {

    	outputPower(sn, pn, model);
    	return "calibration/output_power_auto :: byInput";
    }

    @GetMapping("power_offset")
    String powerOffset(@RequestParam String sn, @RequestParam String pn, @RequestParam String deviceId, Model model) {
    	logger.traceEntry("{} : {} : {}", sn, pn, deviceId);

    	final String[] split = deviceId.split("\\.");
    	final int type = Integer.parseInt(split[0]);
    	final int revision = Integer.parseInt(split[1]);
		final boolean inHertz = (type==100 || type==101 || type==103) && revision<10;
		model.addAttribute("inHertz", inHertz);

		Optional.ofNullable(sn)
    	.filter(serialNumber->!serialNumber.isEmpty())
    	.ifPresent(
    			serialNumber->{

   					try {

   						final FutureTask<Void> taskLoFrequencty = new FutureTask<>(

   								()->{

   									// Get Converter Index.
   									URL url = new URL("http", serialNumber, "/update.cgi");
   									List<NameValuePair> params = new ArrayList<>();
   									params.add(new BasicNameValuePair("exec", "debug_devices"));
   									final NameIndexPair[] pairs = HttpRequest.postForIrtObgect(url.toString(), NameIndexPair[].class,  params).get(5, TimeUnit.SECONDS);
   									Optional.ofNullable(pairs).map(Arrays::stream).orElse(Stream.empty()).filter(p->p.getName().equals("FCM")).findAny().map(p->p.getIndex())
   									.ifPresent(
   											index->{

   						   						try {

   						   							// LO Frequency
   						   							final ConverterInfo converterInfo = getHttpDeviceDebug(serialNumber, ConverterInfo.class,
															new BasicNameValuePair("devid", index.toString()),
															new BasicNameValuePair("command", "config")).get(5, TimeUnit.SECONDS);
   						   							logger.debug(converterInfo);
   						   							Optional.ofNullable(converterInfo).map(ConverterInfo::getLoFrequency).map(IrtFrequency::new).flatMap(IrtFrequency::getValue).ifPresent(lo->model.addAttribute("loFrequencty", lo));

   						   						} catch (MalformedURLException | InterruptedException | ExecutionException | TimeoutException e) {
													logger.catching(e);
												}
   											});

   									return null;
   								});
   						ThreadRunner.runThread(taskLoFrequencty);

   	   					FutureTask<Void> ftProfile = new FutureTask<>(()->null);

   	    				ThreadRunner.runThread(
   								()->{

   									// Get Power table from the profile
   									Optional.of(getProfileWorker(sn, ftProfile, model))
   							    	.ifPresent(
   							    			pw->{
   												pw.getTable(ProfileTableDetails.OUTPUT_POWER.getDescription()).map(CalibrationTable::getTable).ifPresent(table->model.addAttribute("table", table));//TODO

   			    						    	// Get Power detector source
   			    						    	final PowerDetectorSource powerDetectorSource = pw.scanForPowerDetectorSource();
   							    				model.addAttribute("jsFunction", powerDetectorSource.getJsFunction());

   										    	ThreadRunner.runThread(ftProfile);
   							    			});
   								});

    					final FutureTask<CalibrationInfo> httpUpdate = getHttpUpdate(serialNumber, CalibrationInfo.class, new BasicNameValuePair("exec", "calib_ro_info"));
    					// Get settings from DB
    					final CalibrationPowerOffsetSettings settings = calibrationPowerOffsetSettingRepository.findById(pn).orElseGet(()->new CalibrationPowerOffsetSettings(pn, new BigDecimal(13.75, new MathContext(9)), new BigDecimal(14.5, new MathContext(9))));

//    					logger.error("calibrationInfo: {}", calibrationInfo);
    					model.addAttribute("serialNumber", serialNumber);
    					model.addAttribute("settings", settings);

    					final CalibrationInfo calibrationInfo = httpUpdate.get(10, TimeUnit.SECONDS);
    					final IrtValue power = Optional.ofNullable(calibrationInfo).map(CalibrationInfo::getBias).map(Bias::getPower).orElseGet(()->new IrtValue());
						model.addAttribute("power", power);

						ftProfile.get(10, TimeUnit.SECONDS);

						// LO Frequency. Don't stop process on error
   						try {

   							taskLoFrequencty.get(10, TimeUnit.SECONDS);

   						} catch (Exception e) {
							logger.catching(e);
						}

   					} catch (MalformedURLException | InterruptedException | ExecutionException | TimeoutException e) {
						logger.catching(e);
					}
    			});

		return "calibration/power_offset :: modal";
    }

	@GetMapping("gain")
    String gain(@RequestParam String sn, @RequestParam String pn, Model model) {
//    	logger.error(sn);

		Optional.ofNullable(sn)
    	.filter(s->!s.isEmpty())
    	.ifPresent(
    			s->{

					model.addAttribute("serialNumber", s);

					FutureTask<Void> ftProfile = gainFromProfile(sn, model);

					try {

    					final Integer devid = getSystemIndex(sn);
    					model.addAttribute("devid", devid);
    					final FutureTask<CalibrationInfo> httpUpdate = getHttpUpdate(s, CalibrationInfo.class, new BasicNameValuePair("exec", "calib_ro_info"));
    					final FutureTask<Dacs> httpDeviceDebug = getHttpDeviceDebug(s, Dacs.class,
    							new BasicNameValuePair("devid", devid.toString()),
    							new BasicNameValuePair("command", "regs"),
    							new BasicNameValuePair("groupindex", "100"));

    					final CalibrationInfo calibrationInfo = httpUpdate.get(10, TimeUnit.SECONDS);
    					final Dacs dacs = httpDeviceDebug.get(10, TimeUnit.SECONDS);

//    					logger.error("dacs: {}", dacs);
    					Optional.ofNullable(calibrationInfo.getBias()).ifPresent(biasBoard->model.addAttribute("temperature", biasBoard.getTemperature()));
    					model.addAttribute("dac2", dacs.getDac2RowValue());

    					// Get settings from DB
    					final CalibrationGainSettings settings = calibrationGainSettingRepository.findById(pn).orElseGet(()->new CalibrationGainSettings(pn, -40, 85));
    					model.addAttribute("settings", settings);

						ftProfile.get(10, TimeUnit.SECONDS);

    				} catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
						logger.catching(e);
					}
    			});

    	return "calibration/gain :: modal";
    }

	@GetMapping("current_offset")
    String currentOffset(@RequestParam String sn, Model model) {
//    	logger.error(sn);

		Optional.ofNullable(sn)
    	.filter(s->!s.isEmpty())
    	.ifPresent(s->model.addAttribute("serialNumber", s));

    	return "calibration/current_offset :: modal";
    }

	@GetMapping("btr")
    String measurement(@RequestParam String sn, @RequestParam String pn, @RequestParam(required = false) Boolean setting, Model model) {
    	logger.traceEntry("sn: {}; pn: {}", sn, pn);

		model.addAttribute("serialNumber", sn);

		final Optional<BtrSerialNumber> oSerialNumber = serialNumberRepository.findBySerialNumber(sn);
		//The DB serial number does not exist. Redirect to adding a serial number to the database.
    	if(!oSerialNumber.isPresent()) {
    		model.addAttribute("showSetting", true);
    		return "calibration/btr_table :: modal";
    	}
 
    	model.addAttribute("dbSerialNumber", oSerialNumber.get());
    	model.addAttribute("partNumber", pn);

		// Get settings from DB
		calibrationBtrSettingRepository.findById(pn).ifPresent(
				btrSetting->{

					logger.debug(btrSetting);
					model.addAttribute("settings", btrSetting);

					final Optional<Boolean> oSetting = Optional.ofNullable(setting).filter(s->s);
					oSetting.ifPresent(s->model.addAttribute("showSetting", s));

					if(oSetting.isPresent())
						return;

					Optional.ofNullable(sn)
			    	.filter(s->!s.isEmpty())
			    	.ifPresent(
			    			serialNumber->{

								FutureTask<Void> ftProfile = gainFromProfile(sn, model);

								try {

									getUnitMonitor(serialNumber)
									.ifPresent(data->model.addAttribute("monitor", data));

									ftProfile.get(10, TimeUnit.SECONDS);

			    				} catch (MalformedURLException | InterruptedException | ExecutionException | TimeoutException e) {
									logger.catching(e);
								}
			    			});
				});

    	return "calibration/btr_table :: modal";
    }

	public static Optional<Monitor> getUnitMonitor(String serialNumber) throws InterruptedException, ExecutionException, TimeoutException, MalformedURLException {
		return Optional.ofNullable(getHttpUpdate(serialNumber, MonitorInfo.class, new BasicNameValuePair("exec", "mon_info")).get(5, TimeUnit.SECONDS)).map(MonitorInfo::getData);
	}

	@GetMapping("currents")
    String currents(@RequestParam String sn, Model model) {

		model.addAttribute("serialNumber", sn);

		return "calibration/currents :: modal";
	}

	public FutureTask<Void> gainFromProfile(String sn, Model model) {
		FutureTask<Void> ftProfile = new FutureTask<>(()->null);

		ThreadRunner.runThread(
				()->{

					// Get 'zero-attenuation-gain' from the profile
					Optional.ofNullable(getProfileWorker(sn, ftProfile, model))
					.ifPresent(
							pw->{
								pw.getGain().ifPresent(gain->model.addAttribute("gain", gain));
						    	ThreadRunner.runThread(ftProfile);
							});
				});
		return ftProfile;
	}

	@GetMapping("upload_modules_menu")
    String getUploadModuleMenu(@RequestParam String sn, Model model) throws IOException {

		model.addAttribute("serialNumber", sn);

		final List<Info> infos = getModulesInfo(sn);

//    	logger.error(infos);
    	model.addAttribute("modules", infos);

    	return "calibration/calibration :: upload_modules_menu";
    }

	@GetMapping("modules_profile_path_menu")
    String getModuleProfilePathMenu(@RequestParam String sn, Model model) throws IOException {
//		logger.error(sn);

		final List<Info> infos = getModulesInfo(sn);

//    	logger.error(infos);
    	model.addAttribute("modules", infos);

    	return "calibration/calibration :: modules_profile_path_menu";
    }

	@GetMapping("modules_profile_menu")
    String getModuleProfileMenu(@RequestParam String sn, Model model) throws IOException {
//		logger.error(sn);

		model.addAttribute("serialNumber", sn);

		final List<Info> infos = getModulesInfo(sn);

//    	logger.error(infos);
    	model.addAttribute("modules", infos);

    	return "calibration/calibration :: modules_profile_menu";
    }

	@GetMapping("power_chart")
    String powerChart(@RequestParam String sn, Model model) throws IOException {
//		logger.error(sn);

		getHomePageInfo(sn)
		.ifPresent(
				hpi->{
					model.addAttribute("serialNumber", hpi.getSysInfo().getSn());
					model.addAttribute("ip", hpi.getNetInfo().getAddr());
				});

    	return "calibration/power_chart :: modal";
    }

	private List<Info> getModulesInfo(String sn) throws IOException {

		final Map<String, Integer> allDevices = HttpRequest.getAllDevices(sn);
    	allDevices.remove("System");

    	return allDevices.entrySet().stream()
    			.map(
    					es->{
    						try {

    							final URL url = new URL("http", sn, "/device_debug_read.cgi");
    							List<NameValuePair> params = new ArrayList<>();
    							params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("devid", es.getValue().toString()), new BasicNameValuePair("command", "info")}));

    							final Info info = HttpRequest.postForIrtObgect(url.toString(), Info.class, params).get(1, TimeUnit.SECONDS);
    							info.setModuleId(es.getValue());
								return info;

    						} catch (InterruptedException | ExecutionException | TimeoutException | MalformedURLException e) {
    							logger.catching(Level.DEBUG, e);
    						}
    						return null;
    					})
    			.filter(info->info!=null).collect(Collectors.toList());
	}

	private ProfileWorker getProfileWorker(String sn, FutureTask<Void> ftProfile, Model model) {
		ProfileWorker profileWorker = new ProfileWorker(profileFolder, sn);
		try {
			if(!profileWorker.exists()) {
				model.addAttribute("message", "The profile could not be found.");
				ThreadRunner.runThread(ftProfile);
				return null;
			}

		} catch (Exception e) {
			logger.catching(e);
			model.addAttribute("message", e.getLocalizedMessage());
			ThreadRunner.runThread(ftProfile);
			return null;
		}
		return profileWorker;
	}

	public static Optional<HomePageInfo> getHomePageInfo(String sn) throws IOException {
		return Optional.ofNullable(getHonePage(sn)).map(HomePageInfo::new);
	}

	public static Integer getSystemIndex(String sn) throws IOException{

		final Map<String, Integer> allDevices = HttpRequest.getAllDevices(sn);
		return Optional.ofNullable(allDevices.get("System")).orElse(1);
	}

	public static <T> FutureTask<T> getHttpUpdate(String ipAddress, Class<T> toClass, BasicNameValuePair...basicNameValuePairs) throws MalformedURLException {


		final URL url = new URL("http", ipAddress, "/update.cgi");
		List<NameValuePair> params = new ArrayList<>();
		params.addAll(Arrays.asList(basicNameValuePairs));

		return HttpRequest.postForIrtObgect(url.toString(), toClass, params);
}

	public static <T> FutureTask<T> getHttpDeviceDebug(String ipAddress, Class<T> toClass, BasicNameValuePair...basicNameValuePairs) throws MalformedURLException{

			final URL url = new URL("http", ipAddress, "/device_debug_read.cgi");
			logger.debug(url);
			List<NameValuePair> params = new ArrayList<>();
			params.addAll(Arrays.asList(basicNameValuePairs));

			return HttpRequest.postForIrtObgect(url.toString(), toClass, params);
	}

	private static String getHonePage(String ipAddress) throws IOException {

		final URL url = new URL("http", ipAddress, "/overview.asp");
		logger.debug(url);

		return HttpRequest.getForString(url.toString());
}
}
