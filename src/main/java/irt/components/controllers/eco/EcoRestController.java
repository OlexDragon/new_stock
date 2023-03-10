package irt.components.controllers.eco;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import irt.components.beans.jpa.User;
import irt.components.beans.jpa.eco.Eco;
import irt.components.beans.jpa.eco.Eco.Status;
import irt.components.beans.jpa.repository.EcoRepository;
import irt.components.services.UserPrincipal;

@RestController
@RequestMapping("/eco/rest")
public class EcoRestController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.eco.files.path}") private String ecoFilesPath;

	@Autowired private EcoRepository ecoRepository;

	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("'ECO'yyMM");

	@PostConstruct
	public void postConstruct() {
        try {

    		// get system name
        	String sName = InetAddress.getLocalHost().getHostName();

        	// Change the directory if server runs on my computer
        	if(sName.equals("oleksandr"))
        		ecoFilesPath = EcoController.TEST_PATH_TO_ECO_FILES;

        } catch (UnknownHostException e) {
			logger.catching(e);
		}
	}

	@PostMapping(path = "add_eco", consumes = {"multipart/form-data"})
	public String addEco(
								@RequestParam String partNumber,
								@RequestParam String ecoCause,
								@RequestParam String ecoBody,
								@RequestParam(name = "fileToAttach[]", required = false) List<MultipartFile> files,
								Principal principal) throws IOException {


		final Optional<List<MultipartFile>> oFiles = Optional.ofNullable(files).filter(f->!f.isEmpty());

		if(!(principal instanceof UsernamePasswordAuthenticationToken) && partNumber.trim().isEmpty() && ecoCause.trim().isEmpty() && ecoBody.trim().isEmpty())
			return "Not all variables are present.";

		final LocalDate currentdate = LocalDate.now();
		final String format = currentdate.format(formatter);
		final int count = ecoRepository.findByEcoNumberStartsWith(format).parallelStream().map(Eco::getEcoNumber).map(ecoNumber->ecoNumber.substring(7)).mapToInt(Integer::parseInt).max().orElse(0);
		final String sequence = String.format("%03d", count+1);

		final Object pr = ((UsernamePasswordAuthenticationToken)principal).getPrincipal();
		final User user = ((UserPrincipal)pr).getUser();

		final Eco eco = new Eco(format + sequence, partNumber, ecoCause, ecoBody, user.getId());

		eco.setHasFiles(oFiles.isPresent());

		final Eco savedEco = ecoRepository.save(eco);

		// Save files
		oFiles.map(List::stream).orElse(Stream.empty()).forEach(saveFile(savedEco.getId()));

		return "The " + savedEco.getEcoNumber() +  " has been saved.";
	}

	@PostMapping(path = "edit_eco", consumes = {"multipart/form-data"})
	public String editEco(
								@RequestParam Long ecoID,
								@RequestParam String ecoCause,
								@RequestParam String ecoBody,
								@RequestParam(name = "fileToAttach[]", required = false) List<MultipartFile> files,
								Principal principal) throws IOException {



		if(!(principal instanceof UsernamePasswordAuthenticationToken)  && ecoCause.trim().isEmpty() && ecoBody.trim().isEmpty())
			return "Not all variables are present.";

		final Object pr = ((UsernamePasswordAuthenticationToken)principal).getPrincipal();
		final User user = ((UserPrincipal)pr).getUser();

		ecoRepository.findById(ecoID)
		.ifPresent(
				eco->{

					final Eco newEco = new Eco(eco.getEcoNumber(), eco.getPartNumber(), ecoCause, ecoBody, user.getId());

					final Optional<List<MultipartFile>> oFiles = Optional.ofNullable(files).filter(f->!f.isEmpty());
					newEco.setHasFiles(oFiles.isPresent());

					final Integer version = eco.getVersion();
					newEco.setVersion(version+1);

					final Eco savedEco = ecoRepository.save(newEco);

					// Save files
					oFiles.map(List::stream).orElse(Stream.empty()).forEach(saveFile(savedEco.getId()));

					eco.setStatus(Status.CLOSED);
					ecoRepository.save(eco);
				});

		return "The ECO has been saved.";
	}

	private Consumer<? super MultipartFile> saveFile(Long ecoID) {
		return mpFile->{

			if(mpFile.isEmpty())
				return;

			final Path p = Paths.get(ecoFilesPath, ecoID.toString());
			p.toFile().mkdirs();	//create a directory
			String originalFilename = mpFile.getOriginalFilename();
			Path path = Paths.get(p.toString(), originalFilename);

			try {

				mpFile.transferTo(path);

			} catch (IllegalStateException | IOException e) {
				logger.catching(e);
			}
		};
	}
}
