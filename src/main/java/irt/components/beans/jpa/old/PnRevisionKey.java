package irt.components.beans.jpa.old;

import java.io.Serializable;

import javax.persistence.Id;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class PnRevisionKey implements Serializable {
	private static final long serialVersionUID = 5008941385907123214L;

	@Id private Long idComponents;
	@Id private Long pnRevision;
}
