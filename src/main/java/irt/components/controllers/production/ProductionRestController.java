package irt.components.controllers.production;

import java.util.ArrayList;
import java.util.List;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import irt.components.beans.ProductionOrderResponse;
import irt.components.workers.HttpRequest;

@RestController
@RequestMapping("production/Rest")
public class ProductionRestController {
	private final static Logger logger = LogManager.getLogger();

	private static final int SIZE = 5;

	@Value("${irt.url.protocol}")
	private String protocol;

	@Value("${irt.url.login}")
	private String login;

	@Value("${irt.url}")
	private String url;

	@Value("${irt.url.ProductionOrder}")
	private String productionOrder;

	@GetMapping("/sales_order")
	public ProductionOrderResponse salesOrder() throws InterruptedException, ExecutionException, TimeoutException{
		logger.error("Yee");

		String url = createUrl(productionOrder);//TODO Change to Sales Order
		logger.error(url);
		final FutureTask<ProductionOrderResponse> ftProductionOrderResponse = HttpRequest.getForObgect(url, ProductionOrderResponse.class);
		final ProductionOrderResponse productionOrderResponse = ftProductionOrderResponse.get(10, TimeUnit.SECONDS);
		logger.error(productionOrderResponse);
		return productionOrderResponse;
	}

	@GetMapping("/production_order")
	public ProductionOrderResponse productionOrder() throws InterruptedException, ExecutionException, TimeoutException{

		String url = createUrl(productionOrder);
		logger.error(url);
		final FutureTask<ProductionOrderResponse> ftProductionOrderResponse = HttpRequest.getForObgect(url, ProductionOrderResponse.class);
		final ProductionOrderResponse productionOrderResponse = ftProductionOrderResponse.get(10, TimeUnit.SECONDS);
		logger.error(productionOrderResponse);
		return productionOrderResponse;
	}

	public String createUrl(String category) {

		final StringBuilder sb = new StringBuilder(protocol).append(login).append(url).append(category).append('?');

		List<NameValuePair> params = new ArrayList<NameValuePair>();
//		params.add(new BasicNameValuePair("$format", "json"));
		params.add(new BasicNameValuePair("$top", "" + SIZE));
//		params.add(new BasicNameValuePair("$orderby", "IRT_Obsolete,SKU"));
//		Optional.ofNullable(page).map(p->p*SIZE).ifPresent(skip->params.add(new BasicNameValuePair("$skip", skip.toString())));

		// Filter
//		final List<String> contains = new ArrayList<>();

//		Optional.ofNullable(key).filter(k->!k.isEmpty()).map(k->"like(" + k + ",'%" + value + "%')").ifPresent(contains::add);

//		final String joinContains = contains.stream().filter(c->c!=null && !c.isEmpty()).collect(Collectors.joining(" and "));

//		 logger.error("joinContains: '{}'", joinContains);

//		 Optional.of(ComponentsRestController.encode(joinContains)).filter(c->!c.isEmpty()).ifPresent(c->params.add(new BasicNameValuePair("$filter", c)));

		 final String collectPatams = params.stream().map(pair->pair.getName() + "=" + pair.getValue()).collect(Collectors.joining("&"));

		 sb.append(collectPatams);

		 return sb.toString();
	}
}
