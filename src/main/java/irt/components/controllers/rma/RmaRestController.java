package irt.components.controllers.rma;

import static irt.components.controllers.rma.RmaController.onErrorReturn;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import io.nayuki.qrcodegen.QrCode;
import irt.components.beans.RmaData;
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
			return "The IrtProfile with sn:'" + profileWorker.getSerialNumber() + "' was not found.";

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

				.orElse("IrtProfile scan error.");
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

//	@PostMapping(path = "camera/{rmaId}/{userId}/{timestamp}")
//	public String addCameraSnapshot(@PathVariable String rmaId, @PathVariable Long userId, @PathVariable Long timestamp, @RequestParam String snapshot) throws FileNotFoundException, IOException {
//		logger.traceEntry("rmaId: {}; userId: {}; timestamp: {}\n{}", rmaId, userId, timestamp, snapshot);
//
//		if((System.currentTimeMillis() - timestamp)/60000 > 30)	// 30 minutes
//			return "The QR code has expired.\nPlease reopen the RMA message.";
//
//		final RmaService rmaService = selectRmaService(rmaId);
//		final long rmaIdLong = Long.parseLong(rmaId.replaceAll("\\D", ""));
//		final Optional<Comment> oComment = rmaService.findLastRmaComment(userId).filter(c->addToExistingComment(c, rmaIdLong, timestamp));
//		final byte[] imageByte= Base64.decodeBase64(snapshot.replace("data:image/png;base64,", ""));
//
//		if(oComment.isPresent()) {
//			final Comment comment = oComment.get();
//			rmaService.saveImage(comment.getId(), imageByte);
//
//			oComment.filter(c->!c.getHasFiles())
//			.ifPresent(
//					c->{
//						c.setHasFiles(true);
//						rmaService.saveComment(c);
//					});
//
//		}else 
//
//			return rmaService.rmaById(rmaIdLong)
//
//					.map(
//							rma->{
//
//								final Long commentId = rmaService.addComment(rmaIdLong, "Camera Snapshot", userId, true);
//								try {
//
//									rmaService.saveImage(commentId, imageByte);
//
//									if(rma.getStatus()==Rma.Status.CREATED)
//										changeRmaStatus(rmaService, rma, Rma.Status.IN_WORK);
//
//								} catch (IOException e) {
//									logger.catching(e);
//									return e.getLocalizedMessage();
//								}
//								return null;
//							}).orElse(null);
//		return null;
//	}
//	@PostMapping(path = "camera/fd/{rmaId}/{userId}/{timestamp}", consumes = {"multipart/form-data"})
//	public String addCameraFormData(@PathVariable String rmaId, @PathVariable Long userId, @PathVariable Long timestamp, @RequestParam MultipartFile file) {
//
//		if((System.currentTimeMillis() - timestamp)/60000 > 30)	// 30 minutes
//			return "The QR code has expired.\nPlease reopen the RMA message.";
//
//		final RmaService rmaService = selectRmaService(rmaId);
//		final long rmaIdLong = Long.parseLong(rmaId.replaceAll("\\D", ""));
//		final Optional<Comment> oComment = rmaService.findLastRmaComment(userId).filter(c->addToExistingComment(c, rmaIdLong, timestamp));
//
//		if(oComment.isPresent()) {
//
//			final Comment comment = oComment.get();
//			Optional.of(file).ifPresent(rmaService.saveFile(comment.getId()));
//
//			oComment.filter(c->!c.getHasFiles())
//			.ifPresent(
//					c->{
//						c.setHasFiles(true);
//						rmaService.saveComment(c);
//					});
//
//		}else 
//
//			return rmaService.rmaById(rmaIdLong)
//
//					.map(
//							rma->{
//
//								final Long commentId = rmaService.addComment(rmaIdLong, "Camera Snapshot", userId, true);
//								Optional.of(file).ifPresent(rmaService.saveFile(commentId));
//
//								if(rma.getStatus()==Rma.Status.CREATED)
//									changeRmaStatus(rmaService, rma, Rma.Status.IN_WORK);
//
//								return (String)null;
//
//							}).orElse(null);
//
//		return null;
//	}

//	private boolean addToExistingComment(Comment comment, long rmaId, long timestamp) {
//
//		if(comment.getRmaId()!=rmaId)
//			return false;
//
//		final long time = comment.getDate().getTime();
//		if((System.currentTimeMillis() - time)/60000 > 30)	// 30 minutes
//			return false;
//
//		return true;
//	}

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

		final RmaService rmaService = selectRmaService(rmaId);
		final Long id = Long.parseLong(rmaId.replaceAll("\\D", ""));

		boolean saved = rmaService.rmaById(id)

				.map(
						rma->{

							logger.debug(rma);

							try {

								// Change RMA Status.
								changeRmaStatus(rmaService, rma, status);

								// Save Comment
								final Object pr = ((UsernamePasswordAuthenticationToken)principal).getPrincipal();
								final User user = ((UserPrincipal)pr).getUser();
								final Long userId = user.getId();
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

	@GetMapping("qr")
	ResponseEntity<InputStreamResource> getQR(@RequestParam String rmaId, @RequestParam Long userId) throws IOException {
		logger.traceEntry("rmaId: {}; userId: {}", rmaId, userId);

		long timestamp = System.currentTimeMillis();

		final byte[] bytes = InetAddress.getLocalHost().getAddress();
		final String address = IntStream.range(0, bytes.length).mapToObj(index->bytes[index]&0xff).map(Number::toString).collect(Collectors.joining("."));

		final String url = String.format("http://%s:8089/rma/camera/%s/%d/%d", address, rmaId, userId, timestamp);
		QrCode qr = QrCode.encodeText(url, QrCode.Ecc.MEDIUM);
		BufferedImage img = toImage(qr, 4, 10);

		byte[] byteArray = null;
		try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream();){

			ImageIO.write(img, "png", outputStream);
			byteArray = outputStream.toByteArray();
		}

		HttpHeaders headers = new HttpHeaders();
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");
		headers.add("Content-Disposition", "inline; filename=\"RMA-QR.png\"");

		return ResponseEntity.ok()
				.headers(headers)
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(new InputStreamResource(new ByteArrayInputStream(byteArray)));
	}

	private static BufferedImage toImage(QrCode qr, int scale, int border) {
		return toImage(qr, scale, border, 0xFFFFFF, 0x000000);
	}

	/**
	 * Returns a raster image depicting the specified QR Code, with
	 * the specified module scale, border modules, and module colors.
	 * <p>For example, scale=10 and border=4 means to pad the QR Code with 4 light border
	 * modules on all four sides, and use 10&#xD7;10 pixels to represent each module.
	 * @param qr the QR Code to render (not {@code null})
	 * @param scale the side length (measured in pixels, must be positive) of each module
	 * @param border the number of border modules to add, which must be non-negative
	 * @param lightColor the color to use for light modules, in 0xRRGGBB format
	 * @param darkColor the color to use for dark modules, in 0xRRGGBB format
	 * @return a new image representing the QR Code, with padding and scaling
	 * @throws NullPointerException if the QR Code is {@code null}
	 * @throws IllegalArgumentException if the scale or border is out of range, or if
	 * {scale, border, size} cause the image dimensions to exceed Integer.MAX_VALUE
	 */
	private static BufferedImage toImage(QrCode qr, int scale, int border, int lightColor, int darkColor) {
		Objects.requireNonNull(qr);
		if (scale <= 0 || border < 0)
			throw new IllegalArgumentException("Value out of range");
		if (border > Integer.MAX_VALUE / 2 || qr.size + border * 2L > Integer.MAX_VALUE / scale)
			throw new IllegalArgumentException("Scale or border too large");
		
		BufferedImage result = new BufferedImage((qr.size + border * 2) * scale, (qr.size + border * 2) * scale, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < result.getHeight(); y++) {
			for (int x = 0; x < result.getWidth(); x++) {
				boolean color = qr.getModule(x / scale - border, y / scale - border);
				result.setRGB(x, y, color ? darkColor : lightColor);
			}
		}
		return result;
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
