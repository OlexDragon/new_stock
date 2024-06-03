package irt.components.controllers.calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/calibration/rest/fcn")
public class FcmRestConttroller {
	private final static Logger logger = LogManager.getLogger();

    @GetMapping("info")
    public String info() {
		return null;
    }
}
