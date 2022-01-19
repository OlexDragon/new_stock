package irt.components.jpa.beans;

import java.io.Serializable;
import java.util.Base64;
import java.util.Optional;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="users")
public class User implements Serializable{
	private static final long serialVersionUID = 693892051918560263L;

	@Id @GeneratedValue
	private Long id;
	private String username;
	private String password;
	private String firstname;
	private String lastname;
	private Long permission;
	private String extension;
	private String eMail;
	@Enumerated(EnumType.ORDINAL)
	private Status status;

	protected User(){}
	public User(String username, String password, String firstname, String lastname, Long permission, String extension, String email, Status status) {

		setUsername(username);
		setPassword(password);
		setFirstname(firstname);
		setLastname(lastname);
		this.permission = permission;
		setExtension(extension);
		setEmail(email);
		this.status = status;
	}

	public Long   getId() 			{ return id; }
	public String getUsername() 	{ return username; }
	public String getPassword() 	{ return password; }
	public String getDecodedPassword(){ return Optional.ofNullable(password).filter(p->!p.equals("?")).filter(p->p.length()>1).map(p->new String(Base64.getDecoder().decode(p))).orElse("?"); }
	public String getFirstname() 	{ return firstname; }
	public String getLastname() 	{ return lastname; }
	public Long   getPermission() 	{ return permission; }
	public String getExtension() 	{ return extension; }
	public String getEmail() 		{ return eMail; }
	public Status getStatus() 		{ return status; }

	public void setUsername(String username) 	{ this.username = Optional.ofNullable(username).map(String::trim).filter(un->!un.isEmpty()).orElseThrow(()->new NullPointerException("Username can not be null.")); }
	public void setPassword(String password) 	{ this.password = Optional.ofNullable(password).map(String::trim).filter(un->!un.isEmpty()).orElseThrow(()->new NullPointerException("Password can not be null.")); }
	public void setFirstname(String firstname) 	{ this.firstname = Optional.ofNullable(firstname).map(String::trim).filter(un->!un.isEmpty()).orElse(null); }
	public void setLastname(String lastname) 	{ this.lastname =  Optional.ofNullable(lastname).map(String::trim).filter(un->!un.isEmpty()).orElse(null); }
	public void setPermission(Long permission) 	{ this.permission = permission; }
	public void setExtension(String extension) 	{ this.extension = Optional.ofNullable(extension).map(String::trim).filter(un->!un.isEmpty()).orElse(null); }
	public void setStatus(Status status) 		{ this.status = status; }
	public void setEmail(String email) 			{ this.eMail = Optional.ofNullable(email).map(String::trim).filter(un->!un.isEmpty()).orElse(null); }

	@Override
	public int hashCode() {
		return 31 + ((id == null) ? 0 : id.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", username=" + username + ", password=" + password + ", firstname=" + firstname
				+ ", lastname=" + lastname + ", permission=" + permission + ", extension=" + extension + ", eMail="
				+ eMail + ", status=" + status + "]";
	}

	public enum Status{
		INACTIVE,
		ACTIVE
	}
}
