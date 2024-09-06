package irt.components.controllers.production;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import irt.components.beans.OneCeUrl;
import irt.components.beans.SalesOrderResponse;
import irt.components.workers.HttpRequest;

@Controller
@RequestMapping("/production")
public class ProductionController {
	private final static Logger logger = LogManager.getLogger();

	@Autowired private OneCeUrl oneCeUrl;

	@Value("${irt.url.ProductionOrder}")
	private String productionOrder;

	@Value("${irt.url.SalesOrder}")
	private String salesOrder;

	@GetMapping
    String production(Model model) throws InterruptedException, ExecutionException, TimeoutException {
		logger.traceEntry();

		return "production";
    }

    @GetMapping("sales_orders")
    String salesOrders(Model model) throws InterruptedException, ExecutionException, TimeoutException {
		logger.traceEntry();

		String url = oneCeUrl.createUrl(salesOrder);
		logger.debug(url);

		final SalesOrderResponse productionOrderResponse = HttpRequest.getForObgect(url, SalesOrderResponse.class).get(10, TimeUnit.SECONDS);
		logger.debug(productionOrderResponse);
		model.addAttribute("salesOrders", productionOrderResponse.getSalesOrders());

		return "production :: sales_order";
    }
}
