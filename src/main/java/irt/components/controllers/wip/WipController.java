package irt.components.controllers.wip;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
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

	public final static int WIP_WORK_ORDER = 4;
	public final static int LOG_WORK_ORDER = 3;

	public final static int WIP_PART_NUMBER = 2;
	public final static int LOG_PART_NUMBER = 2;

	public final static int WIP_DESCRIPTION = 1;
	public final static int LOG_DESCRIPTION = 4;

	public final static int WIP_QTY = 3;

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

    	} catch (Exception e) {
    		logger.catching(e);
			return error(model, e, f);
		}

    	// Log File
    	final File lf = new File(logFile);
    	if(!lf.exists()){
    		model.addAttribute("path", lf);
    		return  "wip :: no_file";
    	}

    	try(InputStream is=new FileInputStream(lf); ){

    		getDataFomLogFile(is, rows, wo);

    	} catch (Exception e) {
    		logger.catching(e);
			return error(model, e, lf);
		}

    	model.addAttribute("rows", rows);

		return "wip :: content";
    }

	private void getDataFomLogFile(InputStream is, List<WipContent> rows, String wo) throws IOException {

		try(XSSFWorkbook wb=new XSSFWorkbook(is);){
 
			XSSFSheet sheet=wb.getSheetAt(0);

			int scanTo = 0;
			for(int nextToRead = sheet.getLastRowNum(); nextToRead>scanTo; nextToRead--) {

				final XSSFRow row = sheet.getRow(nextToRead);
				if(row==null)
					continue;

				final String workOrder = row.getCell(LOG_WORK_ORDER, MissingCellPolicy.CREATE_NULL_AS_BLANK).toString().trim();

				final List<WipContent> wips = rows.stream().filter(wip->wip.getWorkOrder().equals(workOrder)).collect(Collectors.toList());

				final String pn		 = row.getCell(LOG_PART_NUMBER, MissingCellPolicy.CREATE_NULL_AS_BLANK).toString().trim();
				final String descr	 = row.getCell(LOG_DESCRIPTION, MissingCellPolicy.CREATE_NULL_AS_BLANK).toString().replaceAll("[\n\r]", " ").replaceAll(" +", " ").trim();
				final WipContent logContent = new WipContent(workOrder, pn, descr);
				logContent.setQty(1);

				if(wips.isEmpty()) {

					WipContent tmp = new WipContent(workOrder, "", workOrder + " does not exists in the 'WIP....xlsx' file.");
					tmp.setFromLogFile(logContent);
					wips.add(tmp);

				}else{

					final int tmp = nextToRead - 500;
					scanTo = tmp > 0 ? tmp : 0;

					final int size = wips.size();

					if(size==1) {

						final WipContent w = wips.get(0);
						final WipContent l = w.getFromLogFile();
						if(l==null)
							w.setFromLogFile(logContent);

						else {
							int qty = l.getQty();
							l.setQty(++qty);
						}

					} else if(size>1) {

						final List<WipContent> left = wips.parallelStream()

								.filter(
										w->{
											final String wDescription = w.getDescription();

											if(wDescription.length()==descr.length())
												return wDescription.equals(descr);

											else if(wDescription.length()>descr.length())
												return wDescription.contains(descr);

											else
												return descr.contains(wDescription);
										})
								.collect(Collectors.toList());

						if(left.isEmpty())
							fillNext(wips, logContent);

						else if(left.size()>1)
							fillNext(left, logContent);

						else {
							final WipContent w = left.get(0);
							if(w.getFromLogFile()==null)
								w.setFromLogFile(logContent);
							else 
								addQty(left);

						}
					}
						
				}
			}
		}
	}

	private void fillNext(final List<WipContent> wips, final WipContent logContent) {
		int wSize = wips.size();
		for(int index=0; index<wSize; index++) {
			WipContent w = wips.get(index);
			if(w.getFromLogFile()==null) {
				w.setFromLogFile(logContent);
				break;
			}
		}
		addQty(wips);
	}

	private void addQty(final List<WipContent> wips) {
		final WipContent w = wips.get(0);
		int qty = w.getQty();
		w.setQty(++qty);
	}

	private void getDataFomWipFilw(InputStream is, List<WipContent> rows, String wo) throws IOException {

		try(XSSFWorkbook wb=new XSSFWorkbook(is);){
 
			XSSFSheet sheet=wb.getSheetAt(0);

			for(int nextToRead = sheet.getLastRowNum(); nextToRead>0; nextToRead--) {

				final XSSFRow row = sheet.getRow(nextToRead);
				if(row==null)
					continue;

				final String workOrder = row.getCell(WIP_WORK_ORDER, MissingCellPolicy.CREATE_NULL_AS_BLANK).toString().trim();
				if(workOrder.contains(wo)) {
					
					final String pn 	= row.getCell(WIP_PART_NUMBER	, MissingCellPolicy.CREATE_NULL_AS_BLANK).toString().trim();
					final String descr 	= row.getCell(WIP_DESCRIPTION	, MissingCellPolicy.CREATE_NULL_AS_BLANK).toString().replaceAll("[\n\r]", " ").replaceAll(" +", " ").trim();
					final String qtyStr = row.getCell(WIP_QTY			, MissingCellPolicy.CREATE_NULL_AS_BLANK).toString().trim();
					final Integer qty = Optional.of(qtyStr).filter(s->!s.isEmpty()).filter(s->s.replaceAll("[\\d.]", "").isEmpty()).map(Double::parseDouble).map(Number::intValue).orElse(0);

					final WipContent e = new WipContent(workOrder, pn, descr);
					e.setQty(qty);
					rows.add(e);

				}else if(!rows.isEmpty())
					break;
			}
		}
	}

	private String error(Model model, Exception e, File f) {
		logger.catching(e);
		model.addAttribute("file", f);
		model.addAttribute("error", e);
		return "wip :: error";
	}
}
