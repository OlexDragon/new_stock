package irt.components.beans.jpa;

import java.io.Serializable;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name="arrays")
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString
public class IrtArray implements Serializable{
	private static final long serialVersionUID = 4599250167086381300L;

	@EmbeddedId
	private IrtArrayId irtArrayId;

	private String description;
//	@Column(nullable = true)
//	private Integer sequence;
}
