package irt.components.controllers.calibration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import irt.components.beans.irt.calibration.InitializeSetting;
import irt.components.beans.jpa.IrtArray;
import irt.components.beans.jpa.IrtArrayId;
import irt.components.beans.jpa.repository.IrtArrayRepository;
import irt.components.controllers.calibration.CalibrationRestController.Message;
import irt.components.services.converter.InitializeSettingConverter;

@RestController
@RequestMapping("calibration/rest/initialize")
public class InitializeController {
	private final static Logger logger = LogManager.getLogger();

	@Autowired private IrtArrayRepository	arrayRepository;

	@PostMapping("data")
	Map<String, Object> initializeData(@RequestParam String sn, Integer moduleId, String deviceId) {
    	logger.traceEntry("sn: {}; moduleId: {}; deviceId: {}", sn, moduleId, deviceId);

    	final Map<String, Object> map = new HashMap<>();
    	arrayRepository.findById(new IrtArrayId("initialize", deviceId)).map(IrtArray::getDescription).map(d->new InitializeSettingConverter().convertToEntityAttribute(d))
    	.ifPresent(
    			setting->{
    				map.put("setting", setting);
    				Optional.ofNullable(setting.getRegIndex())
    				.ifPresent(regIndex->{
    					try {

    						final String str = CalibrationRestController.diagnosticsReg(sn, moduleId, regIndex);
							map.put("regs", str);

    					} catch (IOException e) {
    						if(logger.getLevel().compareTo(Level.ERROR)>0)
    							logger.warn(e.getLocalizedMessage());
    						else
    							logger.catching(e);
						}
    				});
    			});
    	return map;
    }

	private final int dac1 = 0x10000;
	private final String dac1Str = "DAC1:";
	@PostMapping("initialize/reg-addr-val")
	Object initialize(@RequestParam String sn, Integer moduleId, String deviceId) throws IOException {
    	logger.traceEntry("sn: {}; moduleId: {}; deviceId: {}", sn, moduleId, deviceId);

    	final Optional<IrtArray> oIrtArray = arrayRepository.findById(new IrtArrayId("initialize", deviceId));
    	if(!oIrtArray.isPresent())
    		return "There are no settings for this device.\nCall Roman to fix this.";

    	final String description = oIrtArray.get().getDescription();
    	if(description==null || description.isEmpty()) {
    		logger.warn("THe data is empty.");
    		return "";
    	}
		final InitializeSetting setting = new InitializeSettingConverter().convertToEntityAttribute(description);
		final Integer regIndex = setting.getRegIndex();
		if(setting.getRegIndex()==null || regIndex<0)
			return "";

		final String str = CalibrationRestController.diagnosticsReg(sn, moduleId, regIndex);
    	final Set<Entry<String, Integer>> entrySet = setting.getNameValue().entrySet();
    	final List<Pair<Integer, Integer>> addrVal = new ArrayList<>();

    	final AtomicInteger reg = new AtomicInteger();
    	try(Scanner scanner = new Scanner(str)){
    		while(scanner.hasNextLine()) {
    			final String nextLine = scanner.nextLine().trim();
    			if(nextLine.startsWith(dac1Str))
    				reg.set(dac1);
    			entrySet.parallelStream().filter(e->nextLine.startsWith(e.getKey())).findAny()
    			.ifPresent(e->{
    				final int split = Integer.parseInt(nextLine.split("0x", 2)[1].split("\\)", 2)[0], 16);
    				addrVal.add(Pair.of(reg.get() + split, e.getValue()));
    			});
    		}
    		Map<String, Object> map = new HashMap<>();
    		map.put("regIndex", setting.getRegIndex());
    		map.put("addrVal", addrVal);
			return map;
    	}
     }

	@PostMapping("initialize/save")
	Message initializeSave(@RequestBody InitializeSetting setting) {
    	logger.traceEntry("setting: {}", setting);

    	if(setting.getDeviceId()==null || setting.getDeviceId().isEmpty())
        	return new Message("Values ​​cannot be saved.\nDevice ID is missing.");

    	final IrtArrayId irtArrayId = new IrtArrayId("initialize", setting.getDeviceId());
    	final IrtArray irtArray = arrayRepository.findById(irtArrayId).orElseGet(()->new IrtArray(irtArrayId, ""));
    	irtArray.setDescription(new InitializeSettingConverter().convertToDatabaseColumn(setting));
    	arrayRepository.save(irtArray);
    	return new Message("");
    }

}
