package irt.components.controllers.rma;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import irt.components.beans.RmaData;
import irt.components.beans.jpa.repository.UserRepository;
import irt.components.beans.jpa.rma.Comment;
import irt.components.beans.jpa.rma.Rma;
import irt.components.beans.jpa.rma.Rma.Status;
import irt.components.services.RmaService;
import irt.components.services.RmaServiceLocal;
import irt.components.services.RmaServiceWeb;

@RestController
@RequestMapping("rma/")
public class CameraRestController {

	public static final String OK = "Ok";
	private static final String SOMETHING_WENT_WRONG = "Something went wrong.";

	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.profile.path}")	private String profileFolder;

	@Value("${irt.onRender}") 				private String onRender;
	@Value("${irt.onRender.rma.create}")	private String createRma;
	@Value("${irt.onRender.rma.readyToAdd}")private String readyToAdd;

	@Autowired private RmaServiceLocal	local;
	@Autowired private RmaServiceWeb	web;

	@Autowired private UserRepository userRepository;

	@GetMapping(path = "camera/{rmaId}/{userId}/{timestamp}")
	public Map<String, Object> getPmaData(@PathVariable String rmaId, @PathVariable Long userId, @PathVariable Long timestamp) {
		logger.traceEntry("rmaId: {}; userId: {}; timestamp: {}", rmaId, userId, timestamp);

		if((System.currentTimeMillis() - timestamp)/60000 > 30)	// 30 minutes
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The QR code has expired.\nPlease reopen the RMA message.");

		final RmaService rmaService = selectRmaService(rmaId);
		final Map<String, Object> map = new HashMap<>();
		final Long id = Long.parseLong(rmaId.replaceAll("\\D", ""));

		rmaService.rmaById(id)
		 .ifPresent(
				 rma->{
					 map.put("PMA", rma);
					 rmaService.findLastRmaComment(id)
					 .ifPresent(
							 c->{
								final long time = c.getDate().getTime();
								if((System.currentTimeMillis() - time)/60000 < 30)	// 30 minutes
									map.put("last-comment", c);
							 });
					 userRepository.findById(userId)
					 .ifPresent(
							 u->{
								 map.put("User", u.getUsername());
							 });
				 });
		return map;
	}

	@PostMapping(path = "camera/{rmaId}/{userId}/{timestamp}", consumes = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
	public String addCameraSnapshot(@PathVariable String rmaId, @PathVariable Long userId, @PathVariable Long timestamp, @RequestBody byte[] bytes, @RequestHeader("content-disposition") Map<String, String> header) throws IOException{
		logger.traceEntry("rmaId: {}; userId: {}; timestamp: {}\n{}\n{}", rmaId, userId, timestamp, bytes, header);

		if((System.currentTimeMillis() - timestamp)/60000 > 30)	// 30 minutes
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The QR code has expired.\nPlease reopen the RMA message.");

		final RmaService rmaService = selectRmaService(rmaId);
		final long rmaIdLong = Long.parseLong(rmaId.replaceAll("\\D", ""));
		final Optional<Comment> oComment = rmaService.findLastRmaComment(userId).filter(c->addToExistingComment(c, rmaIdLong, timestamp));

		AtomicReference<String> afMmessage = new AtomicReference<>();
		if(oComment.isPresent()) {

			final Comment comment = oComment.get();
			final String message = Optional.of(bytes).map(rmaService.saveBytesAsFile(comment.getId())).orElse(CameraRestController.SOMETHING_WENT_WRONG);
			afMmessage.set(message);

			oComment.filter(c->!c.getHasFiles())
			.ifPresent(
					c->{
						c.setHasFiles(true);
						rmaService.saveComment(c);
					});


		}else {

			final String message = rmaService.rmaById(rmaIdLong)

					.map(
							rma->{

								final Long commentId = rmaService.addComment(rmaIdLong, "Camera Snapshot", userId, true);
								final String m = Optional.of(bytes).map(rmaService.saveBytesAsFile(commentId)).orElse(CameraRestController.SOMETHING_WENT_WRONG);

								if(rma.getStatus()==Rma.Status.CREATED)
									changeRmaStatus(rmaService, rma, Rma.Status.IN_WORK);

								return m;

							}).orElse(CameraRestController.OK);
			afMmessage.set(message);
		}

		final String string = afMmessage.get();
		if(!string.equals(CameraRestController.OK)) 
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, string);

		return string;
	}

	private boolean addToExistingComment(Comment comment, long rmaId, long timestamp) {

		if(comment.getRmaId()!=rmaId)
			return false;

		final long time = comment.getDate().getTime();
		if((System.currentTimeMillis() - time)/60000 > 30)	// 30 minutes
			return false;

		return true;
	}

	public Boolean changeRmaStatus(final RmaService rmaService, RmaData rma, Rma.Status status) {

		final Long rId = rma.getId();
		final Boolean changed = Optional.ofNullable(status).filter(st->st!=rma.getStatus())

				.map(st->rmaService.changeStatus(rId, st))
				.orElseGet(
						()->{

							if(rma.getStatus()==Status.CREATED) {
								return rmaService.changeStatus(rId, Rma.Status.IN_WORK);
							}

							return false;
						});

		if(changed)
			logger.info("The RMA status has been changed to RMA.Status = {}; Request = {}", rma.getStatus());
		else
			logger.info("The RMA status has not changed. RMA.Status = {}; Request = {}", rma.getStatus(), status);

		return changed;
	}

	public RmaService selectRmaService(String rmaId) {
		return rmaId.startsWith("web") ? web : local;
	}
}
