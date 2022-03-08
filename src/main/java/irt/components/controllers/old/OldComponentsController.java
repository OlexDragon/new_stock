package irt.components.controllers.old;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import irt.components.beans.jpa.old.OldComponent;
import irt.components.beans.jpa.pn.PnName;
import irt.components.beans.jpa.pn.PnSubtype;
import irt.components.beans.jpa.pn.PnType;
import irt.components.beans.jpa.repository.OldComponentRepository;
import irt.components.beans.jpa.repository.pn.PnNameRepository;
import irt.components.beans.jpa.repository.pn.PnSubtypeRepository;
import irt.components.beans.jpa.repository.pn.PnTypeRepository;

//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

@Controller
@RequestMapping("old")
public class OldComponentsController {
//	private final static Logger logger = LogManager.getLogger();

	@Autowired private PnTypeRepository pnTypeRepository;
	@Autowired private PnNameRepository pnNameRepository;
	@Autowired private PnSubtypeRepository pnSubtypeRepository;
	@Autowired private OldComponentRepository oldComponentRepository;

	@GetMapping
    String oldComponentDB(Model model) {

		final Iterable<PnType> pnTypes = pnTypeRepository.findAll();
		model.addAttribute("pnTypes", pnTypes);

		return "old";
    }

	@PostMapping("get_fields")
	public String getFieldes( @RequestParam Long pnNameCode, Model model) {
//		logger.error(pnNameCode);

		final List<PnSubtype> subtypes = pnSubtypeRepository.findByNameCodeOrderByType(pnNameCode);
		model.addAttribute("subtypes", subtypes);

		String fragment = pnNameRepository.findById(pnNameCode).map(PnName::getFragment).orElse("pcb");
		return "old :: " + fragment;
	}

	@PostMapping("search")
	public String search( @RequestParam(required=false) String id, @RequestParam(required=false) String value, Model model) throws IOException {
//		logger.error("{} : {}", id, value);

		List<OldComponent> components = null;
		Pageable size = PageRequest.of(0, 40);

		value = value.replaceAll("#", "%");
		value = value.replaceAll("\\*", "%");
		if(!value.contains("%"))
			value = "%" + value + "%";
//		logger.error("value: {}", value);

		switch(id) {
		case "componentPN":
			components = oldComponentRepository.findByPartNumberLikeOrderByPartNumber(value, size);
			break;

		case "componentMfrPN":
			components = oldComponentRepository.findByManufPartNumberContainingOrderByPartNumber(value, size);
			break;

		case "description":
			components = oldComponentRepository.findByDescriptionContainingOrderByPartNumber(value, size);
		}

//		logger.error("{}", components);

		model.addAttribute("components", components);

		return "old :: content";
	}
}
