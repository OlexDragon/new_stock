package irt.components.services;

import static irt.components.controllers.rma.RmaController.onErrorReturn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import irt.components.beans.DateContainer;
import irt.components.beans.jpa.repository.rma.RmaCommentsRepository;
import irt.components.beans.jpa.repository.rma.RmaCommentsWebRepository;
import irt.components.beans.jpa.repository.rma.RmaRepository;
import irt.components.beans.jpa.rma.Rma;
import irt.components.beans.jpa.rma.Rma.Status;
import irt.components.beans.jpa.rma.RmaComment;
import irt.components.beans.jpa.rma.RmaCommentWeb;
import irt.components.workers.ProfileWorker;
import irt.components.workers.ThreadRunner;

@Service
public class SerialNumberScaner {
	private final Logger logger = LogManager.getLogger();

	@Value("${irt.onRender.serialNumber}") 	private String onRenderSN;
	@Value("${irt.profile.path}") 	private String profileFolder;

	@Autowired private EntityManager			 entityManager;
	@Autowired private RmaRepository			 rmaRepository;
	@Autowired private RmaCommentsRepository	 rmaCommentsRepository;
	@Autowired private RmaCommentsWebRepository	 rmaCommentsWebRepository;

	@Autowired private MailSender		mailSender;
	@Autowired private RmaServiceWeb	web;

	private Optional<String> oClientIp = Optional.empty();

	@Scheduled(cron = "0 0 1 * * SUN")
	public void scan() throws IOException {

		oClientIp = getClientIp();
		if(!oClientIp.isPresent()) {
			logger.warn("Unable to obtain an IP address.");
			return;
		}

		logger.info("Scan starts");

		try (Stream<Path> stream = Files.walk(Paths.get(profileFolder))) {

			List<String> saved = new ArrayList<>();
			stream.filter(Files::isRegularFile)
		    .filter(
		    		p->{

		    			final String lowerCase = p.getFileName().toString().toLowerCase();
		    			final String sn = lowerCase.split("\\.")[0];

		    			final int length = sn.length();
						if(length>11 || length<8 || sn.replaceAll("\\D", "").length()!=7 || !Character.isDigit(sn.charAt(length-1)) || !lowerCase.endsWith(".bin")) {
		    				logger.info("{} was ignored", lowerCase);
							return false;
						}

		    			return true;
		    		})
		    .forEach(
		    		p->{

		    			final ProfileWorker pw = new ProfileWorker(p);
		    			final LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		    			final String sn = p.getFileName().toString().split("\\.", 2)[0];
						params.add("sn", sn);
		    			pw.getPartNumber().ifPresent(pn->params.add("pn", pn));
		    			pw.getDescription().ifPresent(d->params.add("descr", d));

		    			if(params.size()==3) {
		    				final StringBuffer sb = new StringBuffer(onRenderSN).append("/save");
		    				if(sendRequest(sb, params))
			    				saved.add(sn);
		    			}else
		    				logger.warn("One of the parameters is missing. {}", params);

		    			try {
							TimeUnit.MILLISECONDS.sleep(100);
						} catch (InterruptedException e) {
							logger.catching(e);
						}
		    		});

			logger.info("Scan completed. {} New Serial Numbers: {}", saved.size(), saved);
		}

		ThreadRunner.runThread(scanRMAs());
	}

	private Runnable scanRMAs() {
		return ()->{
			checkCreatedRMAs();
			rmaReadyToShipReminder();
		};
	}

	public void rmaReadyToShipReminder() {

		// Local
		final List<Long> ids = rmaReadyToShipLocal();

		// From Web
		final List<Long> fromWeb = web.rmaIdsByStatus(Rma.Status.READY, Rma.Status.FINALIZED, Rma.Status.FIXED);
		ids.addAll(fromWeb);

		if(!ids.isEmpty())
			mailSender.send("RMA Readiness Reminder.", "We have " + ids.size() + " RAMs ready to ship.", Rma.Status.READY);
	}

	public List<Long> rmaReadyToShipLocal() {
		final CriteriaBuilder criteriaBuilder	 = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Long> criteriaQuery	 = criteriaBuilder.createQuery(Long.class);
		final Root<Rma> root					 = criteriaQuery.from(Rma.class);
		criteriaQuery.where(root.get("status").in(Rma.Status.READY.ordinal(), Rma.Status.FIXED.ordinal(), Rma.Status.FINALIZED.ordinal()));
		final CriteriaQuery<Long> select = criteriaQuery.select(root.get("id")).distinct(true);

		return entityManager.createQuery(select).getResultList();
	}

	public void checkCreatedRMAs() {
		rmaRepository.findByStatus(Rma.Status.CREATED)
		.forEach(
				rma->{

					// Local
					final long days = accumulatedDays(rma);
					if(days>183) {

						rma.setStatus(Status.CLOSED);
						rmaRepository.save(rma);

						mailSender.send(rma.getRmaNumber() + " automatically CLOSED", " RMA was created more than six months ago.\r\nThis RMA is CLOSED as it was never delivered.", rma.getId(), false);

						final RmaComment comment = new RmaComment();
						comment.setComment("This RMA is automatically CLOSED because it was never delivered.");
						comment.setRmaId(rma.getId());
						comment.setUserId(1L);
						comment.setHasFiles(false);
						rmaCommentsRepository.save(comment);
					}

				});

		web.rmaByStatus(Rma.Status.CREATED)
		.forEach(
				rma->{
					final long days = accumulatedDays(rma);
					if(days>183) {

						web.changeStatus(rma.getId(), Status.CLOSED);

						mailSender.send(rma.getRmaNumber() + " automatically CLOSED", " RMA was created more than six months ago.\r\nThis RMA is CLOSED as it was never delivered.", rma.getId(), true);

						final RmaCommentWeb comment = new RmaCommentWeb();
						comment.setComment("This RMA is automatically CLOSED because it was never delivered.");
						comment.setRmaId(rma.getId());
						comment.setUserId(1L);
						comment.setHasFiles(false);
						rmaCommentsWebRepository.save(comment);
					}
				});
	}

	public static Optional<String> getClientIp() {
		final String ip = WebClient.create("https://api.ipify.org")

				.get()
				.retrieve()
				.bodyToMono(String.class)
				.onErrorReturn(onErrorReturn(new Throwable("SerialNumberScaner.getClientIp.onErrorReturn")), "").block();

		return Optional.ofNullable(ip);
	}

	public boolean sendRequest(final StringBuffer sbURL, LinkedMultiValueMap<String, String> params) {

		if(!oClientIp.isPresent()) {
			logger.warn("Unable to obtain an IP address.");
			return false;
		}
		return WebClient.builder().baseUrl(onRenderSN).defaultCookie("clientIP", oClientIp.get()).build()
				.post().uri(uriBuilder->uriBuilder.path("/save").queryParams(params).build())
				.retrieve()
				.bodyToMono(Boolean.class)
				.onErrorReturn(onErrorReturn(new Throwable("SerialNumberScaner.sendRequest.onErrorReturn")), false).block();
	}

	public static long accumulatedDays(DateContainer dateContainer) {
		final Date date = new Date();
		final long now = date.getTime();
		final long before = dateContainer.getDate().getTime();
		final long time = now - before;
		return TimeUnit.DAYS.convert(time, TimeUnit.MILLISECONDS);
	}
}
