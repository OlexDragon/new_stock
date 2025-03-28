package irt.components.controllers.calibration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.management.InvalidApplicationException;
import javax.script.ScriptException;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import irt.components.beans.CurrentAlias;
import irt.components.beans.CurrentOffset;
import irt.components.beans.irt.HWInfo;
import irt.components.beans.irt.Info;
import irt.components.beans.irt.calibration.CurrentToSave;
import irt.components.beans.irt.calibration.ModuleInfo;
import irt.components.beans.jpa.IrtArray;
import irt.components.beans.jpa.IrtArrayId;
import irt.components.beans.jpa.calibration.CurrentLayout;
import irt.components.beans.jpa.repository.IrtArrayRepository;
import irt.components.beans.jpa.repository.calibration.CurrentLayoutRepository;
import irt.components.controllers.calibration.CalibrationRestController.PostFor;
import irt.components.services.converter.CurrentAliastListConverter;
import irt.components.workers.HttpRequest;
import irt.components.workers.ThreadRunner;

@RestController
@RequestMapping("calibration/rest/current")
public class CurrentRestController {
	private final static Logger logger = LogManager.getLogger();
	private final static String LINE_SEPARATOR = System.getProperty("line.separator");

	@Value("${irt.perl.path}")
	private String perlPath;

	@Value("${irt.perl.script.path}")
	private String perlScriptPath;

	@Value("${irt.perl.script.argument}")
	private String perlScriptArgument;

	@Autowired private IrtArrayRepository	arrayRepository;
	@Autowired private CurrentLayoutRepository	layoutRepository;

	@PostMapping("module-info")
	List<ModuleInfo> currentDbLayout(@RequestParam String sn, String topId) throws IOException, ScriptException{
		logger.traceEntry("sn: {}; topId: {}", sn, topId);

		final List<ModuleInfo> list = new ArrayList<>();
		try {
			HttpRequest.getAllModules(sn)
			.forEach(
					(n,i)->{

						final HWInfo hwInfo = CalibrationRestController.diagnostics(HWInfo.class, sn, "hwinfo", i.toString(), "4", null, null, PostFor.IRT_OBJECT);
						logger.debug("n: {}; i: {}; hwInfo: {};", n, i, hwInfo);
						if(hwInfo==null || hwInfo.getSequence() == null || hwInfo.getSequence().isEmpty())
							return;

						final ModuleInfo moduleInfo = new ModuleInfo();
						moduleInfo.setName(n);
						moduleInfo.setIndex(i);
						moduleInfo.setHwInfo(hwInfo);
						try {

							final Info info = CalibrationController.getInfo(sn, i);
							moduleInfo.setInfo(info);
							final int deviceType = info.getDeviceType();
							final int typeVersion = info.getTypeVersion();
							final String moduleId = deviceType + "." + typeVersion;

							final List<CurrentLayout> layout = list.stream().filter(inf->inf.getInfo().getDeviceId().equals(moduleId)).map(inf->inf.getLayout()).findAny().orElseGet(()->currentLayout(sn, topId, moduleId));
							moduleInfo.setLayout(layout);

						} catch (InterruptedException | ExecutionException | TimeoutException e) {
							logger.catching(Level.DEBUG, e);
						} catch (MalformedURLException e) {
							logger.catching(e);
						}
						
						list.add(moduleInfo);
					});
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.catching(Level.DEBUG, e);
		}
		logger.debug(list);

		return list;
	}

	@PostMapping("layout")
	List<CurrentLayout> currentLayout(@RequestParam String sn, String topId, String moduleId) {
    	logger.traceEntry("sn: {}; topId: {}; moduleId: {}", sn, topId, moduleId);

    	final List<CurrentLayout> specialLayouts = layoutRepository.findByTopIdAndModuleIdOrderByCreationDateDesc(sn,  moduleId);

    	if(!specialLayouts.isEmpty())
    		return specialLayouts;	// Special

		return layoutRepository.findByTopIdAndModuleIdOrderByCreationDateDesc(topId,  moduleId);	
	}

	@PostMapping("url")
	IrtArray currentUrl(@RequestParam String moduleId, String url) {
    	logger.traceEntry("moduleId: {}; url: {};", moduleId, url);

    	final IrtArrayId id = new IrtArrayId("current_url", moduleId);
    	final IrtArray irtArray =  arrayRepository.findById(id).orElseGet(()->new IrtArray(id, url));
		return arrayRepository.save(irtArray);
	}

	@PostMapping("layout/save")
	CurrentLayout currentSave(@RequestBody CurrentToSave toSend) {
    	logger.traceEntry("toSend: {};", toSend);

    	CurrentLayout layout;

    	if(toSend.isSpecial()) {
    		final List<CurrentLayout> list = layoutRepository.findByTopIdAndModuleIdOrderByCreationDateDesc(toSend.getSerialNumber(), toSend.getModuleId());
    		if(list.isEmpty())
    			layout = new CurrentLayout(toSend.getSerialNumber(), toSend.getModuleId(), toSend.getLayouts());
    		else {
    			layout = list.get(0);
    			layout.setLayouts(toSend.getLayouts());
    		}

    	}else {

    		final List<CurrentLayout> list = layoutRepository.findByTopIdAndModuleIdOrderByCreationDateDesc(toSend.getTopId(), toSend.getModuleId());

    		if(list.isEmpty())
    			layout = new CurrentLayout(toSend.getTopId(), toSend.getModuleId(),  toSend.getLayouts());

    		else {

    			final CurrentLayout tmp = list.get(0); // get Last saved
    			final long duration = new Date().getTime() - tmp.getCreationDate().getTime();

    			if(TimeUnit.DAYS.convert(duration, TimeUnit.MILLISECONDS) < 21) {
					layout = tmp;
					layout.setLayouts(toSend.getLayouts());
    			}else
	    			layout = new CurrentLayout(toSend.getTopId(), toSend.getModuleId(),  toSend.getLayouts());
    		}
    	}

    	return layoutRepository.save(layout);	
	}

	private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
	@PostMapping("delete")
	String deleteLayout(@RequestParam String topId, String moduleId, String date) {
    	logger.traceEntry("topId: {}; moduleId: {}; date: {};", topId, moduleId, date);

		final List<CurrentLayout> list = layoutRepository.findByTopIdAndModuleIdOrderByCreationDateDesc(topId, moduleId);
		final List<CurrentLayout> filter = list.parallelStream().filter(cl->format.format(cl.getCreationDate()).equals(date)).collect(Collectors.toList());
		switch(filter.size()) {

		case 0:
			return "The requested layout was not found in the database.";

		case 1:
			break;
		default:
			return "Layout not removed.\nMore than one layout found.";
		}

		final CurrentLayout currentLayout = filter.get(0);
		layoutRepository.delete(currentLayout);

		return "";	
	}

	private final static String CURRENT_ALIAS = "current-alias";
	@PostMapping("alias")
	List<CurrentAlias> currentAlias(@RequestParam String topId, String moduleId) {
    	logger.traceEntry("topId: {}; moduleId: {};", topId, moduleId);

    	final String dbId = dbId(topId, moduleId);
		final CurrentAliastListConverter listConverter = new CurrentAliastListConverter();
    	final IrtArrayId id = new IrtArrayId(CURRENT_ALIAS, dbId);
		return arrayRepository.findById(id).map(IrtArray::getDescription).map(listConverter::convertToEntityAttribute).orElse(null);
	}

	public String dbId(String topId, String moduleId) {
		return topId.equals(moduleId) ? topId : topId + " : " + moduleId;
	}

	@PostMapping("save-alias")
	List<CurrentAlias> currentSaveAlias(@RequestParam String topId, String moduleId, String nameInProfile, String nameToShow) {
    	logger.traceEntry("topId: {}; moduleId: {}; nameInProfile: {}; nameToShow: {};", topId, moduleId, nameInProfile, nameToShow);

    	final String dbId = dbId(topId, moduleId);
    	final IrtArrayId id = new IrtArrayId(CURRENT_ALIAS, dbId);
		final Optional<IrtArray> oIrtArray = arrayRepository.findById(id);
		final IrtArray irtArray;

		if(oIrtArray.isPresent()) {
			irtArray = oIrtArray.get();
		}else
			irtArray =  new IrtArray(id, "[]");

		final CurrentAliastListConverter listConverter = new CurrentAliastListConverter();
		final String tmp = irtArray.getDescription();
		final List<CurrentAlias> list = listConverter.convertToEntityAttribute(tmp);
		final Optional<CurrentAlias> oCurrentAlias = list.parallelStream().filter(a->a.getNameInProfile().equals(nameInProfile)).findAny();
		if(oCurrentAlias.isPresent()) {
			final CurrentAlias ca = oCurrentAlias.get();
			ca.setNameToShow(nameToShow);
		}else {
			final CurrentAlias ca = new CurrentAlias(nameInProfile, nameToShow);
			list.add(ca);
		}

		final String column = listConverter.convertToDatabaseColumn(list);
		irtArray.setDescription(column);
		arrayRepository.save(irtArray);

		return list;	
	}

	@PostMapping("delete-alias")
	String currentDeleteAlias(@RequestParam String topId, String moduleId, String nameInProfile) {
    	logger.traceEntry("topId: {}; moduleId: {}; nameInProfile: {};", topId, moduleId, nameInProfile);

    	final String dbId = dbId(topId, moduleId);
    	final IrtArrayId id = new IrtArrayId(CURRENT_ALIAS, dbId);
		final Optional<IrtArray> oIrtArray = arrayRepository.findById(id);

		if(!oIrtArray.isPresent()) 
			return "No data for " + moduleId;

		final CurrentAliastListConverter listConverter = new CurrentAliastListConverter();
		final IrtArray irtArray = oIrtArray.get();

		final String tmp = irtArray.getDescription();
		final List<CurrentAlias> list = listConverter.convertToEntityAttribute(tmp);
		final List<CurrentAlias> l = list.parallelStream().filter(ca->!ca.getNameInProfile().equals(nameInProfile)).collect(Collectors.toList());
		if(l.size()==list.size())
			return "No data for " + nameInProfile;

		irtArray.setDescription(listConverter.convertToDatabaseColumn(l));
		arrayRepository.save(irtArray);

		return "The data for " + nameInProfile + " has been deleted";
	}

    @PostMapping("offset")
    List<CurrentOffset> currentOffset(@RequestParam String sn, @RequestParam Boolean local, HttpServletResponse response) throws UnknownHostException, InvalidApplicationException {
    	logger.traceEntry("Serial Number: {}; local: {}", sn, local);

    	final InetAddress byName = InetAddress.getByName(sn);
    	final byte[] bytes = byName.getAddress();
    	final int length = bytes.length;
    	final List<CurrentOffset> offsets = new ArrayList<>();

    	if(length!=4) {
    		final CurrentOffset co = new CurrentOffset("ERROR");
    		co.getOffsets().add("Unable to get an IP address.");
    		logger.warn("{} - Unable to get an IP address. {}", sn, bytes);
			offsets.add(co);
    		return offsets;
    	}

    	final String ipAddress = IntStream.range(0, length).map(index->bytes[index]&0xff).mapToObj(Integer::toString).collect(Collectors.joining("."));
		logger.debug("ipAddress: {}", ipAddress);

    	 try {

    		 List<String> commands = new ArrayList<>();
    		 commands.add(perlPath);
    		 commands.add(perlScriptPath);
    		 commands.add(perlScriptArgument + ipAddress);
    		 if(local)
    			 commands.add("--local=1");

    		 logger.debug("{}", commands);
 
    		 final ProcessBuilder builder = new ProcessBuilder(commands);
    		 builder.redirectErrorStream(true);

    		 final Process process = builder.start();


    		 final FutureTask<Void> ft = new FutureTask<>(()->null);

    		 ThreadRunner.runThread(
    				 ()->{
    					 final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    			            String line;
    			            try {

    			            	CurrentOffset offset = null;
    			            	while ((line = reader.readLine()) != null) {
    			            		logger.debug(line);
    			            		response.setHeader("error-line", line);
    			            		if(!ft.isDone())
    			            			ThreadRunner.runThread(ft);

    			            		if(line.startsWith("[psu_offset.pl->psu_offset.pl")) {
    			            			if(line.endsWith(".bin:")) {
    			            				final CurrentOffset co = Optional.of(line.split("\\s+")).filter(arr->arr.length==2).map(arr->arr[1].substring(0, arr[1].length()-1)).map(CurrentOffset::new).orElse(null);
    			            				if(co!=null) {
    			            					offset = co;
    			            					offsets.add(co);
    			            				}
    			            			}
    			            		}else if(line.startsWith("pm-vmon-offset") || line.startsWith("device-vmon")) {
    			            			final String l = line;
    			            			Optional.ofNullable(offset).ifPresent(os->os.getOffsets().add(l));
    			            		}
								}

    			            } catch (IOException e) {
								logger.catching(e);
							}
    				 });

    		 // Wait for script to start
    		 try {

    			 ft.get(5, TimeUnit.SECONDS);

    		 } catch (ExecutionException | TimeoutException e) {
				logger.catching(Level.DEBUG, e);
			}

    		try(final OutputStream os = process.getOutputStream();){
    			while(process.isAlive()) {
    				os.write(("\n").getBytes());
    				os.flush();
    	    		Thread.sleep(3000);
    			}
    		}

    		final Integer exitValue = process.exitValue();
    		if(exitValue<0) {
        		response.setHeader("error-code", exitValue.toString());
    			throw new InvalidApplicationException("Script Error");
    		}

    		 return offsets.stream().filter(os->!os.getOffsets().isEmpty()).collect(Collectors.toList());

    	 } catch (IOException | InterruptedException e) {
			logger.catching(e);
			final CurrentOffset co = new CurrentOffset("ERROR");
			offsets.add(co);
			final String localizedMessage = e.getLocalizedMessage();

			if(localizedMessage.isEmpty()) {
	    		co.getOffsets().add(e.getClass().getSimpleName());
	    		return offsets;
			}else {
	    		co.getOffsets().add(localizedMessage);
	    		return offsets;
			}
		}
	}

    @PostMapping(path = "save_offset", consumes = MediaType.APPLICATION_JSON_VALUE)
    Pair<String, String> saveCurrentOffset(@RequestBody CurrentOffset offset) {
    	logger.traceEntry("{}", offset);

    	final File file = new File(offset.getPath());
    	final String name = file.getName();

    	if(!file.exists())
    		return Pair.of(name, "File isn't exists.");

    	StringBuilder sb = new StringBuilder();
    	try(Scanner scanner = new Scanner(file);) {

    		while(scanner.hasNextLine()) {

    			final String line = scanner.nextLine();

    			if(line.startsWith("pm-vmon-offset") || (line.startsWith("device-vmon") && hasIndex(line))) {
    				continue;
    			}

    			sb.append(line).append(LINE_SEPARATOR);
    		}

    		offset.getOffsets().forEach(os->sb.append(os).append(LINE_SEPARATOR));

    	} catch (FileNotFoundException e) {
			logger.catching(e);
    		return Pair.of(name, e.getLocalizedMessage());
		}

		try {
			Files.write(file.toPath(), sb.toString().getBytes());
		} catch (IOException e) {
			logger.catching(e);
    		return Pair.of(name, e.getLocalizedMessage());
		}

		return Pair.of(name, "Saved");
    }

    @PostMapping("dac")
    boolean setDac(@RequestParam String sn, Integer channel, Integer index, Integer value, Boolean save, Boolean binary){
    	logger.traceEntry("sn: {};; channel: {}; index: {}; value: {} binary: {}", sn, channel, index, value, binary);

		try {

			final URL url = new URL("http", sn, "/calibration.cgi");
			List<NameValuePair> params = new ArrayList<>();
			final List<BasicNameValuePair> asList = new ArrayList<>();

			if(Optional.ofNullable(binary).filter(b->b).isPresent()) {
				asList.add(new BasicNameValuePair("channel", "deb_reg"));
				asList.add(new BasicNameValuePair("set", "Set"));
				asList.add(new BasicNameValuePair("index", "26"));
				asList.add(new BasicNameValuePair("addr", index.toString()));
				asList.add(new BasicNameValuePair("value", Integer.toBinaryString(value)));
			}else {
				asList.add(new BasicNameValuePair("index", index.toString()));
				asList.add(new BasicNameValuePair("channel", channel.toString()));
				Optional.ofNullable(save).ifPresent(s->asList.add(new BasicNameValuePair("save_dp", "1")));
				Optional.ofNullable(value).ifPresent(s->asList.add(new BasicNameValuePair("value", value.toString())));
			}

			params.addAll(asList);

			HttpRequest.postForIrtObgect(url.toString(), Object.class, params).get(5, TimeUnit.SECONDS);
			return true;

		} catch (MalformedURLException e) {
			logger.catching(e);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.catching(Level.DEBUG, e);
		}

		return false;
    }

    private final static String[] indexis = new String[] {"101", "103", "105", "107", "109", "111"};
    private boolean hasIndex(String line) {

    	final String[] split = line.split("\\s+");
    	if(split.length<5)
    		return false;

    	return Arrays.stream(indexis).filter(split[2]::equals).findAny().map(i->true).orElse(false);
	}
}
