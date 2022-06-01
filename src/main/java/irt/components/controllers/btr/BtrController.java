package irt.components.controllers.btr;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import irt.components.beans.jpa.btr.BtrSerialNumber;
import irt.components.beans.jpa.btr.BtrWorkOrder;
import irt.components.beans.jpa.repository.btr.BtrSerialNumberRepository;
import irt.components.beans.jpa.repository.btr.BtrWorkOrderRepository;
import irt.components.workers.ProfileWorker;

@Controller
@RequestMapping("btr")
public class BtrController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.profile.path}")
	private String profileFolder;

	@Autowired private BtrWorkOrderRepository workOrderRepository;
	@Autowired private BtrSerialNumberRepository serialNumberRepository;

    @GetMapping
    String btrs() {
       return "btr";
    }

    @PostMapping("search")
    @Transactional(readOnly = true)
    String search(@RequestParam String id, @RequestParam String value, Model model) {

    	List<BtrWorkOrder> workOrders;
		switch(id) {

    	case "btrWoNumber":
    		workOrders = workOrderRepository.findByNumberContainingOrderByNumberDesc(value);
    		break;

    	case "btrSerialNumber":
    		workOrders = serialNumberRepository.findBySerialNumberContainingOrderBySerialNumberDesc(value).stream().map(BtrSerialNumber::getWorkOrder).distinct().collect(Collectors.toList());
    		break;

    	default:	// 'rmaDescription'
    		workOrders = serialNumberRepository.findByDescriptionContainingOrderBySerialNumberDesc(value).stream().map(BtrSerialNumber::getWorkOrder).distinct().collect(Collectors.toList());
    	}

		model.addAttribute("workOrders", workOrders);

    	return "btr :: btrCards";
    }

    @PostMapping("modal_add")
    String modaleAdd(@RequestParam String sn, Model model) {
    	logger.traceEntry("sn: {}", sn);

    	serialNumberRepository.findBySerialNumber(sn).orElseGet(()->{model.addAttribute("serialNumber", sn); return null;});

    	return "btr :: modalSN";
    }

    @PostMapping("comment")
    String modaleComment(@RequestParam Long snId, Principal principal, Model model) {

    	serialNumberRepository.findById(snId).ifPresent(sn->model.addAttribute("serialNumber", sn));

    	return "btr :: modalComment";
    }

    @PostMapping("add_sn")
    @Transactional(readOnly = true)
    String addSerialNumber(@RequestParam String sn, @RequestParam String wo, Principal principal, Model model) throws IOException {

    	if(!(principal instanceof UsernamePasswordAuthenticationToken))
			return "btr :: btrCards";

		final BtrSerialNumber btrSerialNumber = serialNumberRepository.findBySerialNumber(sn)

				.orElseGet(
						()->{
							final BtrSerialNumber serialNumber = new BtrSerialNumber();
							serialNumber.setSerialNumber(sn);
							ProfileWorker profileWorker = new ProfileWorker(profileFolder, sn);

							try { if(!profileWorker.exists()) return null; } catch (IOException e) { logger.catching(Level.DEBUG, e); return null; }

							profileWorker.getDescription().ifPresent(serialNumber::setDescription);

							final BtrWorkOrder btrWorkOrder = workOrderRepository.findByNumber(wo)

									.orElseGet(
											()->{
												final BtrWorkOrder workOrder = new BtrWorkOrder();
												workOrder.setNumber(wo);
												return workOrderRepository.save(workOrder);
											});

							serialNumber.setWorkOrderId(btrWorkOrder.getId());
							serialNumber.setWorkOrder(btrWorkOrder);
							final Set<BtrSerialNumber> sns = Optional.ofNullable(btrWorkOrder.getBtrSerialNumbers())
									.orElseGet(
											()->{
												Set<BtrSerialNumber> list = new HashSet<>();
												btrWorkOrder.setBtrSerialNumbers(list);
												return list;
											});
							sns.add(serialNumber);
							return serialNumberRepository.save(serialNumber);
						});

		Optional.ofNullable(btrSerialNumber).ifPresent(s->model.addAttribute("workOrders", Collections.singletonList(s.getWorkOrder())));

		return "btr :: btrCards";
    }
}
