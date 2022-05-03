package irt.components.controllers.old;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

import com.mysql.cj.exceptions.WrongArgumentException;

import irt.components.beans.jpa.old.OldComponent;
import irt.components.beans.jpa.old.PnRevision;
import irt.components.beans.jpa.repository.OldComponentRepository;
import irt.components.beans.jpa.repository.PnRevisionRepository;
import irt.components.beans.jpa.repository.pn.PnTypeRepository;

//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

@Controller
@RequestMapping("create")
public class CreateComponentController {
//	private final static Logger logger = LogManager.getLogger();

	private static final Long PCB_CODE = 1l;
	private static final Long MUCHINING_CODE = 2l;
	private static final Long WAVEGUIDE = 3l;

	@Autowired private PnTypeRepository pnTypeRepository;
	@Autowired private OldComponentRepository oldComponentRepository;
	@Autowired private PnRevisionRepository pnRevisionRepository;

	@PostMapping
	public String createComponent(@RequestParam Long pnTypeCode, @RequestParam String newPartNumber, String description, Model model) throws IOException {
//		logger.error("{} : {} : {}", pnTypeCode, newPartNumber, description);

		pnTypeRepository.findById(pnTypeCode)
		.ifPresent(
				pnType->{
					Long nextSeqNumner;
					final List<String> partNumbers = oldComponentRepository.getLastIndex(pnType.getFirst(), pnType.getLast());
//					logger.error("{}", partNumbers);

					if(pnTypeCode==PCB_CODE) {
						nextSeqNumner = getPcbSeqNumber(partNumbers);

					}else if(pnTypeCode==MUCHINING_CODE) {
						nextSeqNumner = getMachiningSeqNumber(partNumbers);

					}else if(pnTypeCode==WAVEGUIDE) {
						nextSeqNumner = getPcbSeqNumber(partNumbers);

					}else

						throw new WrongArgumentException("Unknown Type Code: " + pnTypeCode);
//					logger.error(nextSeqNumner);

					final String nextPN = newPartNumber.replace("#", nextSeqNumner.toString());

					final OldComponent component = new OldComponent();
					component.setPartNumber(nextPN);
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
				});

		return "old :: content";
	}

	private long getPcbSeqNumber(List<String> partNumbers) {
		return partNumbers.parallelStream().map(
				pn->{
					int lastIndexOf = pn.lastIndexOf("-");
					final String substring = pn.substring(++lastIndexOf);
					return Long.parseLong(substring);
				})
				.mapToLong(Long::valueOf).max().orElse(0l) + 1;
	}

	private long getMachiningSeqNumber(List<String> partNumbers) {
		return partNumbers.parallelStream()
				.map(
						pn->{
							int indexOf = pn.indexOf("-");
							return pn.substring(++indexOf);
						})
				.map(pn->Arrays.stream(pn.split("-")).map(s->s.replaceAll("\\D", "")).filter(s->!s.isEmpty()).map(Long::parseLong).findFirst().orElse(0l))
				.mapToLong(Long::valueOf).max().orElse(0l) + 1;
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
