package irt.components.controllers.rma;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import irt.components.beans.jpa.User;
import irt.components.beans.jpa.repository.rma.RmaCommentsRepository;
import irt.components.beans.jpa.repository.rma.RmaRepository;
import irt.components.beans.jpa.rma.Rma;
import irt.components.beans.jpa.rma.RmaComment;
import irt.components.services.MailSender;
import irt.components.services.UserPrincipal;
import irt.components.workers.ProfileWorker;
import irt.components.workers.ThreadRunner;

@RestController
@RequestMapping("/rma/rest")
public class RmaRestController {
	private final static Logger logger = LogManager.getLogger();
	private final static long MARINA_ID = 10L;

	@Value("${irt.profile.path}") private String profileFolder;
	@Value("${irt.rma.files.path}") private String rmaFilesPath;

	@Autowired private RmaRepository rmaRepository;
	@Autowired private RmaCommentsRepository rmaCommentsRepository;
	@Autowired private MailSender mailSender;

	@PostConstruct
	public void postConstruct() {

		try {

    		// get system name
        	String sName = InetAddress.getLocalHost().getHostName();

        	// Change the directory if server runs on my computer
        	if(sName.equals("oleksandr"))
        		rmaFilesPath = RmaController.TEST_PATH_TO_RMA_FILES;

        } catch (UnknownHostException e) {
			logger.catching(e);
		}
	}

	@PostMapping("has_prifile")
	public boolean hasProfile(@RequestParam String serialNumber) throws IOException {
		logger.traceEntry(serialNumber);

    	final ProfileWorker profileWorker = new ProfileWorker(profileFolder, serialNumber);
		final String sn = profileWorker.getSerialNumber();
		logger.debug("profileWorker.getSerialNumber() = {}", sn);

		final Optional<Rma> oRma = rmaRepository.findBySerialNumberAndStatusNot(sn, Rma.Status.SHIPPED);

		return profileWorker.exists() && !oRma.isPresent();
	}

	@PostMapping("add_to_rma")
	public String addToRma(@RequestParam String rmaNumber, @RequestParam String serialNumber, Principal principal) throws IOException {
		logger.traceEntry("rmaNumber: {}; serialNumber: {};", rmaNumber, serialNumber);

		final ProfileWorker profileWorker = new ProfileWorker(profileFolder, serialNumber.toUpperCase());
		final boolean exists = profileWorker.exists();

		if(!exists)
			return "The Profile with sn:'" + profileWorker.getSerialNumber() + "' was not found.";

		final String sn = profileWorker.getSerialNumber();
		final Optional<Rma> oRma = rmaRepository.findBySerialNumberAndStatusNot(sn, Rma.Status.SHIPPED);
//		logger.error(oRma);
		if(oRma.isPresent())
			return "The Unit with Serial Number '" + sn + "' exists in the production.";

		return profileWorker.getDescription()
				.map(
						description->{
							RmaController.saveRMA(rmaNumber, description, principal, profileWorker, rmaRepository);
							return "";
						})

				.orElse("Profile scan error.");
	}

	@PostMapping(path = "add_comment", consumes = {"multipart/form-data"})
	public String addComment(
								@RequestParam Long rmaId,
								@RequestParam(required = false) String comment,
								@RequestParam(required = false) Boolean ready,
								@RequestParam(required = false) Boolean shipped,
								@RequestParam(name = "fileToAttach[]", required = false) List<MultipartFile> files,
								Principal principal) throws IOException {


		final Optional<String> oComment = Optional.ofNullable(comment).map(String::trim);
		final Optional<List<MultipartFile>> oFiles = Optional.ofNullable(files).filter(f->!f.isEmpty());

		if(!(principal instanceof UsernamePasswordAuthenticationToken && rmaId!=null && (oComment.isPresent() || ready!=null || shipped !=null || oFiles.isPresent())))
			return "Not all variables are present.";

		final Rma rma = rmaRepository.findById(rmaId).get();

		final Object pr = ((UsernamePasswordAuthenticationToken)principal).getPrincipal();
		final User user = ((UserPrincipal)pr).getUser();

		// Change RMA Status.
		if(shipped!=null && shipped){

			rma.setStatus(Rma.Status.SHIPPED);
			rmaRepository.save(rma);

		}else if(ready!=null && ready){

			rma.setStatus(Rma.Status.READY);
			rmaRepository.save(rma);

		}else if(rma.getStatus()==Rma.Status.CREATED && user.getId()!=MARINA_ID) {

			rma.setStatus(Rma.Status.IN_WORK);
			rmaRepository.save(rma);
		}

		// Save Comment
		final RmaComment rmaComment = new RmaComment();
		rmaComment.setRmaId(rmaId);
		rmaComment.setUserId(user.getId());

		oComment.ifPresent(rmaComment::setComment);
		if(!oComment.isPresent())
			rmaComment.setComment("");

		rmaComment.setHasFiles(oFiles.isPresent());

		final RmaComment savedComment = rmaCommentsRepository.save(rmaComment);

		// Save files
		oFiles.map(List::stream).orElse(Stream.empty()).forEach(saveFile(savedComment.getId()));

		// Send Email
		ThreadRunner.runThread(
				()->{
					synchronized (InetAddress.class) {

						try {

							Thread.sleep(1000);
							String url = "";

							try {

								final InetAddress localHost = InetAddress.getLocalHost();
								final byte[] address = localHost.getAddress();

								url = "\nhttp://" + IntStream.range(0, address.length).mapToObj(index->address[index]&0xff).map(Number::toString).collect(Collectors.joining(".")) + ":8089/rma?rmaNumber=" + rma.getRmaNumber();

							} catch (Exception e) {
								logger.catching(e);
							}

							final String subject = rma.getRmaNumber() + " - " + user.getUsername() + " add new comment";
							mailSender.send(subject, comment + url);

						} catch (Exception e) {
							logger.catching(e);
						}
					}
				});

		return "The comment has been saved.";
	}

	private Consumer<? super MultipartFile> saveFile(Long commentId) {
		return mpFile->{

			if(mpFile.isEmpty())
				return;

			final Path p = Paths.get(rmaFilesPath, commentId.toString());
			p.toFile().mkdirs();	//create a directory
			String originalFilename = mpFile.getOriginalFilename();
			Path path = Paths.get(p.toString(), originalFilename);

			try {

				mpFile.transferTo(path);

			} catch (IllegalStateException | IOException e) {
				logger.catching(e);
			}
		};
	}
}
