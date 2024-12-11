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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.map.HashedMap;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import irt.components.beans.Btr;
import irt.components.beans.IrtMessage;
import irt.components.beans.UserPrincipal;
import irt.components.beans.jpa.User;
import irt.components.beans.jpa.btr.BtrMeasurements;
import irt.components.beans.jpa.repository.btr.BtrMeasurementsRepository;
import irt.components.workers.HttpRequest;

@RestController
@RequestMapping("btr/rest")
public class BtrRestController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.btr.templates}")
	private String templates;

	@Autowired private BtrMeasurementsRepository measurementsRepository;

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

		String url = "http://irttechnologies.com/rest/serial-number/get-id?serialNumber=" + btr.getSerialNumber();
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
	String uploadTemplate(@RequestParam String pn, MultipartFile file) throws IllegalStateException, IOException {
		logger.traceEntry("{}", pn);

		final File f = new File(templates, pn);
		if(!f.exists())
			f.mkdir();

		final File toSave = new File(f, pn + ".xlsx");
		file.transferTo(toSave);

		return  "The Template has been loaded.";
	}

	private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	@GetMapping("template")
	ResponseEntity<Resource> getBtr(@RequestParam String sn, Long measId, String pn) throws FileNotFoundException, IOException{
		logger.traceEntry("sn: {}; measId: {}; pn: {};", sn, measId, pn);

		HttpHeaders headers = new HttpHeaders();
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");
		headers.add("Content-Disposition", "inline; filename=\"" + pn + '_' + sn + ".xlsx" + "\"");

		final File f = new File(templates, pn);
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

						meas.getMeasurement().entrySet()
						.forEach(
								m->{
									final String key = m.getKey();
									final Name name = names.get(key);
									if(name==null) {
										logger.warn("The EXCEL file does not contain this name: {}", m);
										return;
									}

									final CellReference cr = new CellReference(name.getRefersToFormula());
									final String value = m.getValue();
									final Cell cell = sheet.getRow(cr.getRow()).getCell(cr.getCol());
									Double val = null; 
									if(value.replaceAll("[\\d\\.]", "").isEmpty()) {
										val = Double.parseDouble(value);
										cell.setCellValue(val);
									}else
										cell.setCellValue(value);

									final String[] split = name.getNameName().split("\\.");
									final String[] cellValue = getCellValue(workbook, split[1] + "_range", file.getName());
									if(cellValue.length!=0 && val!=null) {
										final double cellVal = val;
										Arrays.stream(cellValue).filter(v->v.contains("min") || v.contains("nom")).findAny().map(v->v.replaceAll("[^\\d.]", "")).filter(v->!v.isEmpty()).map(Double::parseDouble)
										.ifPresent(min->{
											if(Double.compare(cellVal, min)<0) {
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
										});
									}
								});

						// Date
						final Name measurenentDate = names.get("measurenentDate");
						CellReference cr = new CellReference(measurenentDate.getRefersToFormula());
						sheet.getRow(cr.getRow()).getCell(cr.getCol()).setCellValue(df.format(meas.getDate()));

						// User
						final Name user = names.get("user");
						cr = new CellReference(user.getRefersToFormula());
						final User u = meas.getUser();
						final String value = u.getFirstname().charAt(0) + "." + u.getLastname().charAt(0) + '.';
						sheet.getRow(cr.getRow()).getCell(cr.getCol()).setCellValue(value);

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
}
