package irt.components.controllers.components;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import irt.components.beans.Component;
import irt.components.beans.ComponentsResponse;
import irt.components.workers.HttpRequest;

@Controller
public class ComponentsController {
	private final static Logger logger = LogManager.getLogger();

	private static final int SIZE = 60;

	@Value("${irt.url.protocol}")
	private String protocol;

	@Value("${irt.url.login}")
	private String login;

	@Value("${irt.url}")
	private String url;

	@Value("${irt.url.components.catalog}")
	private String componentsCatalog;

    @RequestMapping("/")
    String componentSearch() {
        return "components";
    }

	@PostMapping("components")
	public String search( @RequestParam(required=false) String id, @RequestParam(required=false) String value, @RequestParam(required=false) Integer page, Model model) throws IOException {
//		logger.error("id: {}; value: {}; page: {};", id, value, page);

		String url = createComponentUrl(id, value, page);
//		logger.error(url);
		
		final FutureTask<ComponentsResponse> futureTask = HttpRequest.getForObgect(url, ComponentsResponse.class);

		try {

			final ComponentsResponse componentsResponse = futureTask.get(10, TimeUnit.SECONDS);
			final Component[] components = componentsResponse.getComponents();
			model.addAttribute("components", components);
			model.addAttribute("end", components.length<SIZE);

		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.catching(e);
		}

		return "components :: content";
	}

	// Search Components URL
	private String createComponentUrl(String key, String value, Integer page) throws UnsupportedEncodingException {

		final StringBuilder sb = new StringBuilder(protocol).append(login).append(url).append(ComponentsRestController.encode(componentsCatalog)).append('?');

		List<NameValuePair> params = new ArrayList<NameValuePair>();
//		params.add(new BasicNameValuePair("$format", "json"));
		params.add(new BasicNameValuePair("$top", "" + SIZE));
		params.add(new BasicNameValuePair("$orderby", "SKU"));
		Optional.ofNullable(page).map(p->p*SIZE).ifPresent(skip->params.add(new BasicNameValuePair("$skip", skip.toString())));

		// Filter
		final List<String> contains = new ArrayList<>();

		Optional.ofNullable(key).filter(k->!k.isEmpty()).map(k->"like(" + k + ",'%" + value + "%')").ifPresent(contains::add);

		final String joinContains = contains.stream().filter(c->c!=null && !c.isEmpty()).collect(Collectors.joining(" and "));

//		 logger.error("joinContains: '{}'", joinContains);

		 Optional.of(ComponentsRestController.encode(joinContains)).filter(c->!c.isEmpty()).ifPresent(c->params.add(new BasicNameValuePair("$filter", c)));

		 final String collectPatams = params.stream().map(pair->pair.getName() + "=" + pair.getValue()).collect(Collectors.joining("&"));

		 sb.append(collectPatams);

		 return sb.toString();
	}
}
