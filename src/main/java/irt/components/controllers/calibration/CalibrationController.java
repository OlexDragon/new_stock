package irt.components.controllers.calibration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.script.ScriptException;

import org.apache.http.NameValuePair;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import irt.components.beans.ProductionOrder;
import irt.components.beans.ProductionOrderResponse;
import irt.components.beans.irt.CalibrationInfo;
import irt.components.beans.irt.ConverterInfo;
import irt.components.beans.irt.Dacs;
import irt.components.beans.irt.HomePageInfo;
import irt.components.beans.irt.Info;
import irt.components.beans.irt.IrtFrequency;
import irt.components.beans.irt.Monitor;
import irt.components.beans.irt.MonitorInfo;
import irt.components.beans.irt.SysInfo;
import irt.components.beans.irt.calibration.CalibrationTable;
import irt.components.beans.irt.calibration.NameIndexPair;
import irt.components.beans.irt.calibration.PowerDetectorSource;
import irt.components.beans.irt.calibration.ProfileTableDetails;
import irt.components.beans.jpa.IrtArray;
import irt.components.beans.jpa.IrtArrayId;
import irt.components.beans.jpa.btr.BtrSerialNumber;
import irt.components.beans.jpa.btr.BtrSetting;
import irt.components.beans.jpa.btr.BtrWorkOrder;
import irt.components.beans.jpa.calibration.CalibrationGainSettings;
import irt.components.beans.jpa.calibration.CalibrationOutputPowerSettings;
import irt.components.beans.jpa.calibration.CalibrationPowerOffsetSettings;
import irt.components.beans.jpa.repository.IrtArrayRepository;
import irt.components.beans.jpa.repository.btr.BtrSerialNumberRepository;
import irt.components.beans.jpa.repository.btr.BtrWorkOrderRepository;
import irt.components.beans.jpa.repository.calibration.BtrSettingRepository;
import irt.components.beans.jpa.repository.calibration.CalibrationGainSettingRepository;
import irt.components.beans.jpa.repository.calibration.CalibrationOutputPowerSettingRepository;
import irt.components.beans.jpa.repository.calibration.CalibrationPowerOffsetSettingRepository;
import irt.components.services.HttpSerialPortServersKeeper;
import irt.components.services.converter.InitializeSettingConverter;
import irt.components.workers.HttpRequest;
import irt.components.workers.ProfileWorker;
import irt.components.workers.ThreadRunner;

@Controller
@RequestMapping("calibration")
public class CalibrationController {
	private final static Logger logger = LogManager.getLogger();

	private static final int WORK_ORDER = 3;

	private static final int SERIAL_NUMBER = 1;

	private static final String WO_NOT_FOUND = "WO Not Found";

	@Value("${irt.url.protocol}")
	private String protocol;

	@Value("${irt.url.login}")
	private String login;

	@Value("${irt.url}")
	private String url;

	@Value("${irt.profile.path}")
	private String profileFolder;

	@Value("${irt.log.file}")
	private String logFile;

	@Autowired private HttpSerialPortServersKeeper			 	 httpSerialPortServersKeeper;
	@Autowired private CalibrationOutputPowerSettingRepository	 calibrationOutputPowerSettingRepository;
	@Autowired private CalibrationPowerOffsetSettingRepository	 calibrationPowerOffsetSettingRepository;
	@Autowired private CalibrationGainSettingRepository			 calibrationGainSettingRepository;

	@Autowired private BtrSettingRepository			btrSettingRepository;
	@Autowired private BtrWorkOrderRepository		workOrderRepository;
	@Autowired private BtrSerialNumberRepository	serialNumberRepository;

	@Autowired private IrtArrayRepository	arrayRepository;

	@GetMapping
    String calibration(@RequestParam(required = false) String sn, Model model) {
    	logger.traceEntry(sn);

		final Map<String, String> httpSerialPortServers = httpSerialPortServersKeeper.getHttpSerialPortServers();
		model.addAttribute("serialPortServers", httpSerialPortServers);

		Optional.ofNullable(sn)
		.map(String::trim)
		.filter(s->!s.isEmpty())
		.map(String::toUpperCase)
		.map(
				s->{

					final char charAt = s.charAt(0);
					if((charAt>='A' && charAt<='Z') || s.contains("."))
						return s;

					return "IRT-" + s;
				})
    	.filter(s->!s.trim().isEmpty())
    	.ifPresent(
    			s->{
    				try {

     					final Integer devid = getSystemIndex(s);
    					final Info info = getHttpDeviceDebug(s, Info.class, new BasicNameValuePair("devid", devid.toString()), new BasicNameValuePair("command", "info")).get(10, TimeUnit.SECONDS);
    					model.addAttribute("info", info);
    					model.addAttribute("ip", s);
    					model.addAttribute("devid", devid);
        				model.addAttribute("serialNumber", info.getSerialNumber());
    					logger.debug(info);

    					return;

    				} catch (TimeoutException | UnknownHostException | HttpHostConnectException | NullPointerException | ExecutionException e) {
						logger.catching(Level.DEBUG, e);

					} catch (Exception e) {
						logger.catching(e);
					}

    				try {
    					getHomePageInfo(s, 400)
    					.ifPresent(home->{
    						final SysInfo sysInfo = home.getSysInfo();
    						final Info info = new Info();
    						info.setSerialNumber(sysInfo.getSn());
    						info.setName(sysInfo.getDesc());
    						info.setPartNumber(sysInfo.getHw_id());
    						info.setSoftVertion(sysInfo.getFw_version());
    						info.setDeviceId(sysInfo.getDevid());
    						logger.debug("{}; {};", sysInfo, info);

    						model.addAttribute("info", info);
    						model.addAttribute("ip", home.getNetInfo().getAddr());
            				model.addAttribute("serialNumber", info.getSerialNumber());
    					});
    				} catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
    					logger.catching(Level.DEBUG, e);
    				}
    			});

		return "calibration/calibration";
    }

	@GetMapping("converter/input-power")
    String inputPowerConverter() throws ExecutionException {
		logger.traceEntry();
		return "calibration/serial/fcm_input_power :: converter";
    }

    @GetMapping("converter/output-power")
    String outputPowerConverter() throws ExecutionException {
		logger.traceEntry();
		return "calibration/serial/fcm_output_power :: converter";
    }

    @GetMapping("converter/gain")
    String gainConverter() throws ExecutionException {
		logger.traceEntry();
		return "calibration/serial/fcm_gain :: converter";
    }

    @GetMapping("initialize")
    String initialize(@RequestParam String sn, Model model) throws ExecutionException, IOException, InterruptedException, TimeoutException, ScriptException {
		logger.traceEntry("sn: {}", sn);

		model.addAttribute("serialNumber", sn);

		List<NameValuePair> params = new ArrayList<>();
		params.addAll(Arrays.asList(new BasicNameValuePair("command", "devices" )));

		final Map<String, Integer> devices = HttpRequest.getAllModules(sn);
		final List<Info> infos = getInfos(sn, devices);
		infos.sort((a,b)->b.getDeviceId().compareTo(a.getDeviceId()));
		model.addAttribute("infos", infos);
		logger.debug(infos);

		return "calibration/initialize :: modal";
    }

    @GetMapping("initialize/setting")
    String initializeSetting(@RequestParam String deviceId, Model model) throws ExecutionException, IOException {
		logger.traceEntry("deviceId: ", deviceId);

		model.addAttribute("deviceId", deviceId);
		arrayRepository.findById(new IrtArrayId("initialize", deviceId)).ifPresent(irtArray->model.addAttribute("setting", new InitializeSettingConverter().convertToEntityAttribute(irtArray.getDescription())));

		return "calibration/initialize :: setting";
    }

    @GetMapping("output_power")
    String outputPower(@RequestParam String sn, @RequestParam String pn, Model model) throws ExecutionException {
    	logger.traceEntry(sn);

    	Optional.ofNullable(sn)
		.map(String::trim)
    	.filter(s->!s.isEmpty())
    	.ifPresent(
    			s->{
    				// Get settings from DB
					final CalibrationOutputPowerSettings settings = calibrationOutputPowerSettingRepository.findById(pn).orElseGet(()->new CalibrationOutputPowerSettings(pn, 30, 46, "power1"));

//    					logger.error("calibrationInfo: {}", calibrationInfo);
					model.addAttribute("serialNumber", s);
					model.addAttribute("settings", settings);
    			});
        return "calibration/output_power :: outputPower";
    }

    @GetMapping("output_power/by_input")
    String outputPowerByInput(@RequestParam String sn, @RequestParam String pn, Model model) throws ExecutionException {

//    	model.addAttribute("byInput", true);
    	outputPower(sn, pn, model);
    	return "calibration/output_power_auto :: byInput";
    }

    @GetMapping("output_power/by_gain")
    String outputPowerByGain(@RequestParam String sn, @RequestParam String pn, Model model) throws ExecutionException {
    	logger.error(sn);

    	outputPower(sn, pn, model);
    	return "calibration/output_power_auto :: byGain";
    }

    @GetMapping("power_offset")
    String modalPowerOffset(@RequestParam String sn, @RequestParam String pn, @RequestParam String deviceId, Model model) {
    	logger.traceEntry("{} : {} : {}", sn, pn, deviceId);

    	final String[] split = deviceId.split("\\.");
    	final int type = Integer.parseInt(split[0]);
    	final int revision = Integer.parseInt(split[1]);
		final boolean inHertz = (type==100 || type==101 || type==103) && revision<10;
		model.addAttribute("inHertz", inHertz);

		Optional.ofNullable(sn)
		.map(String::trim)
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

   									Optional.ofNullable(pairs)
   									.map(Arrays::stream)
   									.orElse(Stream.empty())
   									.filter(p->{
   										final String name = p.getName();
										return name!=null && name.equals("FCM");
   									})
   									.findAny()
   									.map(p->p.getIndex())
   									.ifPresent(
   											index->{

   						   						try {

   						   							// LO Frequency
   						   							final ConverterInfo converterInfo = getHttpDeviceDebug(serialNumber, ConverterInfo.class,
															new BasicNameValuePair("devid", index.toString()),
															new BasicNameValuePair("command", "config")).get(5, TimeUnit.SECONDS);
   						   							logger.debug(converterInfo);

   						   							Optional.ofNullable(converterInfo).map(ConverterInfo::getLoFrequency).map(IrtFrequency::new).flatMap(IrtFrequency::getValue).ifPresent(lo->model.addAttribute("loFrequencty", lo));

   						   						} catch (MalformedURLException e) {
													logger.catching(e);
												} catch (InterruptedException | ExecutionException | TimeoutException e) {
													logger.catching(Level.DEBUG, e);
												}
   											});

   									return null;
   								});
   						ThreadRunner.runThread(taskLoFrequencty);

   	   					FutureTask<Void> ftProfile = new FutureTask<>(()->null);
						final ProfileWorker profileWorker = getProfileWorker(serialNumber, model);

						if(profileWorker!=null)
							ThreadRunner.runThread(
									()->{

										// Get Power table from the profile
										profileWorker.getTable(ProfileTableDetails.OUTPUT_POWER.getDescription()).map(CalibrationTable::getTable).ifPresent(table->model.addAttribute("table", table));

										// Get Power detector source
										final PowerDetectorSource powerDetectorSource = profileWorker.scanForPowerDetectorSource();
										model.addAttribute("jsFunction", powerDetectorSource.getJsFunction());

										ThreadRunner.runThread(ftProfile);
   								});

    					// Get settings from DB
    					final CalibrationPowerOffsetSettings settings = calibrationPowerOffsetSettingRepository.findById(pn).orElseGet(()->new CalibrationPowerOffsetSettings(pn, new BigDecimal(13.75, new MathContext(9)), new BigDecimal(14.5, new MathContext(9)), null));

//    					logger.error("calibrationInfo: {}", calibrationInfo);
    					model.addAttribute("serialNumber", serialNumber);
    					model.addAttribute("settings", settings);

						if(profileWorker!=null)
							ftProfile.get(10, TimeUnit.SECONDS);

						// LO Frequency. Don't stop process on error
   						try {

   							taskLoFrequencty.get(10, TimeUnit.SECONDS);

   						} catch (Exception e) {
							logger.catching(e);
						}

   					} catch (InterruptedException | ExecutionException | TimeoutException e) {
   						logger.catching(Level.DEBUG, e);
					}
    			});

		return "calibration/power_offset :: modal";
    }

	@GetMapping("gain")
    String modalGain(@RequestParam String sn, @RequestParam String pn, Model model) {
//    	logger.error(sn);

		Optional.ofNullable(sn)
		.map(String::trim)
    	.filter(s->!s.isEmpty())
    	.ifPresent(
    			s->{

					model.addAttribute("serialNumber", s);

					FutureTask<Void> ftProfile = gainFromProfile(sn, model);

					try {

    					final Integer devid = getSystemIndex(sn);
    					model.addAttribute("devid", devid);
    					final FutureTask<CalibrationInfo> httpUpdate = getHttpUpdate(

    							s,
    							CalibrationInfo.class,
    							new BasicNameValuePair("exec", "calib_ro_info"));

    					final FutureTask<Dacs> httpDeviceDebug = getHttpDeviceDebug(

    							s,
    							Dacs.class,
    							new BasicNameValuePair("devid", devid.toString()),
    							new BasicNameValuePair("command", "regs"),
    							new BasicNameValuePair("groupindex", "100"));
    					logger.debug(httpDeviceDebug);

    					final CalibrationInfo calibrationInfo = httpUpdate.get(10, TimeUnit.SECONDS);
    					final Dacs dacs = httpDeviceDebug.get(10, TimeUnit.SECONDS);
    					logger.debug("\n\t{}\n\t", calibrationInfo, dacs);

//    					logger.error("dacs: {}", dacs);
    					Optional.ofNullable(calibrationInfo.getBias()).ifPresent(biasBoard->model.addAttribute("temperature", biasBoard.getTemperature()));
    					model.addAttribute("dac2", dacs.getDac2RowValue());

    					// Get settings from DB
    					final CalibrationGainSettings settings = calibrationGainSettingRepository.findById(pn).orElseGet(()->new CalibrationGainSettings(pn, -40, 85));
    					model.addAttribute("settings", settings);

						ftProfile.get(10, TimeUnit.SECONDS);

    				} catch (IOException | ScriptException e) {
						logger.catching(e);
					} catch (InterruptedException | ExecutionException | TimeoutException e) {
						logger.catching(Level.DEBUG, e);
					} 
    			});

    	return "calibration/gain :: modal";
    }

	@GetMapping("current_offset")
    String modalCurrentOffset(@RequestParam String sn, Model model) {
//    	logger.error(sn);

		Optional.ofNullable(sn)
		.map(String::trim)
    	.filter(s->!s.isEmpty())
    	.ifPresent(s->model.addAttribute("serialNumber", s));

    	return "calibration/current_offset :: modal";
    }

	@GetMapping("currents")
    String modalCurrents(@RequestParam String sn, Model model) {

		model.addAttribute("serialNumber", sn);
		final List<IrtArray> urls = arrayRepository.findByIrtArrayIdName("current_url");
		model.addAttribute("URLs", urls);

		return "calibration/currents :: modal";
	}

	@GetMapping("current-set-up")
    String modalCurrentSetUp(@RequestParam String sn, Model model) {

		model.addAttribute("serialNumber", sn);

		return "calibration/currents :: setUp";
	}

	@GetMapping("current")
    String modalCurrent(Integer channel, String pot, String switchName, Model model) {
		logger.traceEntry("channel: {}; pot: {}; switchName: {}", channel, pot, switchName);

		model.addAttribute("channel", channel);
		model.addAttribute("potName", pot);
		model.addAttribute("switchName", switchName);

		return "calibration/currents :: current";
	}

	@GetMapping("current-map")
    String modalCurrentap() {
		logger.traceEntry();
		return "calibration/currents :: map";
	}

    @GetMapping("pll")
    String modalPll(@RequestParam String sn, Model model) {

    	Optional.ofNullable(sn)
		.map(String::trim)
    	.filter(s->!s.isEmpty())
    	.ifPresent(s->{
    		model.addAttribute("serialNumber", s);
    	});

    	return "calibration/pll :: modal";
    }

	private Optional<BtrWorkOrder> createNewWO(String woNumber) {
		Optional<BtrWorkOrder> wo;
		final BtrWorkOrder btrWorkOrder = new BtrWorkOrder();
		btrWorkOrder.setNumber(woNumber);
		wo = Optional.of(workOrderRepository.save(btrWorkOrder));
		logger.debug("Created new WO. ( {} )", woNumber);
		return wo;
	}

	private Optional<XSSFRow> getRowWithSN(InputStream is, String sn) throws IOException {

		try(XSSFWorkbook wb=new XSSFWorkbook(is);){

			XSSFSheet sheet=wb.getSheetAt(0);
			for(int nextToRead = sheet.getLastRowNum(); nextToRead>0; nextToRead--) {

				final XSSFRow row = sheet.getRow(nextToRead);
				if(row==null)
					continue;

				final String cellSN = row.getCell(SERIAL_NUMBER, MissingCellPolicy.RETURN_BLANK_AS_NULL).toString().trim();
				if(!cellSN.isEmpty()) {
					return Optional.of(row);
				}
			}

		}
		return Optional.empty();
	}

	private String createProductionOrderUrl(String sn) {
		return new StringBuilder(protocol).append(login).append(url).append("Document_ProductionOrder?$filter=like(Comment,%27%25").append(sn.trim().replaceAll("\\D", "")).append("%25%27)").toString();
	}

	public static Optional<Monitor> getUnitMonitor(String serialNumber) throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException {
		final MonitorInfo value = getHttpUpdate(serialNumber, MonitorInfo.class, new BasicNameValuePair("exec", "mon_info")).get(5, TimeUnit.SECONDS);
		return Optional.ofNullable(value).map(MonitorInfo::getData);
	}

	public FutureTask<Void> gainFromProfile(String sn, Model model) {
		FutureTask<Void> ftProfile = new FutureTask<>(()->null);

		ThreadRunner.runThread(
				()->{

					// Get 'zero-attenuation-gain' from the profile
					Optional.ofNullable(getProfileWorker(sn, model)).ifPresent(pw->pw.getGain().ifPresent(gain->model.addAttribute("gain", gain)));
			    	ThreadRunner.runThread(ftProfile);
				});
		return ftProfile;
	}

	public FutureTask<AtomicReference<String>> partNumberFromProfile(String sn, Model model) {
		AtomicReference<String> atomicReference = new AtomicReference<>();
		FutureTask<AtomicReference<String>> ftProfile = new FutureTask<>(()->atomicReference);

		ThreadRunner.runThread(
				()->{

					// Get 'zero-attenuation-gain' from the profile
					Optional.ofNullable(getProfileWorker(sn, model)).ifPresent(pw->pw.getPartNumber().ifPresent(atomicReference::set));
			    	ThreadRunner.runThread(ftProfile);
				});
		return ftProfile;
	}

	public FutureTask<AtomicReference<String>> descriptionFromProfile(String sn, Model model) {
		AtomicReference<String> atomicReference = new AtomicReference<>();
		FutureTask<AtomicReference<String>> ftProfile = new FutureTask<>(()->atomicReference);

		ThreadRunner.runThread(
				()->{

					// Get 'zero-attenuation-gain' from the profile
					Optional.ofNullable(getProfileWorker(sn, model)).ifPresent(pw->pw.getDescription().ifPresent(atomicReference::set));
			    	ThreadRunner.runThread(ftProfile);
				});
		return ftProfile;
	}

	@GetMapping("modules_menu")
    String getModulesMenu(@RequestParam String sn, @RequestParam String fragment, Model model) throws IOException, InterruptedException, ExecutionException, TimeoutException, ScriptException {

		model.addAttribute("serialNumber", sn);

		final List<Info> infos = getModulesInfo(sn);

//    	logger.error(infos);
    	model.addAttribute("modules", infos);

    	return "calibration/calibration :: " + fragment;
    }

	@GetMapping("power_chart")
    String modalPowerChart(@RequestParam String sn, Model model) throws IOException, InterruptedException, ExecutionException, TimeoutException {
//		logger.error(sn);

		getHomePageInfo(sn, 100)
		.ifPresent(
				hpi->{
					model.addAttribute("serialNumber", hpi.getSysInfo().getSn());
					model.addAttribute("ip", hpi.getNetInfo().getAddr());
				});

    	return "calibration/power_chart :: modal";
    }

	private List<Info> getModulesInfo(String sn) throws IOException, InterruptedException, ExecutionException, TimeoutException, ScriptException {

		final Map<String, Integer> allDevices = HttpRequest.getAllModules(sn);
    	allDevices.remove("System");

    	return getInfos(sn, allDevices);
	}

	public static List<Info> getInfos(String sn, final Map<String, Integer> allDevices) {
		return allDevices.entrySet().stream()
    			.map(
    					es->{
    						try {

								final Integer moduleIndex = es.getValue();
    							return getInfo(sn, moduleIndex);

    						} catch (MalformedURLException e) {
    							logger.catching(Level.DEBUG, e);
    						} catch (InterruptedException | ExecutionException | TimeoutException e) {
								logger.catching(Level.DEBUG, e);
							}
    						return null;
    					})
    			.filter(info->info!=null).collect(Collectors.toList());
	}

	public static Info getInfo(String sn, final Integer moduleIndex) throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException {

		final URL url = new URL("http", sn, "/device_debug_read.cgi");
		final List<NameValuePair> params = new ArrayList<>();

		params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("devid", moduleIndex.toString()), new BasicNameValuePair("command", "info")}));

		final Info info = HttpRequest.postForIrtObgect(url.toString(), Info.class, params).get(1, TimeUnit.SECONDS);
		logger.debug(info);

		Optional.ofNullable(info).ifPresent(inf->{
			inf.setModuleId(moduleIndex);
			Optional.ofNullable(inf.getDeviceId()).map(s->s.replace("{", "")).map(s->s.split("\\.")).filter(s->s.length==3).map(s->s[0] + '.' + s[1]).ifPresent(info::setDeviceId);
		});
		return info;
	}

	private ProfileWorker getProfileWorker(String sn, Model model) {
		ProfileWorker profileWorker = new ProfileWorker(profileFolder, sn.trim());
		try {
			if(!profileWorker.exists()) {
				model.addAttribute("message", "The profile " + sn + ".bin could not be found.");
				return null;
			}

		} catch (Exception e) {
			logger.catching(e);
			model.addAttribute("message", e.getLocalizedMessage());
			return null;
		}
		return profileWorker;
	}

	public static Optional<HomePageInfo> getHomePageInfo(String sn, int timeout) throws IOException, InterruptedException, ExecutionException, TimeoutException {
		final FutureTask<String> ft = getHonePage(sn, timeout);
		final String honePage = ft.get(timeout, TimeUnit.MILLISECONDS);
		return Optional.ofNullable(honePage).map(HomePageInfo::new);
	}

	public static Integer getSystemIndex(String sn) throws IOException, InterruptedException, ExecutionException, TimeoutException, ScriptException{

		final Map<String, Integer> allDevices = HttpRequest.getAllModules(sn);
		return Optional.ofNullable(allDevices.get("System")).orElse(1);
	}

	public static <T> FutureTask<T> getHttpUpdate(String ipAddress, Class<T> toClass, BasicNameValuePair...basicNameValuePairs) throws MalformedURLException {

		final URL url = new URL("http", ipAddress, "/update.cgi");
		logger.debug("{} {}", url, basicNameValuePairs);

		List<NameValuePair> params = new ArrayList<>();
		params.addAll(Arrays.asList(basicNameValuePairs));

		return HttpRequest.postForIrtObgect(url.toString(), toClass, params);
}

	public static <T> FutureTask<T> getHttpDeviceDebug(String ipAddress, Class<T> toClass, BasicNameValuePair...basicNameValuePairs) throws MalformedURLException{

		final URL url = new URL("http", ipAddress, "/device_debug_read.cgi");
		logger.debug("{} {}", url, basicNameValuePairs);

		List<NameValuePair> params = new ArrayList<>();
		params.addAll(Arrays.asList(basicNameValuePairs));

		return HttpRequest.postForIrtObgect(url.toString(), toClass, params);
	}

	private static FutureTask<String> getHonePage(String ipAddress, int timeout) throws IOException, InterruptedException, ExecutionException, TimeoutException {

		final URL url = new URL("http", ipAddress, "/overview.asp");
		logger.debug(url);

		return HttpRequest.getForString(url.toString(), timeout);
}
}
