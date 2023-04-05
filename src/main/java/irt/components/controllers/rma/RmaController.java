package irt.components.controllers.rma;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

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
import irt.components.beans.jpa.rma.Rma.Status;
import irt.components.beans.jpa.rma.RmaComment;
import irt.components.beans.jpa.rma.RmaCountByStatus;
import irt.components.services.UserPrincipal;
import irt.components.workers.IrtPathEncoder;
import irt.components.workers.ProfileWorker;
import javafx.util.Pair;

@Controller
@RequestMapping("rma")
public class RmaController {
	private final static Logger logger = LogManager.getLogger();

	public static final String TEST_PATH_TO_RMA_FILES = "c:\\irt\\rma\\files";

	private static final int SIZE = 1000;

	@Value("${irt.profile.path}") 	private String profileFolder;
	@Value("${irt.rma.files.path}") private String rmaFilesPath;

	@Autowired private RmaRepository rmaRepository;
	@Autowired private RmaCommentsRepository rmaCommentsRepository;

	@PostConstruct
	public void postConstruct() {
        try {

    		// get system name
        	String sName = InetAddress.getLocalHost().getHostName();

        	// Change the directory if server runs on my computer
        	if(sName.equals("oleksandr"))
        		rmaFilesPath = TEST_PATH_TO_RMA_FILES;

        } catch (UnknownHostException e) {
			logger.catching(e);
		}
	}

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

// RMA Number
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

// Serial Number
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

// Description Number
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
			break;

		case "rmaComments":
			rmas = rmaFilter.oStatus
						.map(
								status->{
									switch(status) {

									case IN_WORK:
										return rmaRepository.findDistinctByRmaComments_CommentContainingAndStatusNot(value, Rma.Status.SHIPPED, PageRequest.of(0, SIZE, sort));

									default:
										return rmaRepository.findDistinctByRmaComments_CommentContainingAndStatus(value, status, PageRequest.of(0, SIZE, sort));
									}
								})
						.orElseGet(
								()->{
									return rmaRepository.findDistinctByRmaComments_CommentContaining(value, PageRequest.of(0, SIZE, sort));
								});
		}

		logger.debug("RMA list size: {}", rmas.size());
		model.addAttribute("rmas", rmas);

		final List<RmaCountByStatus> countByStatus = rmaRepository.countByStatus();
		final Long sum = countByStatus.parallelStream().map(RmaCountByStatus::getCount).reduce(0L, Long::sum);
		model.addAttribute("sum", sum);
		final Long ready = countByStatus.parallelStream().filter(r->r.getStatus()==Status.READY).map(RmaCountByStatus::getCount).findAny().orElse(0l);
		model.addAttribute("ready", ready);
		final Long todo = countByStatus.parallelStream().filter(r->r.getStatus()==Status.IN_WORK).map(RmaCountByStatus::getCount).findAny().orElse(0l);
		model.addAttribute("todo", todo);
		final Long waiting = countByStatus.parallelStream().filter(r->r.getStatus()==Status.CREATED).map(RmaCountByStatus::getCount).findAny().orElse(0l);
		model.addAttribute("waiting", waiting);

		return "rma :: rmaCards";
	}

	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("'RMA'yyMM");
	@PostMapping("add_rma")
	public String addRma(@RequestParam String serialNumber, Principal principal, Model model) throws IOException {
//		logger.error("{} : {}", serialNumber, principal);

    	ProfileWorker profileWorker = new ProfileWorker(profileFolder, serialNumber);

		if(!(principal instanceof UsernamePasswordAuthenticationToken) || !profileWorker.exists())
			return "rma :: alert";

		final Optional<Rma> oRma = rmaRepository.findBySerialNumberAndStatusNot(serialNumber, Rma.Status.SHIPPED);
//		logger.error(oRma);
		if(oRma.isPresent())
			return null;

		profileWorker.getDescription()
		.ifPresent(
				description->{
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

					// Part Number
					profileWorker.getPartNumber().ifPresent(rma::setPartNumber);

					final Rma savedRma = rmaRepository.save(rma);

					List<Rma> rmas = new ArrayList<>();
					rmas.add(savedRma);
					model.addAttribute("rmas", rmas);
				});
		return "rma :: rmaCards";
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

	@PostMapping(path = "get_files")
	public String getFiles(@RequestParam Long commentID, Model model) throws IOException {

		model.addAttribute("commentID", commentID);

		fileNames(commentID, model);

		return "rma :: comment_files";
	}

	@PostMapping(path = "show_img")
	public String showImage(@RequestParam Long commentID, @RequestParam Integer imgIndex, Model model) throws IOException {

		model.addAttribute("commentID", commentID);
		model.addAttribute("imgIndex", imgIndex);

		final List<Pair<String, String>> fileNames = fileNames(commentID, model);
		model.addAttribute("imgName", fileNames.get(imgIndex));

		return "rma :: imgModal";
	}

	private List<Pair<String, String>> fileNames(Long commentID, Model model) {

		final File file = Paths.get(rmaFilesPath, commentID.toString()).toFile();

		if(!file.exists())
			return new ArrayList<>();

		final File[] listFiles = file.listFiles();
		final List<Pair<String, String>> fileNames = Arrays.stream(listFiles).filter(f->!f.isDirectory()).filter(f->!f.isHidden()).map(File::getName).map(n->new Pair<String, String>(n, IrtPathEncoder.encode(n))).collect(Collectors.toList());
		model.addAttribute("fileNames", fileNames);
		return fileNames;
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
