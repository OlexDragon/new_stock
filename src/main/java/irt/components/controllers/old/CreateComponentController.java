package irt.components.controllers.old;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import irt.components.beans.jpa.old.OldComponent;
import irt.components.beans.jpa.old.PnRevision;
import irt.components.beans.jpa.repository.OldComponentRepository;
import irt.components.beans.jpa.repository.PnRevisionRepository;

@Controller
@RequestMapping("/create")
public class CreateComponentController {
//	private final static Logger logger = LogManager.getLogger();

	@Autowired private OldComponentRepository oldComponentRepository;
	@Autowired private PnRevisionRepository pnRevisionRepository;

	@PostMapping
	public String createComponent(@RequestParam String pnNameCode, @RequestParam String pnTypeCode, String description, Model model) throws IOException {
//		logger.error("{} : {}", pn, description);

		final String newIndex = oldComponentRepository.getLastIndex(pnNameCode).map(index->"-" + ++index).orElse("-1");

		final OldComponent component = new OldComponent();
		component.setPartNumber(pnNameCode + '-' + pnTypeCode + '-' + newIndex);
		component.setDescription(description);
		
		final OldComponent savedComponent = oldComponentRepository.save(component);

		final PnRevision pnRevision = new PnRevision(savedComponent.getId(), 1L);
		final PnRevision savedRevision = pnRevisionRepository.save(pnRevision);

		Set<PnRevision> set = new HashSet<>();
		set.add(savedRevision);
		savedComponent.setRevisions(set);

		List<OldComponent> components = new ArrayList<>();
		components.add(savedComponent);
		model.addAttribute("components", components);

		return "old :: content";
	}

	@PostMapping("revision")
	public String addComponentRevision(@RequestParam Long id, Model model) throws IOException {
//		logger.error("id: {} ", id);

		oldComponentRepository.findById(id)
		.ifPresent(
				c->{
//					logger.error(c);
					final Long newRevition = c.getRevisions().stream().max(Comparator.comparingLong(PnRevision::getPnRevision)).map(PnRevision::getPnRevision).map(r->++r).orElse(1L);
					final PnRevision pnRevision = new PnRevision(c.getId(), newRevition);
					final PnRevision savedRevision = pnRevisionRepository.save(pnRevision);
					Set<PnRevision> revisions = c.getRevisions();
					revisions.clear();
					revisions.add(savedRevision);
					List<OldComponent> components = new ArrayList<>();
					components.add(c);
					model.addAttribute("components", components);
				});

		return "old :: content";
	}
}
