package irt.components.controllers.calibration;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import irt.components.workers.ProfileWorker;

@RestController
@RequestMapping("/calibration/biasing/rest")
public class BiasingRestController {
	private final static Logger logger = LogManager.getLogger();

	private static final String PROPERY_NAME = BiasingController.BIASING_SEQUENCE;

	@Value("${irt.profile.path}")
	private String profileFolder;

	@PostMapping("save")
	Boolean saveToProfile(@RequestParam String sn, @RequestParam(name = "values[]", required = false) List<String> values){
		logger.error("sn: {}; values: {}", sn, values);
		final ProfileWorker profileWorker = new ProfileWorker(profileFolder, sn);
		try { if(!profileWorker.exists()) return null; } catch (IOException e) { logger.catching(Level.DEBUG, e); return null; }
		final String value = Optional.ofNullable(values).map(v->v.stream().collect(Collectors.joining("-"))).orElse("");
		return profileWorker.saveProperty(PROPERY_NAME, value);
	}
}
