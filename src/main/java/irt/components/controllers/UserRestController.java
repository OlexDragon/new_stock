package irt.components.controllers;

import java.util.Base64;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import irt.components.beans.UserRoles;
import irt.components.beans.jpa.User;
import irt.components.beans.jpa.User.Status;
import irt.components.beans.jpa.repository.UserRepository;

@RestController
@RequestMapping("users")
public class UserRestController {
	private final static Logger logger = LogManager.getLogger();

	@Autowired private UserRepository userRepository;

	@PostMapping("save")
	public Boolean editUser(
			@RequestParam(required=false) Long userId,
			@RequestParam String username,
			@RequestParam String password,
			@RequestParam(required=false) String firstname,
			@RequestParam(required=false) String lastname,
			@RequestParam(required=false) String extension,
			@RequestParam(required=false) String email,
			@RequestParam(required=false) User.Status status,
			@RequestParam(name="permission[]") UserRoles[] permission){

		return Optional.ofNullable(userId).map(editUser(username, password, firstname, lastname, extension, email, permission, status))
				.orElseGet(addUser(username, password, firstname, lastname, extension, email, permission, status));
	}

	private Supplier<? extends Boolean> addUser(String username, String password, String firstname, String lastname, String extension, String email, UserRoles[] permission, Status status) {
		return ()->{

			try{

				if(userRepository.findByUsername(username).isPresent())
					return false;

				long roles = UserRoles.toLong(permission);
				final User user = new User(username, encodeToString(password), firstname, lastname, roles, extension, email, status);
				userRepository.save(user);

			}catch (Exception e) {
				logger.catching(e);
				return false;
			}
			return true;
		};
	}

	private Function<Long, Boolean> editUser(String username, String password, String firstname, String lastname, String extension, String email, UserRoles[] userRoles, Status status) {
		return userId->{

			final Optional<User> oUser = userRepository.findById(userId);

			if(!oUser.isPresent())
				return false;

			try{

				final User user = oUser.get();

				user.setEmail(email);
				user.setExtension(extension);
				user.setFirstname(firstname);
				user.setLastname(lastname);
				user.setPermission(UserRoles.toLong(userRoles));
				user.setStatus(status);
				user.setUsername(username);
				user.setPassword(encodeToString(password));

				userRepository.save(user);

			}catch (Exception e) {
				logger.catching(e);
				return false;
			}

			return true;
		};
	}

	private String encodeToString(String password) {
		return Optional.ofNullable(password).map(String::trim).filter(p->p.length()>1).map(p->Base64.getEncoder().encodeToString(p.getBytes())).orElse("?");
	}
}
