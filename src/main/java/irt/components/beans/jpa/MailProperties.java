package irt.components.beans.jpa;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity @Getter @Setter @ToString
public class MailProperties implements Serializable{
	private static final long serialVersionUID = 693892051918560263L;

	@Id @GeneratedValue
	private Long id;
	private String from;
	private String to;
	private String cc;
	private String password;
}
