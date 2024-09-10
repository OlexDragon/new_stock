package irt.components.controllers.calibration;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("calibration/btr")
public class BtrController {
	private final static Logger logger = LogManager.getLogger();

	@GetMapping
    String modalBtr(@RequestParam String sn, Model model) throws IOException {

		return "calibration/btr_table :: modal";
    }

}
