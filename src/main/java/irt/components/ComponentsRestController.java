package irt.components;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import irt.components.values.BomsResponse;
import irt.components.values.ComponentFullData;
import irt.components.values.ComponentQuantityResponse;
import irt.components.values.ComponentsResponse;
import irt.components.values.LinkToFile;
import irt.components.values.RelatedFilesResponse;
import irt.components.workers.HttpRequest;

@RestController
@RequestMapping("/components")
public class ComponentsRestController {
	private final static Logger logger = LogManager.getLogger();

	private static final String SIZE = "40";

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

	@PostMapping(produces = "application/json;charset=utf-8")
	public ComponentsResponse search( @RequestParam(required=false) String id, @RequestParam(required=false) String value) throws IOException {

		String url = createComponentUrl(id, value);
		
		final FutureTask<ComponentsResponse> futureTask = HttpRequest.getForObgect(url, ComponentsResponse.class);

		try {

			return futureTask.get(10, TimeUnit.SECONDS);

		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.catching(e);
			return null;
		}
	}

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

	// Search Components URL
	private String createComponentUrl(String key, String value) throws UnsupportedEncodingException {

		final StringBuilder sb = new StringBuilder(protocol).append(login).append(url).append(encode(componentsCatalog)).append('?');

		List<NameValuePair> params = new ArrayList<NameValuePair>();
//		params.add(new BasicNameValuePair("$format", "json"));
		params.add(new BasicNameValuePair("$top", SIZE));
		params.add(new BasicNameValuePair("$orderby", "SKU"));
//		params.add(new BasicNameValuePair("$expand:", "IRT_SchematicLetter"));

		// Filter
		final List<String> contains = new ArrayList<>();

		Optional.ofNullable(key).filter(k->!k.isEmpty()).map(k->"like(" + k + ",'%" + value + "%')").ifPresent(contains::add);

		final String joinContains = contains.stream().filter(c->c!=null && !c.isEmpty()).collect(Collectors.joining(" and "));

//		 logger.error("joinContains: '{}'", joinContains);

		 Optional.of(encode(joinContains)).filter(c->!c.isEmpty()).ifPresent(c->params.add(new BasicNameValuePair("$filter", c)));

		 final String collectPatams = params.stream().map(pair->pair.getName() + "=" + pair.getValue()).collect(Collectors.joining("&"));

		 sb.append(collectPatams);

		 return sb.toString();
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
