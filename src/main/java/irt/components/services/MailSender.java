package irt.components.services;

import irt.components.beans.jpa.rma.Rma.Status;

public interface MailSender {

	void send(String subject, String message, Long rmaId, boolean onWeb);
	void send(String subject, String message, Status ready);
}
