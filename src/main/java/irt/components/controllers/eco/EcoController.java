package irt.components.controllers.eco;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import irt.components.beans.jpa.eco.Eco;
import irt.components.beans.jpa.repository.EcoRepository;

@Controller
@RequestMapping("eco")
public class EcoController {
	private final static Logger logger = LogManager.getLogger();

	private static final int SIZE = 100;
	public static final String TEST_PATH_TO_ECO_FILES = "c:\\irt\\eco\\files";

	@Value("${irt.url.protocol}") 			private String protocol;
	@Value("${irt.url.login}") 				private String login;
	@Value("${irt.url}") 					private String url;
	@Value("${irt.url.components.catalog}") private String componentsCatalog;
	@Value("${irt.eco.files.path}") 		private String ecoFilesPath;

	@Autowired private EcoRepository ecoRepository;

	@PostConstruct
	public void postConstruct() {
        try {

    		// get system name
        	String sName = InetAddress.getLocalHost().getHostName();

        	// Change the directory if server runs on my computer
        	if(sName.equals("oleksandr"))
        		ecoFilesPath = TEST_PATH_TO_ECO_FILES;

        } catch (UnknownHostException e) {
			logger.catching(e);
		}
	}

    @GetMapping
    String componentSearch() {
        return "eco/eco";
    }

    @PostMapping
	public String search( @RequestParam(required=false) String id, @RequestParam(required=false) String value, Model model) throws IOException {
		logger.traceEntry("id: {}; value: {}; page: {};", id, value);

		List<Eco> ecos = null;
		switch(id) {
		
		case "ecoNumber":
			ecos = ecoRepository.findByEcoNumberContainingOrderByEcoNumber(value, PageRequest.of(0, SIZE));
			break;

		case "ecoDescription":
			ecos = ecoRepository.findByDescriptionContainingOrderByEcoNumber(value, PageRequest.of(0, SIZE));
			break;

		case "SKU":
			ecos = ecoRepository.findByPartNumberContainingOrderByEcoNumber(value, PageRequest.of(0, SIZE));
			break;

		default:
			return "eco/eco :: ecoCards";
		}

		model.addAttribute("ecos", ecos);

		return "eco/eco :: ecoCards";
	}

	@PostMapping(path = "get_files")
	public String getFiles(@RequestParam Long ecoID, Model model) throws IOException {

		model.addAttribute("ecoID", ecoID);

		fileNames(ecoID, model);

		return "eco/eco :: eco_files";
	}

	@PostMapping(path = "show_img")
	public String showImage(@RequestParam Long ecoID, @RequestParam Integer imgIndex, Model model) throws IOException {

		model.addAttribute("ecoID", ecoID);
		model.addAttribute("imgIndex", imgIndex);

		final List<String> fileNames = fileNames(ecoID, model);
		model.addAttribute("imgName", fileNames.get(imgIndex));

		return "eco/eco :: imgModal";
	}

	private List<String> fileNames(Long ecoID, Model model) {

		final File file = Paths.get(ecoFilesPath, ecoID.toString()).toFile();

		if(!file.exists())
			return new ArrayList<>();

		final File[] listFiles = file.listFiles();
		final List<String> fileNames = Arrays.stream(listFiles).filter(f->!f.isDirectory()).filter(f->!f.isHidden()).map(File::getName).collect(Collectors.toList());
		model.addAttribute("fileNames", fileNames);
		return fileNames;
	}
}
