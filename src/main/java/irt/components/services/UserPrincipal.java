package irt.components.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import irt.components.jpa.beans.User;
import irt.components.jpa.beans.User.Status;

public class UserPrincipal implements UserDetails {
	private static final long serialVersionUID = -1600005108091389940L;

	public UserPrincipal(User user) {
		this.user = user;
	}

	private User user;
	public User getUser() { return user; }

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		boolean noPassword = user.getPassword().equals("?");
		if(noPassword) {
			LogManager.getLogger().warn("The user '{}' does not have password.", user.getUsername());
			return new ArrayList<>();
		}

		Collection<? extends GrantedAuthority> authorities = UserRoles.getAuthorities(user.getPermission());

		return authorities;
	}

	@Override
	public String getPassword() {
		return user.getPassword();
	}

	@Override
	public String getUsername() {
		return user.getUsername();
	}

	@Override
	public boolean isAccountNonExpired() {
		return isAccountNonLocked();
	}

	@Override
	public boolean isAccountNonLocked() {
		return user.getStatus()==Status.ACTIVE && Optional.ofNullable(user.getPermission()).filter(p->p>0).isPresent();
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return isAccountNonLocked();
	}

	@Override
	public boolean isEnabled() {
		return isAccountNonLocked();
	}

	@Override
	public String toString() {
		return "UserPrincipal [user=" + user + "]";
	}
}
