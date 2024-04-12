package irt.components.beans;

import java.util.Date;

import irt.components.beans.jpa.User;
import irt.components.beans.jpa.rma.Rma;
import irt.components.beans.jpa.rma.Rma.Status;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor @Getter @Setter @ToString
public class RmaData implements DateContainer {

	private Long	id;
	private String 	rmaNumber;
	private String 	malfunction;
	private String 	serialNumber;
	private String 	partNumber;
	private String 	description;
	private String 	fullName;
	private Date	creationDate;
	private Status	status;
	private String	username;

	private boolean fromWeb = false;

	public RmaData(Rma rma) {

		id = rma.getId();
		rmaNumber = rma.getRmaNumber();
		serialNumber = rma.getSerialNumber();
		partNumber = rma.getPartNumber();
		description = rma.getDescription();
		creationDate = rma.getDate();
		status = rma.getStatus();

		final User jpaUser = rma.getUser();
		username = jpaUser.getUsername();
		fullName = jpaUser.getFirstname() + ' ' + jpaUser.getLastname();
	}

	@Override
	public Date getDate() {
		return creationDate;
	}
}
