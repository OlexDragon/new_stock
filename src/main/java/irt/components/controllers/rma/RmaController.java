package irt.components.controllers.rma;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import irt.components.beans.jpa.User;
import irt.components.beans.jpa.repository.rma.RmaCommentsRepository;
import irt.components.beans.jpa.repository.rma.RmaRepository;
import irt.components.beans.jpa.rma.Rma;
import irt.components.beans.jpa.rma.RmaComment;
import irt.components.services.UserPrincipal;
import irt.components.workers.ProfileWorker;

@Controller
@RequestMapping("rma")
public class RmaController {
	private static final long MARINA_ID = 10L;

	private final static Logger logger = LogManager.getLogger();

	private static final int SIZE = 60;

	@Value("${irt.profile.path}")
	private String profileFolder;

	@Autowired private RmaRepository rmaRepository;
	@Autowired private RmaCommentsRepository rmaCommentsRepository;

	@GetMapping
    String getRmas() {
       return "rma";
    }

	@PostMapping(path = "search")
	public String searchRma(
			@RequestParam(required=false) 	String id,
			@RequestParam(required=false) 	String value,
			@RequestParam				 	String sortBy,
			@RequestParam					RmaFilter rmaFilter,
											Model model) throws IOException {

		logger.traceEntry("id: {}; value: {}; sortBy: {}; rmaFilter:{}", id, value, sortBy, rmaFilter);

		String name = sortBy.replace("rmaOrderBy", "");
		name = name.substring(0, 1).toLowerCase() + name.substring(1);

		final Direction direction = name.equals("rmaNumber") ? Sort.Direction.DESC : Sort.Direction.ASC;
		logger.debug("name: {}; direction: {}", name, direction);
		Sort sort = Sort.by(direction, name);
		List<Rma> rmas = null;
		switch(id) {

		case "rmaNumber":
			rmas = rmaFilter.oStatus
						.map(
								status->{
									switch(status) {

									case IN_WORK:
										return rmaRepository.findByRmaNumberContainingAndStatusNot(value, Rma.Status.SHIPPED, PageRequest.of(0, SIZE, sort));

									default:
										return rmaRepository.findByRmaNumberContainingAndStatus(value, status, PageRequest.of(0, SIZE, sort));
									}
								})
						.orElseGet(()->rmaRepository.findByRmaNumberContaining(value, PageRequest.of(0, SIZE, sort)));
			break;

		case "rmaSerialNumber":
			rmas = rmaFilter.oStatus
						.map(
								status->{
									switch(status) {

									case IN_WORK:
										return rmaRepository.findBySerialNumberContainingAndStatusNot(value, Rma.Status.SHIPPED, PageRequest.of(0, SIZE, sort));

									default:
										return rmaRepository.findBySerialNumberContainingAndStatus(value, status, PageRequest.of(0, SIZE, sort));
									}
								})
						.orElseGet(()->rmaRepository.findBySerialNumberContaining(value, PageRequest.of(0, SIZE, sort)));
			break;

		case "rmaDescription":
			rmas = rmaFilter.oStatus
						.map(
								status->{
									switch(status) {

									case IN_WORK:
										return rmaRepository.findByDescriptionContainingAndStatusNot(value, Rma.Status.SHIPPED, PageRequest.of(0, SIZE, sort));

									default:
										return rmaRepository.findByDescriptionContainingAndStatus(value, status, PageRequest.of(0, SIZE, sort));
									}
								})
						.orElseGet(()->rmaRepository.findByDescriptionContaining(value, PageRequest.of(0, SIZE, sort)));
		}

		logger.debug("RMA list size: {}", rmas.size());
		model.addAttribute("rmas", rmas);

		return "rma :: rmaCards";
	}

	@PostMapping("add_rma")
	public String addRma(@RequestParam String serialNumber, Principal principal, Model model) throws IOException {
//		logger.error("{} : {}", serialNumber, principal);

    	ProfileWorker profileWorker = new ProfileWorker(profileFolder, serialNumber);

		if(!(principal instanceof UsernamePasswordAuthenticationToken) || !profileWorker.exists())
			return null;

		final Optional<Rma> oRma = rmaRepository.findBySerialNumberAndStatusNot(serialNumber, Rma.Status.SHIPPED);
//		logger.error(oRma);
		if(oRma.isPresent())
			return null;

		profileWorker.getDescription()
		.ifPresent(
				description->{
					final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("'RMA'yyMM");
					final LocalDate currentdate = LocalDate.now();
					final String format = currentdate.format(formatter);
					final int count = rmaRepository.findByRmaNumberStartsWith(format).parallelStream().map(Rma::getRmaNumber).map(rmaNumber->rmaNumber.substring(7)).mapToInt(Integer::parseInt).max().orElse(0);
					final String sequence = String.format("%03d", count+1);

					final Rma rma = new Rma();
					rma.setRmaNumber(format + sequence);
					rma.setDescription(description);
					rma.setSerialNumber(serialNumber);

					final Object pr = ((UsernamePasswordAuthenticationToken)principal).getPrincipal();
					final User user = ((UserPrincipal)pr).getUser();
					rma.setUser(user);
					rma.setUserId(user.getId());

					rma.setStatus(Rma.Status.CREATED);
//					logger.error(rma);

					final Rma savedRma = rmaRepository.save(rma);

					List<Rma> rmas = new ArrayList<>();
					rmas.add(savedRma);
					model.addAttribute("rmas", rmas);
				});
		return "rma :: rmaCards";
	}

	@PostMapping(path = "add_comment")
	public String addComment(@RequestParam Long rmaId, @RequestParam String comment, @RequestParam(required = false) Boolean shipped, Principal principal, Model model) throws IOException {
		logger.traceEntry("rmaId: {}; comment: {}; shipped: {}; principal: {};", rmaId, comment, shipped, principal);

		if(!(principal instanceof UsernamePasswordAuthenticationToken) || rmaId==null || comment.isEmpty())
			return "rma :: rmaBody";

		final RmaComment rmaComment = new RmaComment();
		rmaComment.setRmaId(rmaId);
		rmaComment.setComment(comment.trim());

		final Object pr = ((UsernamePasswordAuthenticationToken)principal).getPrincipal();
		final User user = ((UserPrincipal)pr).getUser();
		rmaComment.setUserId(user.getId());

		rmaCommentsRepository.save(rmaComment);
		List<RmaComment> comments = rmaCommentsRepository.findByRmaId(rmaId);
		model.addAttribute("comments", comments);

		final Rma rma = rmaRepository.findById(rmaId).get();
		if(user.getId()!=MARINA_ID) {

			final Optional<Boolean> oShipped = Optional.ofNullable(shipped).filter(s->s);
			if(oShipped.isPresent()){

				model.addAttribute("rmaId", rmaId);
				rma.setStatus(Rma.Status.SHIPPED);
				model.addAttribute("status", Rma.Status.SHIPPED);
				rmaRepository.save(rma);
			}else if(rma.getStatus()==Rma.Status.CREATED){

				rma.setStatus(Rma.Status.IN_WORK);
				rmaRepository.save(rma);
			}
		}

		return "rma :: rmaBody";
	}

	@PostMapping(path = "comments")
	public String getComments(@RequestParam Long rmaId, Model model) throws IOException {
//		logger.error("rmaId: {} ", rmaId );

		rmaRepository.findById(rmaId).ifPresent(rma->model.addAttribute("status", rma.getStatus()));

		final List<RmaComment> comments = rmaCommentsRepository.findByRmaId(rmaId);
		model.addAttribute("comments", comments);
//		logger.error("comments: {} ", comments );

		return "rma :: rmaBody";
	}

	public enum RmaFilter{
		ALL(null),	// Show all RMAs
		SHI(Rma.Status.SHIPPED),	// Show shipped RMAs
		REA(Rma.Status.READY),		// Show RMAs ready to ship
		WOR(Rma.Status.IN_WORK);	// Show RMAs in work

		private Optional<Rma.Status> oStatus;

		RmaFilter(Rma.Status status){
			this.oStatus = Optional.ofNullable(status);
		}
	}
}
