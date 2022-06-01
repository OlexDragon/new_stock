package irt.components.controllers.btr;

import java.io.IOException;
import java.security.Principal;

import javax.security.sasl.AuthenticationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import irt.components.beans.jpa.User;
import irt.components.beans.jpa.btr.BtrComment;
import irt.components.beans.jpa.repository.btr.BtrCommentRepository;
import irt.components.beans.jpa.repository.btr.BtrSerialNumberRepository;
import irt.components.services.UserPrincipal;

//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

@RestController
@RequestMapping("/btr/rest")
public class BtrRestController {
//	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.profile.path}")
	private String profileFolder;

	@Autowired private BtrSerialNumberRepository serialNumberRepository;
	@Autowired private BtrCommentRepository commentRepository;

	@PostMapping("save_comment")
	boolean saveComment(@RequestParam Long snId, @RequestParam String comment, Principal principal) throws IOException {
//		logger.error(serialNumber);

		if(!(principal instanceof UsernamePasswordAuthenticationToken))
			throw new AuthenticationException();

		serialNumberRepository.findById(snId)
		.ifPresent(
				sn->{
					final BtrComment btrComment = new BtrComment();
					btrComment.setComment(comment);
					btrComment.setSerialNumberId(snId);

					final Object pr = ((UsernamePasswordAuthenticationToken)principal).getPrincipal();
					final User user = ((UserPrincipal)pr).getUser();
					btrComment.setUser(user);
					btrComment.setUserId(user.getId());
					
					commentRepository.save(btrComment);
				});

		return true;
	}

	@PostMapping("as_module")
	boolean asModule(@RequestParam Long parentId, @RequestParam Long moduleId, Principal principal) throws IOException {
//		logger.error(serialNumber);

		if(!(principal instanceof UsernamePasswordAuthenticationToken))
			throw new AuthenticationException();

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
}
