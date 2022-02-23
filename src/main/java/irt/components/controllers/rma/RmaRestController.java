package irt.components.controllers.rma;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import irt.components.workers.ProfileWorker;

@RestController
@RequestMapping("/rma/rest")
public class RmaRestController {
//	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.profile.path}")
	private String profileFolder;

	@PostMapping("has_prifile")
	public boolean hasProfile(@RequestParam String serialNumber) throws IOException {
//		logger.error(serialNumber);

    	ProfileWorker profileWorker = new ProfileWorker(profileFolder, serialNumber);

		return profileWorker.exists();
	}
}
