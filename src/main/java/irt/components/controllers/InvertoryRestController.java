package irt.components.controllers;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import irt.components.beans.InventoriesTransfer;
import irt.components.beans.Inventory;

@RestController
@RequestMapping("/invertory/rest")
public class InvertoryRestController {
	private final static Logger logger = LogManager.getLogger();

	@PostMapping
	public String fromStock(@RequestParam String unitSerialNumber, @RequestParam String key, @RequestParam int qty, @RequestParam String serialNumbers, @RequestParam String userName, @RequestParam String comments) throws IOException {
		logger.error("{}; {}; {}; {}; {};", key, qty, serialNumbers, userName, comments);

		final InventoriesTransfer inventoriesTransfer = new InventoriesTransfer(unitSerialNumber, userName + ": " + comments, new Inventory(key, qty, serialNumbers, null));
		final String json = new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(inventoriesTransfer);

		logger.error(json);

		HttpPost post = new HttpPost("http://192.168.100.241/irt-prod-web/odata/standard.odata/Document_InventoryTransfer");
		post.addHeader("Accept", "text/html,application/json;metadata=full;charset=utf-8;");
		post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

		try(	CloseableHttpClient httpClient = HttpClientBuilder.create().build();
				CloseableHttpResponse response = httpClient.execute(post);){
			
		}
		return null;
	}
}
