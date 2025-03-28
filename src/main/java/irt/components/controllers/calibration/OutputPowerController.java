package irt.components.controllers.calibration;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import irt.components.beans.jpa.calibration.CalibrationOutputPowerSettings;
import irt.components.beans.jpa.repository.calibration.CalibrationOutputPowerSettingRepository;

@Controller
@RequestMapping("calibration/op")
public class OutputPowerController {
	private final static Logger logger = LogManager.getLogger();

	@Autowired private CalibrationOutputPowerSettingRepository	 calibrationOutputPowerSettingRepository;

    @GetMapping
    String outputPower(@RequestParam String sn, @RequestParam String pn, Model model) throws ExecutionException {
    	logger.traceEntry("sn: {}; pn: {};", sn, pn);

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

    @GetMapping("by_input")
    String outputPowerByInput(@RequestParam String sn, @RequestParam String pn, Model model) throws ExecutionException {

    	outputPower(sn, pn, model);
    	model.addAttribute("byInput", true);

    	return "calibration/output_power_auto :: opAuto";
    }

    @GetMapping("by_gain")
    String outputPowerByGain(@RequestParam String sn, @RequestParam String pn, Model model) throws ExecutionException {

    	outputPower(sn, pn, model);
    	model.addAttribute("byInput", false);

    	return "calibration/output_power_auto :: opAuto";
    }
}
