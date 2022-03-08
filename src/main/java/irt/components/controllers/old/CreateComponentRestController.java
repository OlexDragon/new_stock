package irt.components.controllers.old;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import irt.components.beans.jpa.pn.PnName;
import irt.components.beans.jpa.pn.PnType;
import irt.components.beans.jpa.repository.pn.PnNameRepository;
import irt.components.beans.jpa.repository.pn.PnTypeRepository;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

@RestController
@RequestMapping("create/rest")
public class CreateComponentRestController {
//	private final static Logger logger = LogManager.getLogger();

	@Autowired private PnTypeRepository pnTypeRepository;
	@Autowired private PnNameRepository pnNameRepository;

	@PostMapping(path="get_names", produces = "application/json;charset=utf-8")
	public List<PnName> pnNames(@RequestParam Long pnTypeId) throws IOException {

		Optional<PnType> oPnType = pnTypeRepository.findById(pnTypeId);
		if(!oPnType.isPresent())
			return new ArrayList<>();

		final PnType pnType = oPnType.get();
		return pnNameRepository.findByRange(pnType.getFirst(), pnType.getLast());
	}
}
