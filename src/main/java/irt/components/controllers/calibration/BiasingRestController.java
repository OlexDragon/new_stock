package irt.components.controllers.calibration;

import java.io.IOException;
import java.security.Principal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import irt.components.beans.UserPrincipal;
import irt.components.beans.jpa.IrtArray;
import irt.components.beans.jpa.IrtArrayId;
import irt.components.beans.jpa.User;
import irt.components.beans.jpa.repository.IrtArrayRepository;
import irt.components.workers.ProfileWorker;

@RestController
@RequestMapping("/calibration/biasing/rest")
public class BiasingRestController {
	private final static Logger logger = LogManager.getLogger();

	private static final String PROPERY_NAME = BiasingController.BIASING_SEQUENCE;

	@Value("${irt.profile.path}") private String profileFolder;
	@Autowired private IrtArrayRepository arrayRepository;
	
	private DateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy HH:mm");

	@PostMapping("save")
	Boolean saveToProfile(@RequestParam String sn, @RequestParam(name = "values[]", required = false) List<String> values){
		logger.traceEntry("sn: {}; values: {}", sn, values);

		final ProfileWorker profileWorker = new ProfileWorker(profileFolder, sn);
		try { if(!profileWorker.exists()) return null; } catch (IOException e) { logger.catching(Level.DEBUG, e); return null; }
		final String value = Optional.ofNullable(values).map(v->v.stream().collect(Collectors.joining("-"))).orElse("");

		return profileWorker.saveProperty(PROPERY_NAME, value);
	}

	@PostMapping("to-profile")
	String defaultToProfile(@RequestParam String sn, @RequestParam(name = "values[]", required = false) List<String> values, Principal principal){
		logger.traceEntry("sn: {}; values: {}", sn, values);

		if(!(principal instanceof UsernamePasswordAuthenticationToken))
			return "To change the profile, you must be logged in.";

		final ProfileWorker profileWorker = new ProfileWorker(profileFolder, sn);
		try { if(!profileWorker.exists()) return null; } catch (IOException e) { logger.catching(Level.DEBUG, e); return null; }

		
		final String value = Optional.ofNullable(values).map(v->v.stream().collect(Collectors.joining("-"))).orElse("");
		if(value.isEmpty())
			return profileWorker.removeLine(BiasingController.PROFILE_BIASING_SEQUENCE);

		final Object pr = ((UsernamePasswordAuthenticationToken)principal).getPrincipal();
		final User user = ((UserPrincipal)pr).getUser();
		final Calendar cal = Calendar.getInstance();

		return profileWorker.saveLineBefore(BiasingController.PROFILE_BIASING_SEQUENCE, BiasingController.BIASING_SEQUENCE,  BiasingController.PROFILE_BIASING_SEQUENCE + " " + value + "\t # Added by " + user.getUsername() + " - " + dateFormat.format(cal.getTime()));
	}

	@PostMapping("to-db")
	String defaultToDatabase(@RequestParam String sn, String module, @RequestParam(name = "values[]", required = false) List<String> values, Principal principal){
		logger.traceEntry("sn: {}; module: {}; values: {}", sn, module, values);

		if(!(principal instanceof UsernamePasswordAuthenticationToken))
			return "To change the profile, you must be logged in.";
    	final ProfileWorker profileWorkerSystem = new ProfileWorker(profileFolder, sn);
		try { if(!profileWorkerSystem.exists()) return null; } catch (IOException e) { logger.catching(Level.DEBUG, e); return null; }
		final String systemId = BiasingController.getTypeRev(profileWorkerSystem);

    	final ProfileWorker profileWorker = Optional.ofNullable(module).map(m->new ProfileWorker(profileFolder, m)).orElseGet(()->profileWorkerSystem);
		try { if(!profileWorker.exists()) return null; } catch (IOException e) { logger.catching(Level.DEBUG, e); return null; }
		final String modulId = BiasingController.getTypeRev(profileWorker);

		final IrtArrayId irtArrayId = new IrtArrayId(systemId, modulId);

		final String value = Optional.ofNullable(values).map(v->v.stream().collect(Collectors.joining("-"))).orElse("");
		final IrtArray irtArray = arrayRepository.findById(irtArrayId)
				.map(
						arr->{
							arr.setDescription(value);
							return arr;
						})
				.orElse(new IrtArray(irtArrayId, value));

		final IrtArray saved = arrayRepository.save(irtArray);
		logger.info("Resaved sequence: {}", saved);

		return "Saved: " + saved.getDescription();
	}
}
