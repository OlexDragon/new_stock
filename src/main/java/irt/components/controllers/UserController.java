package irt.components.controllers;

import java.util.Arrays;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import irt.components.beans.UserRoles;
import irt.components.beans.jpa.User;
import irt.components.beans.jpa.User.Status;
import irt.components.beans.jpa.repository.UserRepository;

@Controller
@RequestMapping("users")
public class UserController {
//	private final static Logger logger = LogManager.getLogger();

	@Autowired private UserRepository userRepository;

	@RequestMapping
	@Transactional
	public String userList(Model model){
		final Iterable<User> findAll = userRepository.findAllByOrderByStatusDescFirstnameAsc();
		model.addAttribute("users", findAll);
		model.addAttribute("activeUser", Status.ACTIVE);
		return "user";
	}

	@RequestMapping("edit")
	public String editUser(@RequestParam(required=false) Long userId, Model model){

		final UserRoles[] roles = UserRoles.values();
		Arrays.sort(roles, (a, b)->a.name().compareTo(b.name()));
		model.addAttribute( "roles", roles);
		model.addAttribute("activeUser", Status.ACTIVE);
		model.addAttribute("statusValues", Status.values());

		Optional.ofNullable(userId).flatMap(userRepository::findById).ifPresent(u->model.addAttribute( "user", u));

		return "user_edit :: edit_user";
	}
}
