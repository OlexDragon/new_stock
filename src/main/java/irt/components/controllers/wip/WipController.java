package irt.components.controllers.wip;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import irt.components.beans.wip.WipContent;

@Controller
@RequestMapping("wip")
public class WipController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.wip.directory}")
	private String wipDirectory;

	@Value("${irt.log.file}")
	private String logFile;

    @GetMapping
    String getBoms(Model model) {
		logger.traceEntry();

       return "wip";
    }

    @PostMapping("content")
    String getContent(@RequestParam String file, @RequestParam String wo, Model model) {
    	logger.traceEntry("file: {}; wo: {}", file, wo);

    	// WIP Filw
    	final File f = Paths.get(wipDirectory, file).toFile();
    	if(!f.exists()){
    		model.addAttribute("path", f);
    		return  "wip :: no_file";
    	}

    	List<WipContent> rows = new ArrayList<>();
    	try(InputStream is=new FileInputStream(f); ){

    		getDataFomWipFilw(is, rows, wo);

    	} catch (IOException e) {
			return error(model, e);
		}

    	// Log File
    	final File lf = new File(logFile);
    	if(!lf.exists()){
    		model.addAttribute("path", lf);
    		return  "wip :: no_file";
    	}

    	try(InputStream is=new FileInputStream(lf); ){

    		getDataFomLogFile(is, rows, wo);

    	} catch (IOException e) {
			return error(model, e);
		}

    	model.addAttribute("rows", rows);

		return "wip :: content";
    }

	private void getDataFomLogFile(InputStream is, List<WipContent> rows, String wo) throws IOException {

		try(XSSFWorkbook wb=new XSSFWorkbook(is);){
 
			XSSFSheet sheet=wb.getSheetAt(0);

			int toThis = 0;
			for(int i = sheet.getLastRowNum(); i>toThis; i--) {

				final XSSFRow row = sheet.getRow(i);
				if(row==null)
					continue;

				final XSSFCell cell = row.getCell(3, MissingCellPolicy.CREATE_NULL_AS_BLANK);

				final String workOrder = cell.toString();

				final Boolean found = rows.stream().filter(wip->wip.getWorkOrder().equals(workOrder))

						.map(
								wip->{

									final WipContent fromLogFile = wip.getFromLogFile();
									final String pn = row.getCell(2, MissingCellPolicy.CREATE_NULL_AS_BLANK).toString();
									final String descr = row.getCell(4, MissingCellPolicy.CREATE_NULL_AS_BLANK).toString();

									if(fromLogFile==null) {

										final WipContent log = new WipContent(workOrder, pn, descr);
										log.setQty(1);
										wip.setFromLogFile(log);

									}else {

										int qty = fromLogFile.getQty();
										fromLogFile.setQty(++qty);
									}

									return true;
								})
						.filter(t->t).findAny().orElse(false);

				if(found) {
					final int tmp = i - 50;
					toThis = tmp > 0 ? tmp : 0;
				}
			}
		}
	
	}

	private void getDataFomWipFilw(InputStream is, List<WipContent> rows, String wo) throws IOException {

		try(XSSFWorkbook wb=new XSSFWorkbook(is);){
 
			XSSFSheet sheet=wb.getSheetAt(0);

			for(int lastRowNum = sheet.getLastRowNum(); lastRowNum>0; lastRowNum--) {

				final XSSFRow row = sheet.getRow(lastRowNum);
				final XSSFCell cell = row.getCell(4, MissingCellPolicy.CREATE_NULL_AS_BLANK);

				final String workOrder = cell.toString();
				if(workOrder.contains(wo)) {
					
					final String pn = row.getCell(2, MissingCellPolicy.CREATE_NULL_AS_BLANK).toString();
					final String descr = row.getCell(1, MissingCellPolicy.CREATE_NULL_AS_BLANK).toString();
					final String qtyStr = row.getCell(3, MissingCellPolicy.CREATE_NULL_AS_BLANK).toString();
					final Integer qty = Optional.of(qtyStr).filter(s->!s.isEmpty()).filter(s->s.replaceAll("[\\d.]", "").isEmpty()).map(Double::parseDouble).map(Number::intValue).orElse(0);

					final WipContent e = new WipContent(workOrder, pn, descr);
					e.setQty(qty);
					rows.add(e);

				}else if(!rows.isEmpty())
					break;
			}
		}
	}

	private String error(Model model, IOException e) {
		logger.catching(e);
		model.addAttribute("error", e);
		return "wip :: error";
	}
}
