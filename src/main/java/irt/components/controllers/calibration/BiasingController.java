package irt.components.controllers.calibration;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

import irt.components.beans.jpa.IrtArray;
import irt.components.beans.jpa.IrtArrayId;
import irt.components.beans.jpa.repository.IrtArrayRepository;
import irt.components.workers.ProfileWorker;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Controller
@RequestMapping("calibration/biasing")
public class BiasingController {
	private final static Logger logger = LogManager.getLogger();

	static final String DEVICE_TYPE = "device-type ";
	static final String DEVICE_REVISION = "device-revision ";
	static final String PROFILE_BIASING_SEQUENCE = "#biasing-sequence ";
	static final String BIASING_SEQUENCE = "biasing-sequence ";

	@Value("${irt.profile.path}")private String profileFolder;
	@Autowired private IrtArrayRepository arrayRepository;

	@GetMapping
    String outputPower(@RequestParam String sn, @RequestParam(required = false) String module, Model model) throws ExecutionException {
    	logger.traceEntry("sn: {}; module: {}", sn, module);
 
    	model.addAttribute("sn", sn);
    	model.addAttribute("module", module);
    	final ProfileWorker profileWorkerSystem = new ProfileWorker(profileFolder, sn);
		try { if(!profileWorkerSystem.exists()) return null; } catch (IOException e) { logger.catching(Level.DEBUG, e); return null; }
		final String systemId = getTypeRev(profileWorkerSystem);

    	final ProfileWorker profileWorker = Optional.ofNullable(module).map(m->new ProfileWorker(profileFolder, m)).orElseGet(()->profileWorkerSystem);
		try { if(!profileWorker.exists()) return null; } catch (IOException e) { logger.catching(Level.DEBUG, e); return null; }

		model.addAttribute("snTitle", profileWorker.getSerialNumber());
		final String modulId = getTypeRev(profileWorker);
		model.addAttribute("modulId", modulId);
		final IrtArrayId irtArrayId = new IrtArrayId(systemId, modulId);
		final Optional<IrtArray> oIrtArray = arrayRepository.findById(irtArrayId);

		final Map<String, String> lines = profileWorker.getLinesStartsWith(BIASING_SEQUENCE, PROFILE_BIASING_SEQUENCE);
		String line = lines.get(BiasingController.BIASING_SEQUENCE);
    	model.addAttribute("biasingLine", line);
		final Optional<String> oValue = getValue(line);
		List<Biasing> biasing = toBiasing(oValue);

		line = lines.get(BiasingController.PROFILE_BIASING_SEQUENCE);
		final Optional<String> oProfileValue = getValue(line);
		model.addAttribute("fromProfile", oProfileValue.isPresent());

		// The Value from DB or Default from the profile
		final Optional<String> oDbValue = oIrtArray.map(IrtArray::getDescription).filter(d->!d.isEmpty())
				// If Exist Default, use it
				.map(d->oProfileValue.orElse(d))
				.map(Optional::of)
				// Save to DB
				.orElseGet(

				()->{
					// If default value is present
					if(!oIrtArray.isPresent()) {

						// Default value from the profile. Default - commented property
						if(oProfileValue.isPresent())
							return Optional.of(saveIrtArray(irtArrayId, oProfileValue.get()));

						// Not commented value
						if(oValue.isPresent())
							return Optional.of(saveIrtArray(irtArrayId, oValue.get()));
					}
					
					return oProfileValue;
				});
		final List<Biasing> dBiasing = toBiasing(oDbValue);
		logger.debug("\n\t{}\n\t{}", dBiasing, biasing);

		biasing.forEach(
					b->{
						int index = dBiasing.indexOf(b);
						if(index<0) {
							dBiasing.add(b);
							b.setNotDefault(true);
						}else
							dBiasing.get(index).setEnable(true);
					});

    	model.addAttribute("biasing", dBiasing);
		return "calibration/biasing_sequence :: modal";
    }

	public static synchronized String getTypeRev(final ProfileWorker profileWorkerSystem) {
		final Map<String, String> linesSystem = profileWorkerSystem.getLinesStartsWith(DEVICE_TYPE, DEVICE_REVISION);
		final String type = Optional.ofNullable(linesSystem.get(DEVICE_TYPE)).flatMap(BiasingController::getValue).orElse("device-type-indefined");
		final String rev = Optional.ofNullable(linesSystem.get(DEVICE_REVISION)).flatMap(BiasingController::getValue).orElse("device-revision-indefined");
		return type + "." + rev;
	}

	private String saveIrtArray(final IrtArrayId irtArrayId, final String defaultValue) {
		final IrtArray irtArray = new IrtArray(irtArrayId, defaultValue);
		final IrtArray saved = arrayRepository.save(irtArray);
		logger.info("Saved sequence: {}", saved);
		return defaultValue;
	}

	private List<Biasing> toBiasing(final Optional<String> value) {
		return value
				.map(sp->sp.split("-"))
				.map(Arrays::stream)
				.orElse(Stream.empty())
				.filter(name->!name.isEmpty())
				.map(Biasing::new)
				.collect(Collectors.toList());
	}

	static Optional<String> getValue(final String line) {
		return Optional.ofNullable(line)

				.map(s->s.split("\\s+", 2))
				.filter(sp->sp.length>1)
				.map(sp->sp[1].split("#", 2))
				.map(sp->sp[0].trim());
	}

    @RequiredArgsConstructor @AllArgsConstructor @Getter @Setter @EqualsAndHashCode @ToString
    public class Biasing{
    	final String name;
    	boolean enable;
    	boolean notDefault;
    }
}
