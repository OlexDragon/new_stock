package irt.components.services;

import static irt.components.controllers.rma.RmaController.onErrorReturn;
import static irt.components.services.SerialNumberScaner.getClientIp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import irt.components.beans.RmaByIDsRequest;
import irt.components.beans.RmaData;
import irt.components.beans.jpa.repository.rma.RmaCommentsWebRepository;
import irt.components.beans.jpa.rma.Comment;
import irt.components.beans.jpa.rma.Rma;
import irt.components.beans.jpa.rma.Rma.Status;
import irt.components.beans.jpa.rma.RmaCommentWeb;
import irt.components.controllers.FileRestController;
import irt.components.controllers.rma.CameraRestController;
import irt.components.controllers.rma.RmaController.RmaFilter;

@Service
public class RmaServiceWeb implements RmaService {
	final static Logger logger = LogManager.getLogger();

	@Value("${irt.onRender}") 					private String onRender;
	@Value("${irt.onRender.rma.change.status}") private String changeStatus;
	@Value("${irt.onRender.rma.by-id}")			private String byId;
	@Value("${irt.onRender.rma.by-ids}")		private String byIds;
	@Value("${irt.onRender.rma.by-status}")		private String byStatus;
	@Value("${irt.onRender.rma.ids.by-status}")	private String idsByStatus;
	@Value("${irt.rma.files.path.web}")			private String rmaFilesPath;
	@Value("${oleksandr.rma.files.path.web}") 	private String oleksandrRmaFilesPath;

	@Autowired private RmaCommentsWebRepository rmaCommentsRepository;

	@PostConstruct
	public void postConstruct() {
        try {

    		// get system name
        	String sName = InetAddress.getLocalHost().getHostName();

        	// Change the directory if server runs on my computer
        	if(sName.equals("oleksandr"))
           		rmaFilesPath	 = oleksandrRmaFilesPath;

        } catch (UnknownHostException e) {
			logger.catching(e);
		}
	}

	@Override
	public Boolean changeStatus(Long rmaId, Rma.Status status) {

		if(status==null)
			return false;

		return getClientIp()
				.map(this::createCientWithClientIp)
				.map(WebClient::post)
				.map(
						wc->wc.uri(uriBuilder -> uriBuilder.path(changeStatus).queryParam("rmaId", rmaId).queryParam("status", status).build())
						.retrieve()
						.bodyToMono(Boolean.class)
						.onErrorReturn(onErrorReturn(new Throwable("RmaServiceWeb.changeStatus.onErrorReturn")), false)
						.block())
				.orElse(false);
	}

	@Override
	public Long addComment(Long rmaId, String comment, Long userId, boolean hasFiles) {

		final RmaCommentWeb rmaComment = new RmaCommentWeb();
		rmaComment.setRmaId(rmaId);
		rmaComment.setUserId(userId);
		rmaComment.setHasFiles(hasFiles);
		rmaComment.setComment(comment);
		return rmaCommentsRepository.save(rmaComment).getId();
	}

	@Override
	public Consumer<? super MultipartFile> saveFile(Long commentId) {
		return FileRestController.saveFile(rmaFilesPath, commentId);
	}

	@Override
	public Optional<RmaData> rmaById(Long id) {
		return Optional.of(
				createWebClient()
				.get()
				.uri(
						builder->builder.path(byId)
						.queryParam("rmaId", id)
						.build())
				.retrieve()
				.toEntity(RmaData.class)
				.onErrorReturn(onErrorReturn(new Throwable("RmaController.rmaById.onErrorReturn")), ResponseEntity.ok(new RmaData()))
				.block()
				.getBody())
				.filter(rd->rd.getId()!=null);
	}

	@Override
	public String getPathToRmaFiles() {
		return rmaFilesPath;
	}

	@Override
	public Optional<Comment> findLastRmaComment(long userId) {
		return rmaCommentsRepository.findTop1ByUserIdOrderByIdDesc(userId);
	}

	@Override
	public List<Comment> findByUserId(long userId) {
		return rmaCommentsRepository.findByUserId(userId).stream().map(Comment.class::cast).collect(Collectors.toList());
	}

	@Override
	public List<RmaData> rmaByStatus(Status status) {

		return createWebClient()

				.get()
				.uri(
						builder->builder.path(byStatus)
						.queryParam("status", status)
						.build())
				.retrieve()
				.onStatus(
						HttpStatus::isError, response -> response.bodyToMono(String.class)
						.map(Exception::new))
				.bodyToFlux(RmaData.class)
				.collectList()
				.block();
	}

	public List<RmaData> rmasByIds(List<Long> rmaIds, int size, Direction direction, RmaFilter rmaFilter, String name){

		final RmaByIDsRequest rmaByIDsRequest = new RmaByIDsRequest();
		rmaByIDsRequest.setRmaIds(rmaIds);
		rmaByIDsRequest.setSize(size);
		rmaByIDsRequest.setDirection(direction);
		rmaByIDsRequest.setStatus(rmaFilter.getStatus());
		rmaByIDsRequest.setName(name);

		final List<RmaData> fromWeb = createWebClient()

				.post().uri(byIds).contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(rmaByIDsRequest))
				.retrieve().onStatus(HttpStatus::isError, response -> response.bodyToMono(String.class).map(Exception::new)).bodyToFlux(RmaData.class).collectList().block();

		return fromWeb;
	}

	protected WebClient createWebClient() {
		return WebClient.builder().baseUrl(onRender).build();
	}

	protected WebClient createCientWithClientIp(String clientIP) {
		return WebClient.builder().baseUrl(onRender).defaultCookie("clientIP", clientIP).build();
	}

	public List<Long> rmaIdsByStatus(Status... status) {

		return createWebClient()

				.get()
				.uri(
						builder->builder.path(idsByStatus)
						.queryParam("status", (Object[])status)
						.build())
				.retrieve()
				.onStatus(
						HttpStatus::isError, response -> response.bodyToMono(String.class)
						.map(Exception::new))
				.bodyToFlux(Long.class)
				.collectList()
				.block();
	}

	@Override
	public void saveImage(Long commentId, byte[] imageByte) throws FileNotFoundException, IOException {
		final long timeMillis = System.currentTimeMillis();
		final String fileName = "image" + timeMillis + ".png";
		final Path path = Paths.get(rmaFilesPath, commentId.toString());
		final File dir = path.toFile();
		if(!dir.exists())
			dir.mkdirs();
		final File file = path.resolve(fileName).toFile();
		logger.debug(file);
		FileRestController.saveImage(file, imageByte);
	}

	@Override
	public Function<byte[], String> saveBytesAsFile(Long commentId) {
		return bytes->{
			try {

				saveImage(commentId, bytes);

			} catch (IOException e) {
				logger.catching(e);
				return e.getLocalizedMessage();
			}
			return CameraRestController.OK;
		};
	}

	@Override
	public void saveComment(Comment comment) {
		rmaCommentsRepository.save((RmaCommentWeb) comment);
	}
}
