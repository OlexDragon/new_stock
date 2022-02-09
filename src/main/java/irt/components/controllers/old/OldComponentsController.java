package irt.components.controllers.old;

import java.io.IOException;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import irt.components.beans.jpa.old.OldComponent;
import irt.components.beans.jpa.pn.PnName;
import irt.components.beans.jpa.repository.OldComponentRepository;
import irt.components.beans.jpa.repository.pn.PnNameRepository;

@Controller
@RequestMapping("old")
public class OldComponentsController {
//	private final static Logger logger = LogManager.getLogger();

	@Autowired private PnNameRepository pnNameRepository;
	@Autowired private OldComponentRepository oldComponentRepository;

	@RequestMapping
	@Transactional
    String componentSearch(Model model) {

		final Iterable<PnName> pnNames = pnNameRepository.findAll();
		model.addAttribute("pnNames", pnNames);

		return "old";
    }

	@PostMapping("search")
	public String search( @RequestParam(required=false) String id, @RequestParam(required=false) String value, Model model) throws IOException {
//		logger.error("{} : {}", id, value);

		List<OldComponent> components = null;
		Pageable size = PageRequest.of(0, 40);

		switch(id) {
		case "componentPN":
			components = oldComponentRepository.findByPartNumberContainingOrderByPartNumber(value, size);
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
