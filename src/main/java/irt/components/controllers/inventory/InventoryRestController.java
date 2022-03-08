package irt.components.controllers.inventory;

import java.io.IOException;
import java.util.Optional;

import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import irt.components.beans.inventory.InventoryTransfer;
import irt.components.beans.inventory.Inventory;
import irt.components.beans.inventory.InventoryValues;

@RestController
@RequestMapping("/inventory/rest")
public class InventoryRestController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.url.protocol}")
	private String protocol;

	@Value("${irt.url.login}")
	private String login;

	@Value("${irt.url}")
	private String url;

	@Value("${irt.url.inventoryTransfer}")
	private String inventoryTransferUrl;

	@PostMapping
	public Optional<InventoryTransfer> fromStock(@RequestParam String productKey, @RequestParam int qty, @RequestParam String userName, @RequestParam String comments) throws IOException {
		logger.error("{}; {}; {}; {};", productKey, qty, userName, comments);

		final String c = userName + ": " + comments;
		final InventoryTransfer inventoryTransfer = new InventoryTransfer();
		inventoryTransfer.setTransferKey("7e349b05-8dc6-11ec-b0bd-04d4c452793b");
//		inventoriesTransfer.setComment(c);

		final String postUrl = new StringBuilder(protocol).append(login).append(url).append(inventoryTransferUrl).toString();

		// To remove
		final Inventory inventory = new Inventory();
		inventoryTransfer.addInventory(inventory);
		inventory.setTransferKey("7e349b05-8dc6-11ec-b0bd-04d4c452793b");
		inventory.setLineNumber(2);
		inventory.setProductKey(productKey);
		inventory.setQty(qty);
		final InventoryValues inventoryValues = new InventoryValues();
		inventoryValues.getInventories().add(inventory);

		return postInventory(postUrl, inventoryTransfer, InventoryTransfer.class);

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
