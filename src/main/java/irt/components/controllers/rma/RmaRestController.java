package irt.components.controllers.rma;

import static irt.components.controllers.rma.RmaController.onErrorReturn;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.annotation.JsonValue;

import irt.components.beans.RmaRequest;
import irt.components.beans.UserPrincipal;
import irt.components.beans.jpa.User;
import irt.components.beans.jpa.repository.rma.RmaRepository;
import irt.components.beans.jpa.rma.Rma;
import irt.components.beans.jpa.rma.Rma.Status;
import irt.components.services.MailSender;
import irt.components.services.RmaService;
import irt.components.services.RmaServiceLocal;
import irt.components.services.RmaServiceWeb;
import irt.components.workers.ProfileWorker;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@RestController
@RequestMapping("rma/rest")
public class RmaRestController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.profile.path}")	private String profileFolder;

	@Value("${irt.onRender}") 				private String onRender;
	@Value("${irt.onRender.rma.create}")	private String createRma;
	@Value("${irt.onRender.rma.readyToAdd}")private String readyToAdd;

	@Autowired private RmaServiceLocal	local;
	@Autowired private RmaServiceWeb	web;

	@Autowired private RmaRepository rmaRepository;
	@Autowired private MailSender mailSender;

	@GetMapping("ready-to-add")
	public boolean readyToAdd(@RequestParam String sn){
		logger.traceEntry(sn);

		if(sn.replaceAll("\\D", "").length()!=7)
			return false;

		final Boolean onRenderReady = WebClient.builder().baseUrl(onRender).build().get().uri(uriBuilder -> uriBuilder.path(readyToAdd).queryParam("sn", sn).build()).retrieve()
				.bodyToMono(Boolean.class)
				.onErrorReturn(onErrorReturn(new Throwable("RmaRestController.snExists.onErrorReturn")), false)
				.block();
		// If onRender is not ready check local DB.
		if(!onRenderReady)
			return false;

		if(sn.length()==7)
			return !rmaRepository.existsBySerialNumberEndsWithAndStatusNotIn(sn, Status.SHIPPED, Status.CLOSED);

		return !rmaRepository.existsBySerialNumberAndStatusNotIn(sn, Status.SHIPPED, Status.CLOSED);
	}

	@PostMapping("add_to_rma")
	public String addToRma(@RequestParam String rmaNumber, @RequestParam String serialNumber, Principal principal) throws IOException {
		logger.traceEntry("rmaNumber: {}; serialNumber: {};", rmaNumber, serialNumber);

		final ProfileWorker profileWorker = new ProfileWorker(profileFolder, serialNumber.toUpperCase());
		final boolean exists = profileWorker.exists();

		if(!exists)
			return "The Profile with sn:'" + profileWorker.getSerialNumber() + "' was not found.";

		final String sn = profileWorker.getSerialNumber();
		final List<Rma> oRma = rmaRepository.findBySerialNumberAndStatusNotIn(sn, Rma.Status.SHIPPED);
//		logger.error(oRma);
		if(!oRma.isEmpty())
			return "The Unit with Serial Number '" + sn + "' exists in the production.";

		return profileWorker.getDescription()
				.map(
						description->{
							RmaController.saveRMA(rmaNumber, description, principal, profileWorker, rmaRepository);
							return "";
						})

				.orElse("Profile scan error.");
	}

	@PostMapping("add_rma")
	public ResponseMessage addRma(@CookieValue(required = false) String clientIP, @RequestParam String serialNumber, String cause, Principal principal) throws IOException {
		logger.traceEntry("{} : {}", serialNumber, principal);

		if(!(principal instanceof UsernamePasswordAuthenticationToken))
			return new ResponseMessage("Your login has expired. Please refresh the page and <strong>login again.</strong>", BootstapClass.TXT_BG_WARNING);

		final RmaRequest rmaRequest = new RmaRequest();
		rmaRequest.setSn(serialNumber);
		rmaRequest.setCause(cause);
		final User user = ((UserPrincipal)((UsernamePasswordAuthenticationToken)principal).getPrincipal()).getUser();
		rmaRequest.setEmail(user.getEmail());
		rmaRequest.setName("IRT User Id " + user.getId());

		return WebClient.builder().baseUrl(onRender).defaultCookie("clientIP", clientIP).defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build()

				.post().uri(createRma).body(BodyInserters.fromValue(rmaRequest)).retrieve().toEntity(ResponseMessage.class).block().getBody();
	}

	@PostMapping(path = "add_comment", consumes = {"multipart/form-data"})
	public String addComment(
								@RequestParam String rmaId,
								@RequestParam(required = false) String comment,
								@RequestParam(required = false) Rma.Status status,
								@RequestParam(name = "fileToAttach[]", required = false) List<MultipartFile> files,
								Principal principal) throws IOException {

		logger.traceEntry("rmaId: {}; status: {};\n\tcomment:\n{}", rmaId, status, comment);


		final Optional<String> oComment = Optional.ofNullable(comment).map(String::trim);
		final Optional<List<MultipartFile>> oFiles = Optional.ofNullable(files).filter(f->!f.isEmpty());

		if(!(principal instanceof UsernamePasswordAuthenticationToken && rmaId!=null && (oComment.isPresent() || oFiles.isPresent())))
			return "Not all variables are present.";

		final RmaService rmaService = rmaId.startsWith("web") ? web : local;
		final Long id = Long.parseLong(rmaId.replaceAll("\\D", ""));

		boolean saved = rmaService.rmaById(id)

				.map(
						rma->{

							logger.debug(rma);

							try {


								final Object pr = ((UsernamePasswordAuthenticationToken)principal).getPrincipal();
								final User user = ((UserPrincipal)pr).getUser();
								final Long userId = user.getId();

								// Change RMA Status.
								final Boolean changed = Optional.ofNullable(status).filter(st->st!=rma.getStatus())

										.map(st->rmaService.changeStatus(id, st))
										.orElseGet(
												()->{

													if(rma.getStatus()==Status.CREATED)
														return rmaService.changeStatus(id, Rma.Status.IN_WORK);

													return false;
												});
								if(!changed)
									logger.info("The RMA status has not changed. RMA.Status = {}; Request = {}", rma.getStatus(), status);

								// Save Comment
								final String c = oComment.orElse("");
								final Long commentId = rmaService.addComment(id, c, userId, oFiles.isPresent());

								final String subject = rma.getRmaNumber() + " - " + user.getUsername() + " add new comment";
								mailSender.send(subject, c, id, rmaService==web);

								// Save files
								oFiles.map(List::stream).orElse(Stream.empty()).forEach(rmaService.saveFile(commentId));

								return true;

							} catch (Exception e) {
								logger.catching(e);
								return false;
							}
						})
				.orElseGet(
						()->{
							logger.warn("No RMA with ID {}", rmaId);
							return false;
						});

		if(saved)
			return "The comment has been saved.";
		else
			return "The comment was not saved. Check logs.";
	}

	@NoArgsConstructor @AllArgsConstructor @Getter @ToString
	public static class ResponseMessage{
		private String message;
		private BootstapClass cssClass;
	}

	@AllArgsConstructor @Getter @ToString
	public enum BootstapClass{

		TXT_BG_DANGER("text-bg-danger"),
		TXT_BG_SUCCESS("text-bg-success"),
		TXT_BG_PRIMARY("text-bg-primary"),
		TXT_BG_BLACK("text-bg-black"),
		TXT_BG_WARNING("text-bg-warning");

		@JsonValue
		private String value;
	}
}
