package irt.components.controllers.calibration;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

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

import irt.components.beans.OneCeHeader;
import irt.components.beans.OneCeUrl;
import irt.components.beans.PartNumber;
import irt.components.beans.SerialNumber;
import irt.components.beans.jpa.btr.BtrMeasurements;
import irt.components.beans.jpa.btr.BtrPowerDetector;
import irt.components.beans.jpa.calibration.CalibrationGainSettings;
import irt.components.beans.jpa.calibration.CalibrationOutputPowerSettings;
import irt.components.beans.jpa.repository.btr.BtrMeasurementsRepository;
import irt.components.beans.jpa.repository.btr.BtrPowerDetectorRepository;
import irt.components.beans.jpa.repository.calibration.CalibrationGainSettingRepository;
import irt.components.beans.jpa.repository.calibration.CalibrationOutputPowerSettingRepository;
import irt.components.workers.HttpRequest;

@Controller
@RequestMapping("calibration/btr")
public class BtrController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.btr.templates}")
	private String templates;

	@Autowired private OneCeUrl oneCeApiUrl;

	@Autowired private BtrMeasurementsRepository measurementsRepository;
	@Autowired private CalibrationGainSettingRepository calibrationGainSettingRepository;
	@Autowired private CalibrationOutputPowerSettingRepository	 calibrationOutputPowerSettingRepository;
	@Autowired private BtrPowerDetectorRepository	 powerDetectorRepository;

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
		logger.traceEntry(sn);

		final FutureTask<SerialNumber> ftSerialNumber = getSerialNumber(sn);
		final OneCeHeader oneCeHeader = OneCeRestController.getOneCHeader(oneCeApiUrl, sn.replaceAll("\\D", "")).get(5, TimeUnit.SECONDS);
		final Optional<OneCeHeader> oOneCeHeader = Optional.ofNullable(oneCeHeader);
		final Optional<String> oProduct = oOneCeHeader.map(OneCeHeader::getProduct);
		final Optional<String> oSalesSKU = oOneCeHeader.map(OneCeHeader::getSalesSKU);
		final Boolean folderExists = oProduct

				.map(pn->new File(templates, pn))
				.map(File::exists)
				.filter(ex->ex)
				.orElseGet(
						()->{
							return oSalesSKU
							.map(pn->new File(templates, pn))
							.map(File::exists)
							.orElse(false);
						});

		final SerialNumber serialNumber = Optional.ofNullable(ftSerialNumber.get(10, TimeUnit.SECONDS))
				.orElseGet(
						()->{
							SerialNumber s = new SerialNumber();
							s.setSerialNumber(sn);
							s.setPartNumber(new PartNumber());
							return s;
						});
		model.addAttribute("sn", serialNumber);
		model.addAttribute("product", oProduct.orElse("N/A"));
		model.addAttribute("salesSKU", oSalesSKU.orElse("N/A"));
		
		logger.debug(serialNumber);

		Optional.ofNullable(serialNumber).filter(s->s.getId()!=null)
		.ifPresent(
				s->{

					final List<BtrMeasurements> measurements = measurementsRepository.findBySerialNumberId(s.getId());
					model.addAttribute("measurements", measurements);

					final String partNumber = s.getPartNumber().getPartNumber();
					final Integer cols = calibrationGainSettingRepository.findById(partNumber).map(CalibrationGainSettings::getFields).orElse(4);
					model.addAttribute("cols", cols);

					model.addAttribute("profileExists", folderExists);
					
				});

		return "calibration/btr_table :: modal";
    }

	@GetMapping("pd")
    String modalPowerDetector(@RequestParam String sn, Model model) {
		logger.traceEntry("sn: {}", sn);

		model.addAttribute("sn", sn);

		final String serialNumber = sn.replaceAll("\\D", "");
		final BtrPowerDetector detector = powerDetectorRepository.findBySerialNumberId(Long.parseLong(serialNumber));
		if(detector!=null) {
			model.addAttribute("pd", detector);

			return "calibration/btr_power_detector :: modal";
		}

		try {
			Optional.ofNullable(OneCeRestController.getOneCHeader(oneCeApiUrl, serialNumber).get(5, TimeUnit.SECONDS))
			.map(
					header->{
						final String salesSKU = header.getSalesSKU();
						final String product = header.getProduct();
						final CalibrationOutputPowerSettings powerSettings = calibrationOutputPowerSettingRepository.findById(salesSKU).orElseGet(()->calibrationOutputPowerSettingRepository.findById(product).orElse(null));

						if(powerSettings==null)
							return null;

						final Integer fieldsNumber = calibrationGainSettingRepository.findById(salesSKU).map(CalibrationGainSettings::getFields)

								.orElseGet(()->calibrationGainSettingRepository.findById(product).map(CalibrationGainSettings::getFields).orElse(4));

						generateIds(powerSettings, fieldsNumber, model);
						return null;
					})
			.orElseGet(
					()->{
						try {

							final SerialNumber serialNumberWeb = getSerialNumber(sn).get(10, TimeUnit.SECONDS);
							final Optional<String> oPartNumber = Optional.ofNullable(serialNumberWeb).map(SerialNumber::getPartNumber).map(PartNumber::getPartNumber);
							oPartNumber.flatMap(calibrationOutputPowerSettingRepository::findById)
							.ifPresent(
									powerSettings->{
										final Integer fieldsNumber = calibrationGainSettingRepository.findById(oPartNumber.get()).map(CalibrationGainSettings::getFields).orElse(4);
										generateIds(powerSettings, fieldsNumber, model);
									});

						} catch (InterruptedException | ExecutionException | TimeoutException e) {
							logger.catching(e);
						}
						return null;
					});
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.catching(Level.DEBUG, e);
		}

		return "calibration/btr_power_detector :: modal";
	}

	public void generateIds(final CalibrationOutputPowerSettings powerSettings, final Integer fieldsNumber,
			Model model) {
		final BtrPowerDetector pd = new BtrPowerDetector();
		model.addAttribute("pd", pd);
		Map<String, Map<String, String>> measurement = new TreeMap<>();
		pd.setMeasurement(measurement);

		final int start = powerSettings.getStartValue();
		final int stop = powerSettings.getStopValue()-1;
		final int step = 3;

		Integer val = 0;
		for(int row=0; val!=stop;){

			val = start + step * row;
			if(val>=stop)
				val = stop;

			final Map<String, String> rowMap = new TreeMap<>();
			measurement.put(val.toString(), rowMap);
			row++;
			for(int col=0; col<fieldsNumber; col++){
				rowMap.put(row + ".pd." + col, "");
			}
		}
	}

	public static FutureTask<SerialNumber> getSerialNumber(String sn) {
		String url = "http://www.irttechnologies.com/rest/serial-number/by-sn?serialNumber=" + sn.toUpperCase();
		final FutureTask<SerialNumber> ft = HttpRequest.getForObgect(url, SerialNumber.class);
		return ft;
	}
}
