package irt.components.beans.jpa.rma;

import irt.components.beans.jpa.rma.Rma.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor @Getter @ToString
public class RmaCountByStatus {

	private Status	 status;
	private long	 count;
}
