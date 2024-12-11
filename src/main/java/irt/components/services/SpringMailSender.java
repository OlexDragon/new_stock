package irt.components.services;

import java.net.InetAddress;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import irt.components.beans.jpa.repository.MailPropertiesRepository;
import irt.components.beans.jpa.rma.Rma.Status;
import irt.components.workers.ThreadRunner;

@Service
public class SpringMailSender implements MailSender {
	private final static Logger logger = LogManager.getLogger();

	@Autowired  private JavaMailSenderImpl mailSender;
	@Autowired private MailPropertiesRepository repository;

	@Override
	public void send(String subject, String message, Long rmaId, boolean onWeb) {
		logger.traceEntry("\n\tsubject: {};\n message: {}\n\trmaId: {}\n\tonWeb: {}", subject, message, rmaId, onWeb);
		ThreadRunner.runThread(()->send(subject, message, ":8089/rma/by-id?rmaId=" + rmaId + (onWeb ? "&onWeb=" + onWeb : "")));
	}

	@Override
	public void send(String subject, String message, Status status) {
		logger.traceEntry("\n\tsubject: {};\n message: {}\n\tstatus: {}", subject, message, status);
		ThreadRunner.runThread(()->send(subject, message, ":8089/rma/by-status?status=" + status ));
	}

	public void send(String subject, String message) {
		logger.traceEntry("\n\tsubject: {};\n message: {}", subject, message);
		repository.findById(1L)
		.ifPresent(
				p->{

					mailSender.setUsername(p.getFrom());
					mailSender.setPassword(p.getPassword());

					final String[] to = p.getTo().split(",");
					final String[] cc = Optional.ofNullable(p.getCc()).map(c->c.split(",")).orElse(null);

					final SimpleMailMessage m = new SimpleMailMessage();
					m.setFrom(p.getFrom());
			        m.setTo(to);
			        Optional.ofNullable(cc).ifPresent(m::setCc);

			        m.setSubject(subject);
			        m.setText(message);

			        mailSender.send(m);
				});
	}

	private void send(String subject, String message, final String path) {
		logger.traceEntry("\n\tsubject: {};\n message: {}\n\tpath: {}", subject, message, path);
		synchronized (InetAddress.class) {

			try {

				Thread.sleep(1000);
				String url;

				try {

					final String sName = InetAddress.getLocalHost().getHostName();
					url = "\nhttp://" + sName + path;

					final InetAddress localHost = InetAddress.getLocalHost();
					final byte[] address = localHost.getAddress();

					url += "\nhttp://" + IntStream.range(0, address.length).mapToObj(index->address[index]&0xff).map(Number::toString).collect(Collectors.joining(".")) + path;

				} catch (Exception e) {
					logger.catching(e);
					url = "";
				}

				send(subject, message + url);

			} catch (Exception e) {
				logger.catching(e);
			}
		}
	}
}
