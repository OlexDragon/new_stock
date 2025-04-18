package irt.components.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import irt.components.beans.RmaData;
import irt.components.beans.jpa.repository.rma.RmaCommentsRepository;
import irt.components.beans.jpa.repository.rma.RmaRepository;
import irt.components.beans.jpa.rma.Comment;
import irt.components.beans.jpa.rma.Rma;
import irt.components.beans.jpa.rma.Rma.Status;
import irt.components.beans.jpa.rma.RmaComment;
import irt.components.controllers.FileRestController;
import irt.components.controllers.rma.CameraRestController;

@Service
public class RmaServiceLocal implements RmaService {
	final static Logger logger = LogManager.getLogger();

	@Autowired private RmaRepository rmaRepository;
	@Autowired private RmaCommentsRepository rmaCommentsRepository;

	@Value("${oleksandr.rma.files.path}") private String oleksandrFilesPath;
	@Value("${irt.rma.files.path}")	private String rmaFilesPath;

	@PostConstruct
	public void postConstruct() {
        try {

    		// get system name
        	String sName = InetAddress.getLocalHost().getHostName();

        	// Change the directory if server runs on my computer
        	if(sName.equals("oleksandr"))
           		rmaFilesPath	 = oleksandrFilesPath;

        } catch (UnknownHostException e) {
			logger.catching(e);
		}
	}

	@Override
	public Boolean changeStatus(Long rmaId, Rma.Status status) {
		logger.traceEntry("rmaId: {}; status: {};", rmaId, status);

		return rmaRepository.findById(rmaId)
				.map(
						rma->{
							try {

								if(status==null || rma.getStatus()==status)
									return false;

								rma.setStatus(status);

								rmaRepository.save(rma);
								return true;

							} catch (Exception e) {
								logger.catching(e);
								return false;
							}
						})
				.orElse(false);
	}

	@Override
	public Long addComment(Long rmaId, String comment, Long userId, boolean hasFiles) {

		final RmaComment rmaComment = new RmaComment();
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
	public void saveImage(Long commentId, byte[] imageByte) throws FileNotFoundException, IOException {
		final long timeMillis = System.currentTimeMillis();
		final String fileName = "image" + timeMillis + ".png";
		final File file = Paths.get(rmaFilesPath, commentId.toString(), fileName).toFile();
		if(!file.exists())
			file.mkdirs();
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
	public Optional<RmaData> rmaById(Long id) {
		return rmaRepository.findById(id).map(RmaData::new);
	}

	@Override
	public String getPathToRmaFiles() {
		return rmaFilesPath;
	}

	@Override
	public List<RmaData> rmaByStatus(Status status) {
		return rmaRepository.findByStatus(status).stream().map(RmaData::new).collect(Collectors.toList());
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
	public void saveComment(Comment comment) {
		rmaCommentsRepository.save((RmaComment) comment);
	}
}
