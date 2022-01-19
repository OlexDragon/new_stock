package irt.components;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import irt.components.values.BomContentResponse;
import irt.components.values.BomsResponse;
import irt.components.workers.HttpRequest;

@Controller
@RequestMapping("bom")
public class BomController {
	private final static Logger logger = LogManager.getLogger();

	private static final String SIZE = "40";

	@Value("${irt.url.protocol}")
	private String protocol;

	@Value("${irt.url.login}")
	private String login;

	@Value("${irt.url}")
	private String url;

	@Value("${irt.url.bom}")
	private String bomCatalog;

	@Value("${irt.url.bom.content}")
	private String bomContent;

	@Value("${irt.url.components.catalog}")
	private String componentsCatalog;

    @GetMapping
    String getBoms(@RequestParam(name="key", required=false) String bomKey, Model model) {

    	Optional.ofNullable(bomKey).filter(key->!key.isEmpty())
    	.ifPresent(
    			key->{

    		    	try {

    		    		final String url = createBomUrl(key);
//    		    		logger.error(url);
    		    	
    		    		final FutureTask<BomsResponse> futureTask = HttpRequest.getForObgect(url, BomsResponse.class);

    					final BomsResponse bomsResponse = futureTask.get(10, TimeUnit.SECONDS);
//     			    	logger.error(bomsResponse);
 
    					model.addAttribute("boms", bomsResponse.getBoms());
    					
//    					final String ownerUrl = createSingleComponentUrl(bom.getOwnerKey());
//        		    	final FutureTask<Component> ownerFutureTask = HttpRequest.requestObgect(ownerUrl, Component.class);
//        		    	final Component component = ownerFutureTask.get(10, TimeUnit.SECONDS);
//     			    	logger.error(component);
     			    	 
//    					model.addAttribute("partNumber", component.getPartNumber());
    					

    				} catch (InterruptedException | ExecutionException | TimeoutException | UnsupportedEncodingException e) {
    					logger.catching(e);
    				}
    			});

       return "bom";
    }

    @PostMapping("components")
    String getBomComponents(@RequestParam String bomKey, Model model) {
//		logger.error(bomKey);

    	Optional.ofNullable(bomKey).filter(key->!key.isEmpty())
    	.ifPresent(
    			key->{

    		    	try {

    		    		final String url = createBomContentsUrl(key);
//    		    		logger.error(url);
    		    	
    		    		final FutureTask<BomContentResponse> futureTask = HttpRequest.getForObgect(url, BomContentResponse.class);

    					final BomContentResponse bomContentResponse = futureTask.get(10, TimeUnit.SECONDS);
//     			    	logger.error(bomContentResponse);
 
    					model.addAttribute("bomContents", bomContentResponse.getBomContents());
    					
//    					final String ownerUrl = createSingleComponentUrl(bom.getOwnerKey());
//        		    	final FutureTask<Component> ownerFutureTask = HttpRequest.requestObgect(ownerUrl, Component.class);
//        		    	final Component component = ownerFutureTask.get(10, TimeUnit.SECONDS);
//     			    	logger.error(component);
     			    	 
//    					model.addAttribute("partNumber", component.getPartNumber());
    					

    				} catch (InterruptedException | ExecutionException | TimeoutException | UnsupportedEncodingException e) {
    					logger.catching(e);
    				}
    			});
    
        return "bom::bomBody";
    }

	@PostMapping(path = "search", produces = "application/json;charset=utf-8")
	public String searchBom(
			@RequestParam(required=false) String id,
			@RequestParam(required=false) String value,
			Model model) throws IOException {

//		logger.error("{} : {}", id, value);
		final String url = createSearchByBomUrl(id, value);

		final FutureTask<BomsResponse> futureTask = HttpRequest.getForObgect(url, BomsResponse.class);

		try {

			BomsResponse bomsResponse = futureTask.get(10, TimeUnit.SECONDS);futureTask.get(10, TimeUnit.SECONDS);
			model.addAttribute("boms", bomsResponse.getBoms());

			return "bom :: bomCards";

		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.catching(e);
			return null;
		}
	}

	private final static Map<String, String> parseId = new HashMap<>();
	static {
		parseId.put("bomPartNumber"		, "Owner/SKU");
		parseId.put("bomDescription"	, "Description");
		parseId.put("componentPartNumber","Content/Products/SKU");
		parseId.put("componentMfrPN"	, "Content/Products/MfrPNs");
	}

	// Search Components URL
	private String createSearchByBomUrl(String htmlId, String value) throws UnsupportedEncodingException {

		final StringBuilder sb = new StringBuilder(protocol).append(login).append(url).append(ComponentsRestController.encode(bomCatalog)).append('?');

		List<NameValuePair> params = new ArrayList<NameValuePair>();
//		params.add(new BasicNameValuePair("$format", "json"));
		params.add(new BasicNameValuePair("$top", SIZE));
		params.add(new BasicNameValuePair("$orderby", "Owner/SKU"));
		params.add(new BasicNameValuePair("$expand", "Owner"));
		params.add(new BasicNameValuePair("$select", "Ref_Key,Description,Status,Owner,Owner/Ref_Key,Owner/SKU,Owner/Description"));

		// Filter
		final List<String> contains = new ArrayList<>();

		Optional.ofNullable(htmlId).map(parseId::get).map(key->"like(" + key + ",'%" + value + "%')").ifPresent(contains::add);

		final String joinContains = contains.stream().filter(c->c!=null && !c.isEmpty()).collect(Collectors.joining(" and "));

//		 logger.error("joinContains: '{}'", joinContains);

		 Optional.of(ComponentsRestController.encode(joinContains)).filter(c->!c.isEmpty()).ifPresent(c->params.add(new BasicNameValuePair("$filter", c)));

		 final String collectPatams = params.stream().map(pair->pair.getName() + "=" + pair.getValue()).collect(Collectors.joining("&"));

		 sb.append(collectPatams);

		 return sb.toString();
	}

	// Single BOM URL
	private String createBomUrl(String key) throws UnsupportedEncodingException {
		return new StringBuilder(protocol).append(login).append(url).append(bomCatalog).append('?')
				.append(ComponentsRestController.encode("$expand=Owner&$select=Ref_Key,Description,Owner/Ref_Key,Owner/SKU&$filter=Ref_Key eq guid'")).append(key).append('\'').toString();
	}

	// Single BOM URL
	private String createBomContentsUrl(String key) throws UnsupportedEncodingException {
		return new StringBuilder(protocol).append(login).append(url).append(bomContent).append('?')
				.append(ComponentsRestController.encode("$expand=Products&$select=IRT_Reference,Quantity,Products/Ref_Key,Products/SKU,Products/Description,Products/MfrPNs,Products/ProductsType&$filter=Ref_Key eq guid'")).append(key).append('\'').toString();
	}
}
