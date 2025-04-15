package irt.components.controllers.rma;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.util.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import irt.components.beans.RmaBy;
import irt.components.beans.RmaData;
import irt.components.beans.UserPrincipal;
import irt.components.beans.UserRoles;
import irt.components.beans.jpa.User;
import irt.components.beans.jpa.repository.UserRepository;
import irt.components.beans.jpa.repository.rma.RmaCommentsRepository;
import irt.components.beans.jpa.repository.rma.RmaCommentsWebRepository;
import irt.components.beans.jpa.repository.rma.RmaRepository;
import irt.components.beans.jpa.rma.Rma;
import irt.components.beans.jpa.rma.Rma.Status;
import irt.components.beans.jpa.rma.RmaComment;
import irt.components.beans.jpa.rma.RmaCommentWeb;
import irt.components.beans.jpa.rma.RmaCountByStatus;
import irt.components.services.RmaService;
import irt.components.services.RmaServiceLocal;
import irt.components.services.RmaServiceWeb;
import irt.components.workers.IrtPathEncoder;
import irt.components.workers.ProfileWorker;
import lombok.Getter;

@Controller
@RequestMapping("rma")
public class RmaController {
	private final static Logger logger = LogManager.getLogger();

	public static final String TEST_PATH_TO_RMA_FILES = "c:\\irt\\rma\\files";

	private static final int MAX_RMA_PAGE_SIZE = 1000;

	@Value("${irt.onRender}") 						private String onRender;
	@Value("${irt.onRender.rma.by-serial}")			private String bySerial;
	@Value("${irt.onRender.rma.by-rma}")			private String byRma;
	@Value("${irt.onRender.rma.by-description}")	private String byDescription;
	@Value("${irt.onRender.rma.by-id}")				private String byId;
	@Value("${irt.onRender.rma.by-ids}")			private String byIds;

	@Value("${irt.profile.path}") 	private String profileFolder;

	@Autowired private EntityManager			 entityManager;
	@Autowired private RmaRepository			 rmaRepository;
	@Autowired private RmaCommentsRepository	 rmaCommentsRepository;
	@Autowired private RmaCommentsWebRepository	 rmaCommentsWebRepository;
	@Autowired private UserRepository			 userRepository;

	@Autowired private RmaServiceLocal	local;
	@Autowired private RmaServiceWeb	web;

	@PostConstruct
	public void postConstruct() {
         RmaBy.setRepository(rmaRepository);
	}

	@GetMapping
    String getRmas( @RequestParam(required = false) Map<String, String> rmaParam, Principal principal, Model model) {
		logger.traceEntry("rmaParam: {};", rmaParam);


		Optional.ofNullable(principal)
		.filter(UsernamePasswordAuthenticationToken.class::isInstance)
		.map(UsernamePasswordAuthenticationToken.class::cast)
		.map(UsernamePasswordAuthenticationToken::getPrincipal)
		.map(UserPrincipal.class::cast)
		.map(UserPrincipal::getUser)
		.map(User::getPermission)
		.map(UserRoles::getAuthorities)
		.ifPresent(
				aut->{
					Set<Rma.Status> statuses = new HashSet<>();
					aut.parallelStream().map(UserRoles.class::cast)
					.forEach(
							ur->{
								switch(ur) {

								case SHIPPING:
									statuses.add(Rma.Status.READY);
									statuses.add(Rma.Status.SHIPPED);
									break;

								case PRODUCTION:
									statuses.add(Rma.Status.FIXED);
									statuses.add(Rma.Status.WAITTING);
									break;

								case FINALIZE:
									statuses.add(Rma.Status.FINALIZED);
									break;

								case ADD_RMA:
									statuses.add(Rma.Status.CLOSED);
									break;

								default:
								}
							});

					model.addAttribute("rmaStatuses", statuses);
				});

		model.addAttribute("timestamp", System.currentTimeMillis());

		return "rma";
    }

	@GetMapping("by-id")
    String rmas(@RequestParam Long rmaId, @RequestParam(required = false) Boolean onWeb, HttpServletResponse response) {
		logger.traceEntry("rmaId: {}; onWeb: {};", rmaId, onWeb);

		Optional.ofNullable(onWeb).filter(ow->ow).map(ow->(RmaService)web).orElse(local).rmaById(rmaId)
		.ifPresent(
				rma->{
					try {

						statusToRmaFilter(rma.getStatus(), response);

						final String arrayNode = new ObjectMapper().writeValueAsString(new String[] {"rmaNumber" , rma.getRmaNumber()});
						response.addCookie(new Cookie("rmaSearch", URLEncoder.encode(arrayNode.toString(), "UTF-8")));

					} catch (UnsupportedEncodingException | JsonProcessingException e) {
						logger.catching(e);
					}
				});

		return "redirect:/rma";
	}

	@GetMapping("by-status")
    String rmas(@RequestParam Status status, HttpServletResponse response) {

		statusToRmaFilter(status, response);

		try {

			final String arrayNode = new ObjectMapper().writeValueAsString(new String[] {"rmaNumber" , "RMA"});
			response.addCookie(new Cookie("rmaSearch", URLEncoder.encode(arrayNode.toString(), "UTF-8")));

		} catch (JsonProcessingException | UnsupportedEncodingException e) {
			logger.catching(e);
		}

		return "redirect:/rma";
	}

//	@GetMapping("camera/{rmaId}/{userId}/{timestamp}")
//    String cameraPage(@PathVariable String rmaId, @PathVariable Long userId, @PathVariable Long timestamp, HttpServletRequest request, Model model) {
//		logger.traceEntry("rmaId: {}; userId: {}; timestamp: {}", rmaId, userId, timestamp);
//
//		if(request.getHeader("user-agent").contains("Android"))
//			model.addAttribute("message", "Android");
//
//		final boolean b = (System.currentTimeMillis() - timestamp)/60000 > 30;
//		if(b)	// 30 minutes
//			model.addAttribute("message", "The QR code has expired.\nPlease reopen the RMA message.");
//
//		return "camera";
//	}

	private void statusToRmaFilter(Status status, HttpServletResponse response) {
		logger.traceEntry("status: {}", status);

		Arrays.stream(RmaFilter.values()).parallel()
		.filter(
				filter->
				Arrays.stream(filter.getStatus()).parallel()
				.anyMatch(status::equals))
		.reduce((a,b)->a.getStatus().length<b.getStatus().length ? a : b)
		.ifPresent(filrer->response.addCookie(new Cookie("rmaFilter", filrer.name())));
	}

	@PostMapping("search")
	public String searchRma(
			@RequestParam(required=false)	RmaFilter rmaFilter,
			@RequestParam(required=false) 	String id,		// RMA field ID on the RMA search page
			@RequestParam(required=false) 	String value,	// Value of this field
			@CookieValue(required = false) 	String sortBy,
											Model model) throws IOException {

		logger.traceEntry("id: {}; value: {}; sortBy: {}; rmaFilter:{}", id, value, sortBy, rmaFilter);

		final RmaFilter filter = Optional.ofNullable(rmaFilter).orElse(RmaFilter.ALL);
		final Optional<String> oSortBy = Optional.ofNullable(sortBy);
		final String name = oSortBy.map(sb->sb.replace("rmaOrderBy", "")).map(sb->sb.substring(0, 1).toLowerCase() + sb.substring(1)).orElse("rmaNumber");

		List<RmaData> rmas;
		switch(id) {

// RMA Number
		case "rmaNumber":
			rmas = rmas(RmaBy.byRmaNumber(), byRma, value, oSortBy.orElse("rmaOrderByRmaNumber"), filter);
			break;

// Serial Number
		case "rmaSerialNumber":
			rmas = rmas(RmaBy.bySerialNumber(), bySerial, value, oSortBy.orElse("rmaOrderByRmaNumber"), filter);
			break;

// Description Number
		case "rmaDescription":
			rmas = rmas(RmaBy.byDescription(), byDescription, value, oSortBy.orElse("rmaOrderByRmaNumber"), filter);
			break;

		default:
		case "rmaComments":
			rmas = byComment(value, filter, name);
		}

		model.addAttribute("rmas", rmas);

		final List<RmaCountByStatus> countByStatus = rmaRepository.countByStatus();
		final Long sum = countByStatus.parallelStream().map(RmaCountByStatus::getCount).reduce(0L, Long::sum);
		model.addAttribute("sum", sum);
		final Long ready = countByStatus.parallelStream().filter(r->r.getStatus()==Status.READY).map(RmaCountByStatus::getCount).findAny().orElse(0l);
		model.addAttribute("ready", ready);
		final Long todo = countByStatus.parallelStream().filter(r->r.getStatus()==Status.IN_WORK).map(RmaCountByStatus::getCount).findAny().orElse(0l);
		model.addAttribute("todo", todo);
		final Long waiting = countByStatus.parallelStream().filter(r->r.getStatus()==Status.CREATED).map(RmaCountByStatus::getCount).findAny().orElse(0l);
		model.addAttribute("waiting", waiting);

		return "rma :: rmaCards";
	}

	static Rma saveRMA(final String rmaNumber, String description, Principal principal, ProfileWorker profileWorker, RmaRepository rmaRepository) {
		final Rma rma = new Rma();
		rma.setRmaNumber(rmaNumber);
		rma.setDescription(description);
		rma.setSerialNumber(profileWorker.getSerialNumber());

		final Object pr = ((UsernamePasswordAuthenticationToken)principal).getPrincipal();
		final User user = ((UserPrincipal)pr).getUser();
		rma.setUser(user);
		rma.setUserId(user.getId());

		rma.setStatus(Rma.Status.CREATED);

		// Part Number
		profileWorker.getPartNumber().ifPresent(rma::setPartNumber);

		final Rma savedRma = rmaRepository.save(rma);
		return savedRma;
	}

	@PostMapping(path = "comments")
	public String comments(@RequestParam String rmaId, Model model) throws IOException {
		logger.traceEntry("rmaId: {} ", rmaId );

		final Long id = Long.parseLong(rmaId.replaceAll("\\D", ""));

		if(rmaId.startsWith("web")) {
			final List<RmaCommentWeb> comments = rmaCommentsWebRepository.findByRmaId(id);
			logger.trace(comments);
			model.addAttribute("comments", comments);
			model.addAttribute("onWeb", true);

		}else {
			final List<RmaComment> comments = rmaCommentsRepository.findByRmaId(id);
			logger.trace(comments);
			model.addAttribute("comments", comments);
			model.addAttribute("onWeb", false);
		}


		return "rma :: rmaBody";
	}

	@PostMapping(path = "get_files")
	public String getFiles(@RequestParam Long commentID, @RequestParam Boolean onWeb, Model model) throws IOException {
		logger.traceEntry("commentID: {}; onWeb: {}", commentID, onWeb);

		model.addAttribute("commentID", commentID);
		model.addAttribute("onWeb", onWeb);

		fileNames(commentID, onWeb, model);

		return "rma :: comment_files";
	}

	@PostMapping(path = "show_img")
	public String showImage(@RequestParam Long commentID, @RequestParam Integer imgIndex, @RequestParam Boolean onWeb, Model model) throws IOException {

		model.addAttribute("commentID", commentID);
		model.addAttribute("imgIndex", imgIndex);
		model.addAttribute("onWeb", onWeb);

		final List<Pair<String, String>> fileNames = fileNames(commentID, onWeb, model);
		model.addAttribute("imgName", fileNames.get(imgIndex));

		return "rma :: imgModal";
	}

	private List<Pair<String, String>> fileNames(Long commentID, boolean onWeb, Model model) {

		final RmaService rmaService = onWeb ? web : local;
		final File file = Paths.get(rmaService.getPathToRmaFiles(), commentID.toString()).toFile();

		if(!file.exists())
			return new ArrayList<>();

		final File[] listFiles = file.listFiles();
		final List<Pair<String, String>> fileNames = Arrays.stream(listFiles).filter(f->!f.isDirectory()).filter(f->!f.isHidden()).map(File::getName).map(n->Pair.of(n, IrtPathEncoder.encode(n))).collect(Collectors.toList());
		model.addAttribute("fileNames", fileNames);
		return fileNames;
	}

	private List<RmaData> rmas(RmaBy rmaBy, String path, String value, String sortBy, RmaFilter rmaFilter) {
		logger.traceEntry("rmaBy: {}; path: {}; value: {}; sortBy: {}; rmaFilter:{}", rmaBy, path, value, sortBy, rmaFilter);

		final List<RmaData> rmaDatas = Arrays.stream(

				WebClient.builder().baseUrl(onRender).build().get().uri(buildUri(path, value, sortBy, rmaFilter)).retrieve().toEntity(RmaData[].class)
				.onErrorReturn(onErrorReturn(new Throwable("RmaController.rmaBy.onErrorReturn")), ResponseEntity.ok(new RmaData[0]))
				.block()
				.getBody()).map(
						rmaData->{
							final String fullName = rmaData.getFullName();
							final String[] split = fullName.split("IRT User Id ",2);
							if(split.length>1) {
								long id = Long.parseLong(split[1]);
								userRepository.findById(id)
								.ifPresent(
										u->{
											rmaData.setUsername(u.getUsername());
											rmaData.setFullName(u.getFirstname() + ' ' + u.getLastname());
										});
							}
							return rmaData;
						}).collect(Collectors.toList());

		final int size = rmaDatas.size();
		if(size<MAX_RMA_PAGE_SIZE) {
			final List<RmaData> collect = Optional.of(rmaFilter).map(RmaFilter::getStatus).map(rmaBy.containingIn(value, sortBy, MAX_RMA_PAGE_SIZE - size))

					.orElseGet(rmaBy.containing(value, sortBy, MAX_RMA_PAGE_SIZE - size))
					.stream().map(RmaData::new).collect(Collectors.toList());

			rmaDatas.addAll(collect);
		}
		return logger.traceExit(rmaDatas);
	}

	private List<RmaData> byComment(String value, RmaFilter rmaFilter, String name) {

		final Direction direction = name.equals("rmaNumber") ? Sort.Direction.DESC : Sort.Direction.ASC;

		final List<Long> rmaIds = getRmaIDs(value);

		final List<RmaData> rmaDatas = Optional.of(rmaIds).filter(ids->!ids.isEmpty())

				.map(ids->web.rmasByIds(rmaIds, MAX_RMA_PAGE_SIZE, direction, rmaFilter, name))
				.orElseGet(()->new ArrayList<>());

		final int size = rmaDatas.size();

		if(size<MAX_RMA_PAGE_SIZE) {

			final Sort sort = Sort.by(direction, name);
			Pageable pageable = PageRequest.of(0, MAX_RMA_PAGE_SIZE-size, sort);
			final Status[] status = rmaFilter.getStatus();

			if(status==null || status.length==0) {

				final List<RmaData> list = rmaRepository.findDistinctByRmaCommentsCommentContaining(value, pageable).stream().map(RmaData::new).collect(Collectors.toList());
				rmaDatas.addAll(list);

			}else {

				final List<RmaData> list = rmaRepository.findDistinctByRmaCommentsCommentContainingAndStatusIn(value, pageable, status).stream().map(RmaData::new).collect(Collectors.toList());
				rmaDatas.addAll(list);
			}
			
		}

		return rmaDatas;
	}

	protected List<Long> getRmaIDs(String like) {

		final CriteriaBuilder criteriaBuilder	 = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Long> criteriaQuery	 = criteriaBuilder.createQuery(Long.class);
		final Root<RmaCommentWeb> root			 = criteriaQuery.from(RmaCommentWeb.class);
		criteriaQuery.where(criteriaBuilder.like(criteriaBuilder.upper(root.get("comment")), '%' + like + '%'));
		final CriteriaQuery<Long> select = criteriaQuery.select(root.get("rmaId")).distinct(true);

		return entityManager.createQuery(select).getResultList();
	}

	public static PageRequest getPageRequest(String sortBy, int size) {
		logger.traceEntry("sortBy: {}; size: {};", sortBy, size);
//		logger.catching(new Throwable());
		String name = Optional.ofNullable(sortBy).map(sb->sb.replace("rmaOrderBy", "")).map(sb->sb.substring(0, 1).toLowerCase() + sb.substring(1)).orElse("rmaNumber");
		final Direction direction = name.equals("rmaNumber") ? Sort.Direction.DESC : Sort.Direction.ASC;
		final Sort sort = Sort.by(direction, name);

		return logger.traceExit(PageRequest.of(0, size<1 ? 1 : size, sort));
	}

	public static Predicate<? super Throwable> onErrorReturn(Throwable throwable) {
		return e->{
			throwable.initCause(e);
			logger.catching(throwable);
			return true;
		};
	}

	protected Function<UriBuilder, URI> buildUri(String path, String value, String sortBy, RmaFilter rmaFilter) {
		logger.traceEntry("path: {}; value: {}; sortBy: {}; rmaFilter: {};", path, value, sortBy, rmaFilter);

		return logger.traceExit(

				builder->{

					final Direction direction;
					final String name;
					switch(sortBy) {
					case "rmaOrderBySerialNumber":
						name = "SerialNumberSerialNumber";	// field in irt.web.bean.jpa.Rma (onRender)
						direction = Direction.ASC;
						break;
					default:
						name = "rmaNumber";					// field in irt.web.bean.jpa.Rma (onRender)
						direction = Direction.DESC;
					}

					return builder.path(path)
							.queryParam("like", value)
							.queryParam("name", name)
							.queryParam("size", MAX_RMA_PAGE_SIZE)
							.queryParam("status", (Object[])rmaFilter.getStatus())
							.queryParam("direction", direction)
							.build();
				});
	}

	@Getter 
	public enum RmaFilter{
		ALL(Rma.Status.values()),					// Show all RMAs
		SHI(Rma.Status.SHIPPED, Rma.Status.CLOSED),	// Show shipped RMAs
		REA(Rma.Status.READY, Rma.Status.FIXED, Rma.Status.FINALIZED),	// Show RMAs ready to ship
		WOR(Rma.Status.IN_WORK, Rma.Status.CREATED, Rma.Status.WAITTING);// Show RMAs in work

		private Status[] status;

		RmaFilter(Rma.Status... status){
			Objects.nonNull(status);
			this.status = status;
		}
	}
}
