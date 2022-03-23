package irt.components.controllers.calibration;

import java.math.BigDecimal;
import java.math.MathContext;
import java.net.MalformedURLException;
import java.net.URL;
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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import irt.components.beans.irt.BiasBoard;
import irt.components.beans.irt.CalibrationInfo;
import irt.components.beans.irt.ConverterInfo;
import irt.components.beans.irt.Dacs;
import irt.components.beans.irt.Info;
import irt.components.beans.irt.IrtFrequency;
import irt.components.beans.irt.IrtValue;
import irt.components.beans.irt.calibration.CalibrationTable;
import irt.components.beans.irt.calibration.PowerDetectorSource;
import irt.components.beans.irt.calibration.ProfileTableDetails;
import irt.components.beans.jpa.CalibrationOutputPowerSettings;
import irt.components.beans.jpa.CalibrationPowerOffsetSettings;
import irt.components.beans.jpa.repository.CalibrationOutputPowerSettingRepository;
import irt.components.beans.jpa.repository.CalibrationPowerOffsetSettingRepository;
import irt.components.workers.HttpRequest;
import irt.components.workers.ProfileWorker;
import irt.components.workers.ThreadRunner;

@Controller
@RequestMapping("/calibration")
public class CalibrationController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.profile.path}")
	private String profileFolder;

	@Autowired CalibrationOutputPowerSettingRepository calibrationOutputPowerSettingRepository;
	@Autowired CalibrationPowerOffsetSettingRepository calibrationPowerOffsetSettingRepository;

	@GetMapping
    String calibration(@RequestParam(required = false) String sn, Model model) {
//    	logger.error(sn);
    	Optional.ofNullable(sn)
    	.filter(s->!s.isEmpty())
    	.ifPresent(
    			s->{
    				try {

    					final Info info = getHttpDeviceDebug(sn, Info.class, new BasicNameValuePair("devid", "1"), new BasicNameValuePair("command", "info")).get(10, TimeUnit.SECONDS);
    					model.addAttribute("info", info);
//    					logger.error(info);

    				} catch (MalformedURLException | InterruptedException | ExecutionException | TimeoutException e) {
						logger.catching(Level.DEBUG, e);
					}
    			});
        return "calibration/calibration";
    }

    @GetMapping("output_power")
    String outputPower(@RequestParam String sn, @RequestParam String pn, Model model) {
//    	logger.error(sn);
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
    					final IrtValue power = Optional.ofNullable(calibrationInfo.getBiasBoard()).map(BiasBoard::getPower).orElseGet(()->new IrtValue());
						model.addAttribute("power", power);

    				} catch (MalformedURLException | InterruptedException | ExecutionException | TimeoutException e) {
						logger.catching(e);
					}
    			});
        return "calibration/output_power :: outputPower";
    }

    @GetMapping("power_offset")
    String powerOffset(@RequestParam String sn, @RequestParam String pn, @RequestParam String deviceId, Model model) {
//    	logger.error("{} : {} : {}", sn, pn, deviceId);

    	final String[] split = deviceId.split("\\.");
    	final int type = Integer.parseInt(split[0]);
    	final int revision = Integer.parseInt(split[1]);
		final boolean inHertz = (type==100 || type==101 || type==103) && revision<10;
		model.addAttribute("inHertz", inHertz);

		Optional.ofNullable(sn)
    	.filter(s->!s.isEmpty())
    	.ifPresent(
    			s->{

   					try {

   						final FutureTask<ConverterInfo> httpDeviceDebug = getHttpDeviceDebug(s, ConverterInfo.class,
								new BasicNameValuePair("devid", "1003"),
								new BasicNameValuePair("command", "config"));

   	   					FutureTask<Void> ftProfile = new FutureTask<>(()->null);

   	    				ThreadRunner.runThread(
   								()->{

   				    				// Get Power table from the profile
   							    	ProfileWorker profileWorker = new ProfileWorker(profileFolder, sn);
   							    	try {
   										if(!profileWorker.exists()) {
   					    					model.addAttribute("message", "The profile could not be found.");
   											ThreadRunner.runThread(ftProfile);
   											return;
   										}

   										profileWorker.getTable(ProfileTableDetails.OUTPUT_POWER.getDescription()).map(CalibrationTable::getTable).ifPresent(table->model.addAttribute("table", table));

   	    						    	// Get Power detector source
   	    						    	final PowerDetectorSource powerDetectorSource = profileWorker.scanForPowerDetectorSource();
   					    				model.addAttribute("jsFunction", powerDetectorSource.getJsFunction());

   								    	ThreadRunner.runThread(ftProfile);

   							    	} catch (Exception e) {
   										logger.catching(e);
   				    					model.addAttribute("message", e.getLocalizedMessage());
   										ThreadRunner.runThread(ftProfile);
   										return;
   									}
   								});

    					final FutureTask<CalibrationInfo> httpUpdate = getHttpUpdate(s, CalibrationInfo.class, new BasicNameValuePair("exec", "calib_ro_info"));
    					// Get settings from DB
    					final CalibrationPowerOffsetSettings settings = calibrationPowerOffsetSettingRepository.findById(pn).orElseGet(()->new CalibrationPowerOffsetSettings(pn, new BigDecimal(13.75, new MathContext(9)), new BigDecimal(14.5, new MathContext(9))));

//    					logger.error("calibrationInfo: {}", calibrationInfo);
    					model.addAttribute("serialNumber", s);
    					model.addAttribute("settings", settings);

    					final CalibrationInfo calibrationInfo = httpUpdate.get(10, TimeUnit.SECONDS);
    					final IrtValue power = Optional.ofNullable(calibrationInfo.getBiasBoard()).map(BiasBoard::getPower).orElseGet(()->new IrtValue());
						model.addAttribute("power", power);

						ftProfile.get(10, TimeUnit.SECONDS);

   						try {

   							final ConverterInfo converterInfo = httpDeviceDebug.get(5, TimeUnit.SECONDS);
//   							logger.error(converterInfo);
   							Optional.ofNullable(converterInfo).map(ConverterInfo::getLoFrequency).map(IrtFrequency::new).flatMap(IrtFrequency::getValue).ifPresent(lo->model.addAttribute("loFrequencty", lo));

   						} catch (Exception e) {
							logger.catching(e);
						}

   					} catch (MalformedURLException | InterruptedException | ExecutionException | TimeoutException e) {
						logger.catching(e);
					}
    			});

		return "calibration/power_offset :: modal";
    }

	@GetMapping("temperature")
    String temperature(@RequestParam String sn, Model model) {
//    	logger.error(sn);

		Optional.ofNullable(sn)
    	.filter(s->!s.isEmpty())
    	.ifPresent(
    			s->{

					model.addAttribute("serialNumber", s);

					FutureTask<Void> ftProfile = new FutureTask<>(()->null);

    				ThreadRunner.runThread(
							()->{

			    				// Get Power table from the profile
						    	ProfileWorker profileWorker = new ProfileWorker(profileFolder, sn);
						    	try {
									if(!profileWorker.exists()) {
				    					model.addAttribute("message", "The profile could not be found.");
										ThreadRunner.runThread(ftProfile);
										return;
									}

									profileWorker.getGain().ifPresent(gain->model.addAttribute("gain", gain));

							    	ThreadRunner.runThread(ftProfile);

						    	} catch (Exception e) {
									logger.catching(e);
			    					model.addAttribute("message", e.getLocalizedMessage());
									ThreadRunner.runThread(ftProfile);
									return;
								}
							});
    				try {

    					final FutureTask<CalibrationInfo> httpUpdate = getHttpUpdate(s, CalibrationInfo.class, new BasicNameValuePair("exec", "calib_ro_info"));
    					final FutureTask<Dacs> httpDeviceDebug = getHttpDeviceDebug(s, Dacs.class,
    							new BasicNameValuePair("devid", "1"),
    							new BasicNameValuePair("command", "regs"),
    							new BasicNameValuePair("groupindex", "100"));

    					final CalibrationInfo calibrationInfo = httpUpdate.get(10, TimeUnit.SECONDS);
    					final Dacs dacs = httpDeviceDebug.get(10, TimeUnit.SECONDS);

//    					logger.error("dacs: {}", dacs);
    					Optional.ofNullable(calibrationInfo.getBiasBoard()).ifPresent(biasBoard->model.addAttribute("temperature", biasBoard.getTemperature()));
    					model.addAttribute("dac2", dacs.getDac2RowValue());

						ftProfile.get(10, TimeUnit.SECONDS);

    				} catch (MalformedURLException | InterruptedException | ExecutionException | TimeoutException e) {
						logger.catching(e);
					}
    			});

    	return "calibration/temperature :: modal";
    }

	public static <T> FutureTask<T> getHttpUpdate(String ipAddress, Class<T> toClass, BasicNameValuePair...basicNameValuePairs) throws MalformedURLException {


		final URL url = new URL("http", ipAddress, "/update.cgi");
		List<NameValuePair> params = new ArrayList<>();
		params.addAll(Arrays.asList(basicNameValuePairs));

		return HttpRequest.postForIrtObgect(url.toString(), toClass, params);
}

	public static <T> FutureTask<T> getHttpDeviceDebug(String ipAddress, Class<T> toClass, BasicNameValuePair...basicNameValuePairs) throws MalformedURLException{

			final URL url = new URL("http", ipAddress, "/device_debug_read.cgi");
//			logger.error(url);
			List<NameValuePair> params = new ArrayList<>();
			params.addAll(Arrays.asList(basicNameValuePairs));

			return HttpRequest.postForIrtObgect(url.toString(), toClass, params);
	}
}
