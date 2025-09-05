package irt.components.beans.jpa.pn;

import java.io.Serializable;

import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class PnSubtypeKey implements Serializable {
	private static final long serialVersionUID = 7455049278350964771L;

	@Id private Long code;
	@Id private Long nameCode;
}
