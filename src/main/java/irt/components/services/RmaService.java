package irt.components.services;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.web.multipart.MultipartFile;

import irt.components.beans.RmaData;
import irt.components.beans.jpa.rma.Rma;
import irt.components.beans.jpa.rma.Rma.Status;

public interface RmaService {

	/**
	 * 
	 * @param rmaId
	 * @param comment
	 * @param userId
	 * @param hasFiles
	 * @return RmaComment ID
	 */
	Long 							addComment(Long rmaId, String comment, Long userId, boolean hasFiles);
	Boolean changeStatus(Long rmaId, Rma.Status status);
	Optional<RmaData> 				rmaById(Long id);
	String 							getPathToRmaFiles();
	Consumer<? super MultipartFile> saveFile(Long commentId);
	List<RmaData> 					rmaByStatus(Status status);

//	final static Logger logger = LogManager.getLogger();
	final static long MARINA_ID = 10L;
	public static Rma.Status determineStatus(Rma.Status currentStatus, Boolean shipped, Boolean ready, Long userId){
//		logger.traceEntry("currentStatus: {}; shipped: {}; ready: {}; userId: {}", currentStatus, shipped, ready, userId);

		final Rma.Status status;

		if(shipped!=null && shipped)
			status = Rma.Status.SHIPPED;

		else if(ready!=null && ready)
			status = Rma.Status.READY;

		else if(currentStatus==Rma.Status.CREATED && userId!=MARINA_ID)
			status = Rma.Status.IN_WORK;

		else
			status = currentStatus;

		return status;
	}
}
