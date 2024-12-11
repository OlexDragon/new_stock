package irt.components.controllers.calibration;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import irt.components.beans.SerialNumber;
import irt.components.beans.jpa.btr.BtrMeasurements;
import irt.components.beans.jpa.calibration.CalibrationGainSettings;
import irt.components.beans.jpa.repository.btr.BtrMeasurementsRepository;
import irt.components.beans.jpa.repository.calibration.CalibrationGainSettingRepository;
import irt.components.workers.HttpRequest;

@Controller
@RequestMapping("calibration/btr")
public class BtrController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.btr.templates}")
	private String templates;

	@Autowired private BtrMeasurementsRepository measurementsRepository;
	@Autowired private CalibrationGainSettingRepository calibrationGainSettingRepository;

	@PostConstruct
	public void postConstruct() {
        try {

    		// get system name
        	String sName = InetAddress.getLocalHost().getHostName();

        	// Change the directory if server runs on my computer
        	if(sName.equals("oleksandr"))
        		templates = "C:\\irt\\btr\\templates";

        	File folder = new File(templates);
        	if(!folder.exists())
        		folder.mkdirs();

        } catch (UnknownHostException e) {
			logger.catching(e);
		}
	}

	@GetMapping
    String modalBtr(@RequestParam String sn, Model model) throws IOException, InterruptedException, ExecutionException, TimeoutException {

		String url = "http://irttechnologies.com/rest/serial-number/by-sn?serialNumber=" + sn.toUpperCase();
		final FutureTask<SerialNumber> ft = HttpRequest.getForObgect(url, SerialNumber.class);
		final SerialNumber serialNumber = ft.get(3, TimeUnit.SECONDS);
		model.addAttribute("sn", serialNumber);
		logger.debug(serialNumber);

		Optional.ofNullable(serialNumber)
		.ifPresent(
				s->{

					final String partNumber = s.getPartNumber().getPartNumber();
					final List<BtrMeasurements> measurements = measurementsRepository.findBySerialNumberId(s.getId());
					model.addAttribute("measurements", measurements);

					final Integer cols = calibrationGainSettingRepository.findById(partNumber).map(CalibrationGainSettings::getFields).orElse(4);
					model.addAttribute("cols", cols);

					File file = new File(templates, partNumber);
					model.addAttribute("profileExists", file.exists());
					
				});

		return "calibration/btr_table :: modal";
    }

}
