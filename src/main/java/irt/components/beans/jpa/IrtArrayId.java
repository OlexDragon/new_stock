package irt.components.beans.jpa;

import java.io.Serializable;

import javax.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Embeddable
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @EqualsAndHashCode @ToString
public class IrtArrayId implements Serializable {
	private static final long serialVersionUID = 6000736172147526071L;

	private String name;
	private String id;
}
