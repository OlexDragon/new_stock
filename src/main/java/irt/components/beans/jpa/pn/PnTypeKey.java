package irt.components.beans.jpa.pn;

import java.io.Serializable;

import javax.persistence.Id;

public class PnTypeKey implements Serializable {
	private static final long serialVersionUID = 7455049278350964771L;

	@Id private Long code;
	@Id private Long nameCode;
}
