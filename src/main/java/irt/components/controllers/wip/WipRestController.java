package irt.components.controllers.wip;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import irt.components.beans.wip.WoDetails;
import irt.components.beans.wip.WoValuesToChange;


@RestController
@RequestMapping("wip/rest")
public class WipRestController {
	private final static Logger logger = LogManager.getLogger();


	@Value("${irt.tmp.directory}")
	private String tmpDirectory;

	@Value("${irt.wip.directory}")
	private String wipDirectory;

	@Value("${irt.log.file}")
	private String logFile;

	@PostMapping("files")
	public List<SimpleEntry<String, Long>> getWipFiles() throws IOException {
		logger.traceEntry(wipDirectory);

		final File directory = Paths.get(wipDirectory).toFile();
		final File[] listFiles = directory.listFiles((d,fn)->fn.toLowerCase().startsWith("wip"));
		final List<AbstractMap.SimpleEntry<String, Long>> toSend = new ArrayList<>();

		for(int i=0; i<listFiles.length; i++) {
			File f = listFiles[i];
			toSend.add(new AbstractMap.SimpleEntry<>(f.getName(), Files.readAttributes(f.toPath(), BasicFileAttributes.class).lastModifiedTime().toMillis()));
		}

		return toSend;
	}

	@RolesAllowed("WIP_PAGE")
	@PostMapping("save")
	public List<String> saveChanges(@RequestBody WoValuesToChange toChange) {
		logger.traceEntry("{}", toChange);

		final WoDetails fromWIP = toChange.getFromWIP();
		WoDetails fromLOG = toChange.getFromLOG();

		if(fromLOG==null) {
			toChange.setFromLOG(fromWIP);
			fromLOG = fromWIP;
		}

		final List<String> message = new ArrayList<>();

		final String partNumber = toChange.getPartNumber();
		final String description = toChange.getDescription();
		final String wipPartNumber = fromWIP.getPartNumber();
		final String wipDescription = fromWIP.getDescription();

		logger.debug("\n\t{}\n\t{}\n\t{}\n\t{}\n\t{}\n\t{}", ()->partNumber, ()->wipPartNumber, ()->partNumber.equals(wipPartNumber), ()->description, ()->wipDescription, ()->description.equals(wipDescription));

		if(partNumber.equals(wipPartNumber) && description.equals(wipDescription))
			message.add("All fields of the WIP... file are the same. The file has not been updated.");
		else
			message.add(saveDataToWip(toChange));

		final String logPartNumber = fromLOG.getPartNumber();
		final String logDescription = fromLOG.getDescription();

		if(partNumber.equals(logPartNumber) && description.equals(logDescription))
			message.add("All fields of the LOG file are the same. The file has not been updated.");
		else
			message.add(saveDataToLog(toChange));

		return message;
	}

	private String saveDataToWip(WoValuesToChange toChange) {

		final String wipFile = toChange.getWipFile();
		final File file = Paths.get(wipDirectory, wipFile).toFile();
    	return updateFile(file, toChange, WipController.WIP_WORK_ORDER, WipController.WIP_PART_NUMBER, WipController.WIP_DESCRIPTION);
	}

	private String saveDataToLog(WoValuesToChange toChange) {

		final File file = new File(logFile);
    	return updateFile(file, toChange, WipController.LOG_WORK_ORDER, WipController.LOG_PART_NUMBER, WipController.LOG_DESCRIPTION);
	}

	private String updateFile(final File file, WoValuesToChange toChange, int workOrderIndex, int partNumberIndex, int descriptionIndex) {

		if(!file.exists())
    		return  file + " does not exists.";

		final String fileName = file.getName();

		// copy file to the tmp directory
		try {

			final String[] split = fileName.split("\\.", 2);
			int index = 1;
			File tmpFile = new File(tmpDirectory, fileName);

			while(tmpFile.exists()) {
				tmpFile = new File(tmpDirectory, split[0] + '[' + index + "]." + split[1] );
				++index;
			}


			FileUtils.copyFile(file, tmpFile);

		} catch (Exception e) {
		    logger.catching(e);
		    return e.getLocalizedMessage();
		}

		// save changes
    	try(	InputStream is=new FileInputStream(file);
    			XSSFWorkbook workbook = new XSSFWorkbook(is);){
     
        	List<XSSFRow> rows = new ArrayList<>();
    		XSSFSheet sheet=workbook.getSheetAt(0);

    		int scanTo = 0;
    		final String wo = toChange.getWo();
			for(int nextToRead = sheet.getLastRowNum(); nextToRead > scanTo; nextToRead--) {

    			final XSSFRow row = sheet.getRow(nextToRead);
    			if(row==null)
    				continue;

    			final String workOrder = row.getCell(workOrderIndex, MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue().trim();

				if(workOrder.equals(wo)) {
					rows.add(row);
					scanTo = nextToRead > 5 ? nextToRead - 5 : 0;
				}
    		}

    		final boolean isEmpty = rows.isEmpty();
    		rows = rows.stream().filter(comparePartNumberAndDescription(workOrderIndex==WipController.WIP_WORK_ORDER, toChange, partNumberIndex, descriptionIndex)).collect(Collectors.toList());

    		if(rows.isEmpty()) {

    			if(!isEmpty)
    				return "Update cancelled. Another user has made changes.";

    			return wo + " can not found. (" + fileName + ")";
    		}

    		rows.forEach(
    				row->{
    					row.getCell(partNumberIndex).setCellValue(toChange.getPartNumber());
    					row.getCell(descriptionIndex).setCellValue(toChange.getDescription());
    				});

        	try(	FileOutputStream os = new FileOutputStream(file);
        			BufferedOutputStream bos = new BufferedOutputStream(os)){

        		workbook.write(bos);
        	}

        	return fileName + " has been updated. To refresh this page, close any message.";

    	} catch (Exception e) {
    		logger.catching(e);
			return e.getLocalizedMessage();
		}
	}

	private Predicate<? super XSSFRow> comparePartNumberAndDescription(boolean isWip, WoValuesToChange toChange, int partNumberIndex, int descriptionIndex) {
		return row->{
			final String pn 	= row.getCell(partNumberIndex	, MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue().replaceAll("[\t\n\\s+]", "");
			final String descr 	= row.getCell(descriptionIndex	, MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue().replaceAll("[\t\n\\s+]", "");
			final String partNumber = (isWip ? toChange.getFromWIP().getPartNumber() : toChange.getFromLOG().getPartNumber()).replaceAll("[\t\n\\s+]", "");
			final String description = (isWip ? toChange.getFromWIP().getDescription() : toChange.getFromLOG().getDescription()).replaceAll("[\t\n\\s+]", "");

			logger.debug("\n\t{}\n\t{}\n\t{}\n\t{}\n\t{}\n\t{}\n\t{}", ()->isWip ? "isWip:" : "isLOG:", ()->pn, ()->partNumber, ()->pn.equals(partNumber), ()->descr, ()->description, ()->descr.equals(description));

			return pn.equals(partNumber) && descr.equals(description);
		};
	}
}
