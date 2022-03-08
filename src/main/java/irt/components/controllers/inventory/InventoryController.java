package irt.components.controllers.inventory;

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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import irt.components.beans.ComponentsResponse;
import irt.components.beans.inventory.InventiryTransferResponse;
import irt.components.beans.inventory.InventoryTransfer;
import irt.components.controllers.components.ComponentsRestController;
import irt.components.workers.HttpRequest;

@Controller
@RequestMapping("inventory")
public class InventoryController {
	private final static Logger logger = LogManager.getLogger();

	private static final String SIZE = "40";

	@Value("${irt.url.protocol}")
	private String protocol;

	@Value("${irt.url.login}")
	private String login;

	@Value("${irt.url}")
	private String url;

	@Value("${irt.url.inventoryTransfer}")
	private String inventoryTransferUrl;

    @GetMapping
    String getInventoryPage() {

    	return "/inventory";
    }

    @PostMapping("add_inventory")
    String addInventoryTransfer(@RequestParam String description, @RequestParam String userName, HttpServletResponse response) throws IOException {

		final InventoryTransfer inventoryTransfer = new InventoryTransfer();
    	final String comment = userName + ": " + description;
		inventoryTransfer.setComment(comment);

		final String postUrl = new StringBuilder(protocol).append(login).append(url).append(inventoryTransferUrl).toString();

		postInventory(postUrl, inventoryTransfer, InventoryTransfer.class)
		.ifPresent(
				transfer->{
					logger.error(transfer);
			    	Cookie cookie = new Cookie("inventorytSearch", "[\"Number\",\"" + transfer.getNumber() + "\"]");
			    	response.addCookie(cookie);
				});;

    	return "inventory";
    }

    @PostMapping
    String searchInventories( @RequestParam String name, @RequestParam String value, Model model) throws UnsupportedEncodingException {

		String url = createUrl(name, value);
		logger.error(url);
		final FutureTask<InventiryTransferResponse> futureTask = HttpRequest.getForObgect(url, InventiryTransferResponse.class);

		try {

			final InventiryTransferResponse transfer = futureTask.get(10, TimeUnit.SECONDS);
			logger.error(transfer);
			model.addAttribute("transfers", transfer.getInventoryTransfers());

		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.catching(e);
		}

       return "inventory :: inventoryCards";
    }

	private String createUrl(String name, String value) throws UnsupportedEncodingException {

		final StringBuilder sb = new StringBuilder(protocol).append(login).append(url).append(ComponentsRestController.encode(inventoryTransferUrl)).append('?');

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("$top", SIZE));

		// Filter
		final List<String> contains = new ArrayList<>();

		Optional.ofNullable(name).filter(k->!k.isEmpty()).map(k->"like(" + k + ",'%" + value + "%')").ifPresent(contains::add);

		final String joinContains = contains.stream().filter(c->c!=null && !c.isEmpty()).collect(Collectors.joining(" and "));

		 logger.error("joinContains: '{}'", joinContains);

		 Optional.of(ComponentsRestController.encode(joinContains)).filter(c->!c.isEmpty()).ifPresent(c->params.add(new BasicNameValuePair("$filter", c)));

		 final String collectPatams = params.stream().map(pair->pair.getName() + "=" + pair.getValue()).collect(Collectors.joining("&"));

		 sb.append(collectPatams);

		 return sb.toString();
	}

	private <T>  Optional<T> postInventory(String postUrl, T object, Class<T> classToReturn) throws IOException {

		final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(Include.NON_NULL).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		final String json = mapper.writer().withDefaultPrettyPrinter().writeValueAsString(object);
		logger.error("\n{}\n{}", postUrl, json);

		final HttpPost post = new HttpPost(postUrl);
		post.addHeader("Accept", "text/html,application/json;metadata=full;charset=utf-8;");
		post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

		try(	CloseableHttpClient httpClient = HttpClientBuilder.create().build();
				CloseableHttpResponse response = httpClient.execute(post);){

			return Optional.ofNullable(response.getEntity())
					.map(
							entity->{

								try {

									final String string = EntityUtils.toString(entity);
									logger.error(string);

									return mapper.readValue(string, classToReturn);

								} catch (ParseException | IOException e) {
									logger.catching(e);
								}

								return null;
							});
		}
	}
}
