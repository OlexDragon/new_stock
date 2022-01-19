package irt.components.calibration;

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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import irt.components.jpa.beans.CalibrationOutputPowerSettings;
import irt.components.jpa.repository.CalibrationOutputPowerSettingRepository;
import irt.components.values.units.BiasBoard;
import irt.components.values.units.CalibrationInfo;
import irt.components.values.units.Dacs;
import irt.components.values.units.Info;
import irt.components.values.units.Value;
import irt.components.workers.HttpRequest;

@Controller
@RequestMapping("/calibration")
public class CalibrationController {
	private final static Logger logger = LogManager.getLogger();

	@Autowired CalibrationOutputPowerSettingRepository calibrationOutputPowerSettingRepository;

	@GetMapping
    String calibration(@RequestParam(required = false) String sn, Model model) {
//    	logger.error(sn);
    	Optional.ofNullable(sn)
    	.ifPresent(
    			s->{
    				try {

    					final Info info = getHttpDeviceDebug(sn, Info.class, new BasicNameValuePair("devid", "1"), new BasicNameValuePair("command", "info")).get(10, TimeUnit.SECONDS);
    					model.addAttribute("info", info);

    				} catch (MalformedURLException | InterruptedException | ExecutionException | TimeoutException e) {
						logger.catching(Level.DEBUG, e);
					}
    			});
        return "calibration";
    }

    @GetMapping("output_power")
    String outputPower(@RequestParam String sn, @RequestParam String pn, Model model) {
//    	logger.error(sn);
    	Optional.ofNullable(sn)
    	.ifPresent(
    			s->{
    				try {

    					final FutureTask<CalibrationInfo> httpUpdate = getHttpUpdate(s, CalibrationInfo.class, new BasicNameValuePair("exec", "calib_ro_info"));
    					final CalibrationOutputPowerSettings settings = calibrationOutputPowerSettingRepository.findById(pn).orElseGet(()->new CalibrationOutputPowerSettings(pn, 30, 46));

//    					logger.error("calibrationInfo: {}", calibrationInfo);
    					model.addAttribute("serialNumber", s);
    					model.addAttribute("settings", settings);

    					final CalibrationInfo calibrationInfo = httpUpdate.get(10, TimeUnit.SECONDS);
    					final Value power = Optional.ofNullable(calibrationInfo.getBiasBoard()).map(BiasBoard::getPower).orElseGet(()->new Value());
						model.addAttribute("power", power);

    				} catch (MalformedURLException | InterruptedException | ExecutionException | TimeoutException e) {
						logger.catching(e);
					}
    			});
        return "calibration/output_power :: outputPower";
    }

	@GetMapping("gain")
    String gain(@RequestParam String sn, Model model) {
//    	logger.error(sn);
    	Optional.ofNullable(sn)
    	.ifPresent(
    			s->{
    				try {

    					final Dacs dacs = getHttpDeviceDebug(s, Dacs.class,
    							new BasicNameValuePair("devid", "1"),
    							new BasicNameValuePair("command", "regs"),
    							new BasicNameValuePair("groupindex", "100"))
    						.get(10, TimeUnit.SECONDS);

//    					logger.error("dacs: {}", dacs);
    					model.addAttribute("dac2", dacs.getDac2RowValue());

    				} catch (MalformedURLException | InterruptedException | ExecutionException | TimeoutException e) {
						logger.catching(e);
					}
    			});
        return "calibration/output_power :: outputPower";
    }

	public static <T> FutureTask<T> getHttpUpdate(String ipAddress, Class<T> toClass, BasicNameValuePair...basicNameValuePairs) throws MalformedURLException {


		final URL url = new URL("http", ipAddress, "/update.cgi");
		List<NameValuePair> params = new ArrayList<>();
		params.addAll(Arrays.asList(basicNameValuePairs));

		return HttpRequest.postForIrtObgect(url.toString(), toClass, params);
}

	public static <T> FutureTask<T> getHttpDeviceDebug(String ipAddress, Class<T> toClass, BasicNameValuePair...basicNameValuePairs) throws MalformedURLException{

			final URL url = new URL("http", ipAddress, "/device_debug_read.cgi");
			List<NameValuePair> params = new ArrayList<>();
			params.addAll(Arrays.asList(basicNameValuePairs));

			return HttpRequest.postForIrtObgect(url.toString(), toClass, params);
	}
}
