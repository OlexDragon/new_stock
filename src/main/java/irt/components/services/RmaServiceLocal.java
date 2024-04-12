package irt.components.services;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import irt.components.beans.RmaData;
import irt.components.beans.jpa.repository.rma.RmaCommentsRepository;
import irt.components.beans.jpa.repository.rma.RmaRepository;
import irt.components.beans.jpa.rma.Rma;
import irt.components.beans.jpa.rma.Rma.Status;
import irt.components.beans.jpa.rma.RmaComment;
import irt.components.controllers.FileRestController;

@Service
public class RmaServiceLocal implements RmaService {

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

		final Rma rma = rmaRepository.findById(rmaId).get();
		rma.setStatus(status);

		if(rma.getStatus()==status)
			return false;

		rmaRepository.save(rma);
		return true;
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
}
