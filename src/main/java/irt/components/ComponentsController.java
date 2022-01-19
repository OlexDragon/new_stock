package irt.components;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ComponentsController {

    @RequestMapping("/")
    String componentSearch() {
        return "components";
    }
}
