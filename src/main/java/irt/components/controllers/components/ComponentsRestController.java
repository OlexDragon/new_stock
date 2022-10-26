package irt.components.controllers.components;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import irt.components.beans.BomsResponse;
import irt.components.beans.ComponentFullData;
import irt.components.beans.ComponentQuantityResponse;
import irt.components.beans.LinkToFile;
import irt.components.beans.RelatedFilesResponse;
import irt.components.workers.HttpRequest;

@RestController
@RequestMapping("/components")
public class ComponentsRestController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.url.protocol}")
	private String protocol;

	@Value("${irt.url.login}")
	private String login;

	@Value("${irt.url}")
	private String url;

	@Value("${irt.url.components.catalog}")
	private String componentsCatalog;

	@Value("${irt.url.components.attributes}")
	private String componentsAttributes;

	@Value("${irt.url.components.manufactures}")
	private String componentsMnufactures;

	@Value("${irt.url.supliers}")
	private String componentsSupliers;

	@Value("${irt.url.location}")
	private String location;

	@Value("${irt.url.bom}")
	private String bom;

	@Value("${irt.url.files}")
	private String filesCatalog;

	@Value("${irt.host}")
	private String host;

	@PostMapping(path="single", produces = "application/json;charset=utf-8")
	public ComponentFullData singleComponent(@RequestParam("key") String componentKey) throws IOException {
//		logger.error(componentKey);

		String url = createComponentQtyUrl(componentKey);
		final FutureTask<ComponentQuantityResponse> quantityFutureTask = HttpRequest.getForObgect(url, ComponentQuantityResponse.class);

		url = createBomUrl(componentKey);
//		logger.error(url);
		final FutureTask<BomsResponse> bomFutureTask = HttpRequest.getForObgect(url, BomsResponse.class);

		url = createFilesUrl(componentKey);
//		logger.error(url);
		final FutureTask<RelatedFilesResponse> filesFutureTask = HttpRequest.getForObgect(url, RelatedFilesResponse.class);

		try {

			// Component Quantities Location
			final ComponentQuantityResponse componentQuantityResponse = quantityFutureTask.get(10, TimeUnit.SECONDS);
//			logger.error(componentQuantityResponse);
			final ComponentFullData componentData = new ComponentFullData(componentQuantityResponse);

			//BOM
			final BomsResponse bomsResponse = bomFutureTask.get(10, TimeUnit.SECONDS);
//			logger.error("\n{}\n'{}'", url, bomsResponse);
			componentData.setBomsResponse(bomsResponse);

			//Link to file
			LinkToFile.setHost(host);
			final RelatedFilesResponse relatedFilesResponse = filesFutureTask.get(10, TimeUnit.SECONDS);

			componentData.setRelatedFilesResponse(relatedFilesResponse);

//			logger.error("\n{}\n{}", url, relatedFilesResponse);
			return componentData;

		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.catching(e);
			return null;
		}
    }

	@PostMapping("test-login")
	public boolean testLogin() {
		return true;
	}

	// Quantities
	private String createComponentQtyUrl(String key) throws UnsupportedEncodingException {
		return new StringBuilder(protocol).append(login).append(url)
				.append("AccumulationRegister_InventoryInWarehouses/Balance(").append(encode("Dimensions='Products,StructuralUnit',Condition='Products_Key eq guid'")).append(key).append("'')")
				.append("?$expand=Products,StructuralUnit&$select=QuantityBalance,StructuralUnit/Ref_Key,StructuralUnit/Description,StructuralUnit/PredefinedDataName,Products/Ref_Key,Products/SKU,Products/MfrPNs,Products/ProductsType,Products/Description").toString();
	}

	// Where use...
	private String createBomUrl(String componentKey) throws UnsupportedEncodingException {
		return new StringBuilder(protocol).append(login).append(url).append(bom).append('?')
				.append(encode("$filter=Content/Products_Key eq guid'")).append(componentKey).append(encode("'&$select=Ref_Key,Description,Owner/Ref_Key,Owner/SKU,Owner/Description,Status&$orderby=Description&$expand=Owner")).toString();
	}

	// Related Files
	private String createFilesUrl(String componentKey) throws UnsupportedEncodingException {
		return new StringBuilder(protocol).append(login).append(url).append(filesCatalog).append('?')
				.append(encode("$filter=FileOwner_Key eq guid'")).append(componentKey).append(encode("'&$select=Ref_Key,Author,FileOwner_Key,PathToFile")).toString();
	}

	public static String encode(String str) throws UnsupportedEncodingException {

		if(str==null || str.isEmpty())
			return "";

		return UriUtils.encodePath(str, StandardCharsets.UTF_8.toString());
	}
}
