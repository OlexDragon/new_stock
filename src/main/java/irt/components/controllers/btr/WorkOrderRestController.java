package irt.components.controllers.btr;

import java.io.IOException;
import java.security.Principal;

import javax.security.sasl.AuthenticationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import irt.components.beans.irt.calibration.measurement.MeasurementRequest;
import irt.components.beans.jpa.User;
import irt.components.beans.jpa.btr.BtrComment;
import irt.components.beans.jpa.btr.BtrMeasurements;
import irt.components.beans.jpa.repository.btr.BtrCommentRepository;
import irt.components.beans.jpa.repository.btr.BtrMeasurementsRepository;
import irt.components.beans.jpa.repository.btr.BtrSerialNumberRepository;
import irt.components.services.UserPrincipal;

@RestController
@RequestMapping(path = "/wo/rest", produces = MediaType.APPLICATION_JSON_VALUE)
public class WorkOrderRestController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.profile.path}")
	private String profileFolder;

	@Autowired private BtrSerialNumberRepository serialNumberRepository;
	@Autowired private BtrCommentRepository commentRepository;
	@Autowired private BtrMeasurementsRepository measurementsRepository;

	@PostMapping("save_comment")
	boolean saveComment(@RequestParam Long snId, @RequestParam String comment, Principal principal) throws IOException {

		if(!(principal instanceof UsernamePasswordAuthenticationToken))
			throw new AuthenticationException("Authorization required.");

		serialNumberRepository.findById(snId)
		.ifPresent(
				sn->{
					final BtrComment btrComment = new BtrComment();
					btrComment.setComment(comment);
					btrComment.setSerialNumberId(snId);

					final User user = getUser(principal);
					btrComment.setUser(user);
					btrComment.setUserId(user.getId());
					
					commentRepository.save(btrComment);
				});

		return true;
	}

	@PostMapping("as_module")
	boolean asModule(@RequestParam Long parentId, @RequestParam Long moduleId, Principal principal) throws IOException {

		if(!(principal instanceof UsernamePasswordAuthenticationToken))
			throw new AuthenticationException("Authorization required.");

		if(parentId==moduleId)
			return false;

		serialNumberRepository.findById(moduleId)
		.ifPresent(
				sn->{
					sn.setParentId(parentId);
					serialNumberRepository.save(sn);
				});

		return true;
	}

	@PostMapping("save_measurement")
	boolean saveMeasurement(@RequestBody MeasurementRequest requestBody, Principal principal) throws IOException {
		logger.traceEntry("{}", requestBody);

		if(!(principal instanceof UsernamePasswordAuthenticationToken))
			throw new AuthenticationException("Authorization required.");

		BtrMeasurements measurements = new BtrMeasurements();
		measurements.setSerialNumberId(requestBody.getSerialNumberId());
		measurements.setMeasurement(requestBody.getMeasurement());
		measurements.setUserId(getUser(principal).getId());
		measurementsRepository.save(measurements);

		return true;
	}

	public User getUser(Principal principal) {
		final Object pr = ((UsernamePasswordAuthenticationToken)principal).getPrincipal();
		final User user = ((UserPrincipal)pr).getUser();
		return user;
	}

	  @ExceptionHandler(AuthenticationException.class)
	  public ResponseEntity<String> handleAuthenticationException(AuthenticationException exception) {
	    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
	  }

	  @ExceptionHandler( DataIntegrityViolationException.class)
	  public ResponseEntity<String> handleMysqlDataTruncationException(Exception exception) {

		  Throwable throwable = exception;

		  while(true) {

			  Throwable cause = throwable.getCause();
			  if(cause==null)
				  break;

			  throwable = cause;
		  }

		  return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(throwable.getMessage());
	  }
}
