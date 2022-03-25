package irt.components.controllers.rma;

import java.io.IOException;
import java.security.Principal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import irt.components.beans.jpa.User;
import irt.components.beans.jpa.repository.rma.RmaRepository;
import irt.components.beans.jpa.rma.Rma;
import irt.components.services.UserPrincipal;
import irt.components.workers.ProfileWorker;

//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

@RestController
@RequestMapping("/rma/rest")
public class RmaRestController {
//	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.profile.path}")
	private String profileFolder;

	@Autowired private RmaRepository rmaRepository;

	@PostMapping("has_prifile")
	public boolean hasProfile(@RequestParam String serialNumber) throws IOException {
//		logger.error(serialNumber);

    	final ProfileWorker profileWorker = new ProfileWorker(profileFolder, serialNumber);
		final Optional<Rma> oRma = rmaRepository.findBySerialNumberAndShipped(serialNumber, false);

		return profileWorker.exists() && !oRma.isPresent();
	}

	@PostMapping("add_to_rma")
	public String addToRma(@RequestParam String rmaNumber, @RequestParam String serialNumber, Principal principal) throws IOException {
//		logger.error("rmaNumber: {}; serialNumber: {};", rmaNumber, serialNumber);

		final ProfileWorker profileWorker = new ProfileWorker(profileFolder, serialNumber);
		final boolean exists = profileWorker.exists();

		if(!exists)
			return "The Profile with sn:'" + serialNumber + "' was not found.";

		final Optional<Rma> oRma = rmaRepository.findBySerialNumberAndShipped(serialNumber, false);
//		logger.error(oRma);
		if(oRma.isPresent())
			return "The Unit with Serial Number '" + serialNumber + "' exists in the production.";

		return profileWorker.getDescription()
				.map(
						description->{

							final Rma rma = new Rma();
							rma.setRmaNumber(rmaNumber);
							rma.setSerialNumber(serialNumber);
							rma.setDescription(description);

							final Object pr = ((UsernamePasswordAuthenticationToken)principal).getPrincipal();
							final User user = ((UserPrincipal)pr).getUser();
							rma.setUser(user);
							rma.setUserId(user.getId());
//							logger.error(rma);

							rmaRepository.save(rma);
							return "";
						})

				.orElse("Profile scan error.");
	}
}
