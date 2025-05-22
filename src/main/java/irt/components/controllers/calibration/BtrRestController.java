package irt.components.controllers.calibration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Header;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import irt.components.beans.Btr;
import irt.components.beans.IrtMessage;
import irt.components.beans.OneCeHeader;
import irt.components.beans.OneCeUrl;
import irt.components.beans.UserPrincipal;
import irt.components.beans.jpa.User;
import irt.components.beans.jpa.btr.BtrMeasurements;
import irt.components.beans.jpa.btr.BtrPowerDetector;
import irt.components.beans.jpa.repository.btr.BtrMeasurementsRepository;
import irt.components.beans.jpa.repository.btr.BtrPowerDetectorRepository;
import irt.components.workers.HttpRequest;

@RestController
@RequestMapping("btr/rest")
public class BtrRestController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.btr.templates}")
	private String templates;

	@Value("${irt.url.travelers}")
	private String urlTravelers;

	@Autowired private BtrMeasurementsRepository measurementsRepository;
	@Autowired private BtrPowerDetectorRepository	 powerDetectorRepository;

	@Autowired private OneCeUrl oneCeApiUrl;

	@PostConstruct
	public void postConstruct() {
        try {

    		// get system name
        	String sName = InetAddress.getLocalHost().getHostName();

        	// Change the directory if server runs on my computer
        	if(sName.equals("oleksandr"))
        		templates = "C:\\irt\\btr\\templates";

        } catch (UnknownHostException e) {
			logger.catching(e);
		}
	}

	@PostMapping("save")
	IrtMessage save(@RequestBody Btr btr, Principal principal) {
		logger.traceEntry("{}", btr);

		if(!(principal instanceof UsernamePasswordAuthenticationToken))
			return  new IrtMessage("To save the measurement data, you must be logged in.\n Log In and try again.");

		final Object pr = ((UsernamePasswordAuthenticationToken)principal).getPrincipal();
		final User user = ((UserPrincipal)pr).getUser();

		String url = "http://www.irttechnologies.com/rest/serial-number/get-id?serialNumber=" + btr.getSerialNumber();
		try {

			final String id = HttpRequest.getForString(url, 3, TimeUnit.SECONDS);
			if(id.isEmpty())
				return new IrtMessage("Failed to get serial number ID. Refresh the page and try again.");

			final long snId = Long.parseLong(id);
			final List<BtrMeasurements> btrMeasurements = measurementsRepository.findBySerialNumberId(snId);
			if(btrMeasurements.isEmpty()) {
				final BtrMeasurements meas = new BtrMeasurements(snId, btr.getData(), user.getId());
				measurementsRepository.save(meas);
				return new IrtMessage("");
			}

			final BtrMeasurements meas = btrMeasurements.parallelStream().max(Comparator.comparing(m -> m.getDate())).get();
			final Date now = new Date();
			final Date date = meas.getDate();
			final long days = TimeUnit.DAYS.convert(now.getTime() - date.getTime(), TimeUnit.MILLISECONDS);

			if(days>30) {

				final BtrMeasurements m = new BtrMeasurements(snId, btr.getData(), user.getId());
				measurementsRepository.save(m);
				return new IrtMessage("");

			}else {

				meas.setMeasurement(btr.getData());
				meas.setUserId(user.getId());
				measurementsRepository.save(meas);
				return new IrtMessage("");
			}

		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.catching(Level.DEBUG, e);
			return  new IrtMessage("TimeoutException.\nTry again.");
		}
	}

	@PostMapping("template/upload")
	String uploadTemplate(@RequestParam String sn, Boolean localPN, MultipartFile file) throws IllegalStateException, IOException, InterruptedException, ExecutionException, TimeoutException {
		logger.traceEntry("sn: {}; localPN: {};", sn, localPN);

		final boolean local = Optional.ofNullable(localPN).orElse(false);
		final OneCeHeader oneCeHeader = OneCeRestController.getOneCHeader(oneCeApiUrl, sn).get(5, TimeUnit.SECONDS);

		if(oneCeHeader==null) {
			logger.error("Something went wrong. Unable to contact the !C.");
			return "Something went wrong. Unable to contact the !C.";
		}

		final File f = new File(templates, local ? oneCeHeader.getProduct() : oneCeHeader.getSalesSKU());
		if(!f.exists())
			f.mkdir();

		final File toSave = new File(f, oneCeHeader.getSalesSKU() + ".xlsx");
		file.transferTo(toSave);

		return  "The Template has been loaded.";
	}

	private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	@GetMapping("template")
	ResponseEntity<Resource> getBtr(@RequestParam String sn, Long measId) throws FileNotFoundException, IOException, InterruptedException, ExecutionException, TimeoutException{
		logger.traceEntry("sn: {}; measId: {};", sn, measId);

		final FutureTask<OneCeHeader> ftOneCHeader = OneCeRestController.getOneCHeader(oneCeApiUrl, sn);
		final List<NameValuePair> params = new ArrayList<>();
		final String serialNumber = sn.replaceAll("\\D", "");
		params.add(new BasicNameValuePair("sn", serialNumber));
		final BasicNameValuePair section = new BasicNameValuePair("section", "converter-tuning");
		params.add(section);
		String url = oneCeApiUrl.createUrl("travelers", params);
		final FutureTask<String> ftConverter = HttpRequest.getForStringFT(url);

		params.remove(section);
		final FutureTask<String> ftUnit = getUnitTuningFT(serialNumber);

		final OneCeHeader oneCeHeader = ftOneCHeader.get(5, TimeUnit.SECONDS);
		final String pn = oneCeHeader.getSalesSKU();

		HttpHeaders headers = new HttpHeaders();
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");
		headers.add("Content-Disposition", "inline; filename=\"" + pn + '_' + sn + ".xlsx" + "\"");

		final File f = Optional.of(new File(templates, oneCeHeader.getProduct())).filter(File::exists).orElseGet(()->new File(templates, oneCeHeader.getSalesSKU()));
		if(!f.exists())
			return ResponseEntity.notFound().headers(headers).build();

		final File[] listFiles = f.listFiles();
		if(listFiles.length==0)
			return ResponseEntity.notFound().headers(headers).build();

		if(listFiles.length>1)
			logger.warn("More than one file found.");

		InputStream is;
		final File file = listFiles[0];
		try(	final FileInputStream fis = new FileInputStream(file);
				final Workbook workbook  = WorkbookFactory.create(fis);
				final ByteArrayOutputStream bos = new ByteArrayOutputStream();){

			final Map<String, Name> names = new HashedMap<>();
			workbook.getAllNames().forEach(
					n->{
						final String name = n.getNameName().replace("\\", "");
						names.put(name, n);
					});

			measurementsRepository.findById(measId)
			.ifPresent(
					meas->{

						final Sheet sheet = workbook.getSheetAt(0);

						final Map<String, String> measurement = meas.getMeasurement();
						addToMeasurement(measurement, ftConverter);
						addToMeasurement(measurement, ftUnit);

						powerDetectorRepository.findById(Long.parseLong(serialNumber)).map(BtrPowerDetector::getMeasurement).map(Map::entrySet).map(Set::stream).orElse(Stream.empty()).map(Map.Entry::getValue).forEach(measurement::putAll);

						measurement.entrySet()
						.forEach(
								m->{
									final String key = m.getKey();
									final Name name = names.get(key);
									if(name==null) {
										logger.warn("{} does not contain this name: {}", file, m);
										return;
									}

									final CellReference cr = new CellReference(name.getRefersToFormula());
									final String value = m.getValue();

									if(value.isEmpty())
										return;

									final Cell cell = sheet.getRow(cr.getRow()).getCell(cr.getCol());
									Double val = null; 
									if(value.replaceAll("[\\d\\.-]", "").isEmpty()) {
										val = Double.parseDouble(value);
										cell.setCellValue(val);
									}else
										cell.setCellValue(value);

									final String n = name.getNameName();
									final String[] split = n.split("\\.");
									final String[] cellRange = getCellValue(workbook, (split.length>1 ? split[1] : split[0]) + "_range", file.getName());
									if(cellRange.length!=0 && val!=null) {
										final double cellVal = val;

										final Map<String, List<String>> collect = Arrays.stream(cellRange).collect(Collectors.groupingBy(collect()));

										// Minimum
										final Optional<List<String>> oMin = Optional.ofNullable(collect.get("min"));
										oMin.filter(l->!l.isEmpty()).map(l->l.get(0)).map(v->v.replaceAll("[^\\d.]", "")).filter(v->!v.isEmpty()).map(Double::parseDouble)
										.ifPresent(min->{
											if(Double.compare(cellVal, min)<0) 
												setCellBackground(workbook, cell);
										});
										if(oMin.isPresent())
											return;

										// Maximum
										final Optional<List<String>> oMax = Optional.ofNullable(collect.get("max"));
										oMax.filter(l->!l.isEmpty()).map(l->l.get(0)).map(v->v.replaceAll("[^\\d.-]", "")).filter(v->!v.isEmpty()).map(Double::parseDouble)
										.ifPresent(max->{
											if(Double.compare(cellVal, max)>0) 
												setCellBackground(workbook, cell);
										});
									}
								});

						// Date
						final Optional<Name> oDateName = Optional.ofNullable(names.get("measurenentDate"));
						oDateName.map(Name::getRefersToFormula).map(CellReference::new)
						.ifPresent(cr->sheet.getRow(cr.getRow()).getCell(cr.getCol()).setCellValue(df.format(meas.getDate())));
						if(!oDateName.isPresent())
							logger.warn("The EXCEL file does not contain this name: measurenentDate");

						// User
						final Optional<Name> oUserName = Optional.ofNullable(names.get("user"));
						oUserName.map(Name::getRefersToFormula).map(CellReference::new)
						.ifPresent(
								cr->{
									final User u = meas.getUser();
									final String value = u.getFirstname().charAt(0) + "." + u.getLastname().charAt(0) + '.';
									sheet.getRow(cr.getRow()).getCell(cr.getCol()).setCellValue(value);
								}
						);
						if(!oUserName.isPresent())
							logger.warn("The EXCEL file does not contain this name: user");

						final Header header = sheet.getHeader();
						final String left = header.getLeft().replace("${serialNumber}", sn);
						header.setLeft(left);

						workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();

						try {
							workbook.write(bos);
						} catch (IOException e) {
							logger.catching(e);
						}
					});
			is = new ByteArrayInputStream(bos.toByteArray());
		}
		
		return ResponseEntity.ok()
				.headers(headers)
				.contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
				.body(new InputStreamResource(is));
	}

	@GetMapping("header")
	public Message<String> getHeader(@RequestParam String sn) throws InterruptedException, ExecutionException, TimeoutException {
		final List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("sn", sn.replaceAll("\\D", "")));
		params.add(new BasicNameValuePair("section", "header"));
		String url = oneCeApiUrl.createUrl("travelers", params);
		final String string = HttpRequest.getForStringFT(url).get(3, TimeUnit.SECONDS);
	
		return new Message<String>() {

			@Override
			public String getPayload() {
				return string;
			}

			@Override
			public MessageHeaders getHeaders() {
				Map<String, Object> header = new HashMap<>();
				header.put("section", "unit-header");
				header.put("serial-number", sn);
				return new MessageHeaders(header);
			}
		};
	}

	@GetMapping("unit-tuning")
	public Message<String> getUnitTuning(@RequestParam String sn) throws InterruptedException, ExecutionException, TimeoutException{
		final String string = getUnitTuningFT(sn).get(3, TimeUnit.SECONDS);
		return new Message<String>() {

			@Override
			public String getPayload() {
				return string;
			}

			@Override
			public MessageHeaders getHeaders() {
				Map<String, Object> header = new HashMap<>();
				header.put("section", "unit-tuning-imd-3");
				header.put("serial-number", sn);
				return new MessageHeaders(header);
			}};
	}

	public FutureTask<String> getUnitTuningFT(String serialNumber) {
		final List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("sn", serialNumber.replaceAll("\\D", "")));
		params.add(new BasicNameValuePair("section", "unit-tuning-imd-3"));
		String url = oneCeApiUrl.createUrl("travelers", params);
		return HttpRequest.getForStringFT(url);
	}

	private Function<String, String> collect() {
		return str->{

			if(str.contains("min"))
				return "min";

			if(str.contains("max"))
				return "max";

			if(str.contains("nom") || str.contains("typ"))
				return "typ";

			return "other";
		};
	}

	private void setCellBackground(final Workbook workbook, final Cell cell) {
		CellStyle style = workbook.createCellStyle();
		Color color = new XSSFColor(new byte[] {(byte) 245, (byte) 193, (byte) 205});
		style.setFillForegroundColor(color);
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		CellStyle cellStyle = cell.getCellStyle();
		style.setBorderBottom(cellStyle.getBorderBottom());
		style.setBorderLeft(cellStyle.getBorderLeft());
		style.setBorderRight(cellStyle.getBorderRight());
		style.setBorderTop(cellStyle.getBorderTop());
		style.setDataFormat(cellStyle.getDataFormat());
		style.setAlignment(cellStyle.getAlignment());
		cell.setCellStyle(style);
	}

	private void addToMeasurement(final Map<String, String> measurement, final FutureTask<String> ftConverter) {
		logger.traceEntry("{}", measurement);
		try {

			final String string = ftConverter.get(5, TimeUnit.SECONDS);
			logger.debug(string);
			Map<String, String> map = stringToMap(string);
			measurement.putAll(map);

		} catch (InterruptedException | ExecutionException | TimeoutException | JsonProcessingException e) {
			logger.catching(e);
		}
	}

	@GetMapping("template/gain")
	String[] gainFromTemplate(@RequestParam String pn) throws FileNotFoundException, IOException{
		logger.traceEntry("pn: {}" , pn);

		File file = new File(templates, pn);
		if(!file.exists()) 
			return null;

		final File[] listFiles = file.listFiles();
		if(listFiles.length==0)
			return null;

		try(	final FileInputStream fis = new FileInputStream(listFiles[0]);
				final Workbook workbook  = WorkbookFactory.create(fis);){

			return getCellValue(workbook, "Gain_range", file.getName());
		}
	}

	@PostMapping("pd/save")
	 public IrtMessage savePowerDetector(@RequestBody BtrPowerDetector pd, Principal principal){
		logger.traceEntry("{}", pd);

		if(!(principal instanceof UsernamePasswordAuthenticationToken))
			return  new IrtMessage("To save the data, you must be logged in.\n Log In and try again.");

		pd.setDate(new Date());
		powerDetectorRepository.save(pd);

		return new IrtMessage("");
	}

	public String[] getCellValue(final Workbook workbook, final String name, String fileName) {
		final Optional<Name> oName = Optional.ofNullable(workbook.getName(name));
		if(!oName.isPresent()){
			logger.warn("The template " + fileName + " do not contains cell name '" + name + "'.");
			return new String[0];
		}

		final Name cellName = oName.get();
		final CellReference cr = new CellReference(cellName.getRefersToFormula());
		final Cell cell = workbook.getSheetAt(0).getRow(cr.getRow()).getCell(cr.getCol());
		final String[] split = cell.getStringCellValue().split("\\s{2,40}");

		return split;
	}

	public static Map<String, String> stringToMap(String jSon) throws JsonProcessingException, JsonMappingException {
		logger.traceEntry(jSon);

		final List<Map<String, String>> value = new ObjectMapper().readValue(jSon, new TypeReference<List<Map<String,String>>>() {});

        if(value.isEmpty())
        	return new HashMap<>();

        final Map<String, String> map = value.get(0);
        map.remove("Component");
        map.remove("Setting");

         Optional.ofNullable(map.remove("IMD")).map(s->(s.contains(":") ? s.split(":")[1] : s).trim()).map(s->s.split("[;/, ]+"))
         .ifPresent(
        		 a->{
        			 for(int i=0; i<a.length; i++) {

        				 final String trim = a[i].trim();
        				 if(trim.isEmpty())
        					 continue;

        				 String v = "err";
        		         try {

							v = Long.toString(Math.round(Double.valueOf(trim)));

        		         } catch (NumberFormatException e) {
							logger.catching(e);
        		         }

        		         map.put(".IMD." + i, v);
        			 }
        			 logger.debug(map);
        		});

        Optional.ofNullable(map.remove("Notes")).map(s->s.split("\\n"))
        .ifPresent(
        		s->{
        			final List<String> collect = Arrays.stream(s).filter(m->m.toUpperCase().contains("MONITOR")).map(m->m.split("[:=]", 2)).filter(m->m.length>1).map(m->m[1].trim().split("[;/, ]+"))
        					.flatMap(m->Arrays.stream(m)).map(m->m.replaceAll("[^\\d.-]", "")).collect(Collectors.toList());

        			if(collect.isEmpty())
        				logger.warn("Notes: {} -> {};", s.length, s);

        			for(int i=0; i<collect.size();i++) {
        				String v = "err";
        				try {

        					v = Double.toString(Double.valueOf(collect.get(i)));

        				} catch (NumberFormatException e) {
        					logger.catching(e);
        				}

        				map.put(".Monitor." + i, v);
        			}
        		});

        return logger.traceExit(map);
	}
}
