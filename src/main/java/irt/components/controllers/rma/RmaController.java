package irt.components.controllers.rma;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
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
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

@Controller
@RequestMapping("rma")
public class RmaController {
//	private final static Logger logger = LogManager.getLogger();

	private static final int SIZE = 40;

	@Value("${irt.profile.path}")
	private String profileFolder;

	@Autowired private RmaRepository rmaRepository;
	@Autowired private RmaCommentsRepository rmaCommentsRepository;

    @GetMapping
    String getRmas() {

       return "rma";
    }

	@PostMapping(path = "search")
	public String searchBom(
			@RequestParam(required=false) String id,
			@RequestParam(required=false) String value,
											Model model) throws IOException {

		List<Rma> rmas = null;
		switch(id) {

		case "rmaSerialNumber":
			rmas = rmaRepository.findBySerialNumberContainingOrderBySerialNumber(value, PageRequest.of(0, SIZE));
			break;

		case "rmaDescription":
			rmas = rmaRepository.findByDescriptionContainingOrderBySerialNumber(value, PageRequest.of(0, SIZE));
		}
		
		model.addAttribute("rmas", rmas);

		return "rma :: rmaCards";
	}

	@PostMapping("add_rma")
	public String addRma(@RequestParam String serialNumber, Principal principal, Model model) throws IOException {
//		logger.error("{} : {}", serialNumber, principal);

    	ProfileWorker profileWorker = new ProfileWorker(profileFolder, serialNumber);

		if(!(principal instanceof UsernamePasswordAuthenticationToken) || !profileWorker.exists())
			return null;

		profileWorker.getDescription()
		.ifPresent(
				d->{

					final Rma rma = new Rma();
					rma.setDescription(d);
					rma.setSerialNumber(serialNumber);

					final Object pr = ((UsernamePasswordAuthenticationToken)principal).getPrincipal();
					final User user = ((UserPrincipal)pr).getUser();
					rma.setUser(user);
					rma.setUserId(user.getId());

					final Rma savedRma = rmaRepository.save(rma);

					List<Rma> rmas = new ArrayList<>();
					rmas.add(savedRma);
					model.addAttribute("rmas", rmas);
				});
		return "rma :: rmaCards";
	}

	@PostMapping(path = "add_comment")
	public String addComment(@RequestParam Long rmaId, @RequestParam String comment, Principal principal, Model model) throws IOException {

		if(!(principal instanceof UsernamePasswordAuthenticationToken) || rmaId==null || comment.isEmpty())
			return "rma :: rmaBody";

		final RmaComment rmaComment = new RmaComment();
		rmaComment.setRmaId(rmaId);
		rmaComment.setComment(comment.trim());

		final Object pr = ((UsernamePasswordAuthenticationToken)principal).getPrincipal();
		final User user = ((UserPrincipal)pr).getUser();
		rmaComment.setUser(user);
		rmaComment.setUserId(user.getId());

		final RmaComment savedComment = rmaCommentsRepository.save(rmaComment);
		List<RmaComment> comments = new ArrayList<>();
		comments.add(savedComment);
		model.addAttribute("comments", comments);

		return "rma :: rmaBody";
	}

	@PostMapping(path = "comments")
	public String getComments(@RequestParam Long rmaId, Model model) throws IOException {
//		logger.error("rmaId: {} ", rmaId );

		final List<RmaComment> comments = rmaCommentsRepository.findByRmaId(rmaId);
		model.addAttribute("comments", comments);
//		logger.error("comments: {} ", comments );

		return "rma :: rmaBody";
	}
}
