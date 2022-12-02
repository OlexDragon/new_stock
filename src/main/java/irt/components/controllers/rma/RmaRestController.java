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
import irt.components.services.UserPrincipal;
import irt.components.workers.ProfileWorker;

@RestController
@RequestMapping("/rma/rest")
public class RmaRestController {
	private final static Logger logger = LogManager.getLogger();
	private final static long MARINA_ID = 10L;

	@Value("${irt.profile.path}") private String profileFolder;
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
        		rmaFilesPath = RmaController.TEST_PATH_TO_RMA_FILES;

        } catch (UnknownHostException e) {
			logger.catching(e);
		}
	}

	@PostMapping("has_prifile")
	public boolean hasProfile(@RequestParam String serialNumber) throws IOException {
//		logger.error(serialNumber);

    	final ProfileWorker profileWorker = new ProfileWorker(profileFolder, serialNumber);
		final Optional<Rma> oRma = rmaRepository.findBySerialNumberAndStatusNot(serialNumber, Rma.Status.SHIPPED);

		return profileWorker.exists() && !oRma.isPresent();
	}

	@PostMapping("add_to_rma")
	public String addToRma(@RequestParam String rmaNumber, @RequestParam String serialNumber, Principal principal) throws IOException {
//		logger.error("rmaNumber: {}; serialNumber: {};", rmaNumber, serialNumber);

		String sn = serialNumber.toUpperCase();
		final ProfileWorker profileWorker = new ProfileWorker(profileFolder, serialNumber);
		final boolean exists = profileWorker.exists();

		if(!exists)
			return "The Profile with sn:'" + sn + "' was not found.";

		final Optional<Rma> oRma = rmaRepository.findBySerialNumberAndStatusNot(sn, Rma.Status.SHIPPED);
//		logger.error(oRma);
		if(oRma.isPresent())
			return "The Unit with Serial Number '" + sn + "' exists in the production.";

		return profileWorker.getDescription()
				.map(
						description->{

							final Rma rma = new Rma();
							rma.setRmaNumber(rmaNumber);
							rma.setSerialNumber(sn);
							rma.setDescription(description);

							final Object pr = ((UsernamePasswordAuthenticationToken)principal).getPrincipal();
							final User user = ((UserPrincipal)pr).getUser();
							rma.setUser(user);
							rma.setUserId(user.getId());
//							logger.error(rma);
							rma.setStatus(Rma.Status.CREATED);

							rmaRepository.save(rma);
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
