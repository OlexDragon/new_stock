package irt.components.workers;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import irt.components.beans.irt.CalibrationRwInfo;
import irt.components.beans.irt.update.ToUpload;

public class HttpRequest {
	private final static Logger logger = LogManager.getLogger();
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	public static <T> FutureTask<T> getForObgect(String url, Class<T> classToReturn) {
		logger.traceEntry("classToReturn: {}; url: {}", classToReturn, url);

		final FutureTask<T> ft = new FutureTask<T>(
				()->{

					final HttpUriRequest uriRequest = new HttpGet(url);
			    	return httpForObject(classToReturn, uriRequest);
				});
		ThreadRunner.runThread(ft);

		return ft;
	}

	public static <T> FutureTask<T> postForObgect(String url, Class<T> classToReturn, List<NameValuePair> params) {
		logger.traceEntry("*** postForObgect classToReturn: {}; url: {}", classToReturn, url);

		final FutureTask<T> ft = new FutureTask<T>(
				()->{

					final HttpPost httpPost = new HttpPost(url);
					setEntity(httpPost, params);

					return httpForObject(classToReturn, httpPost);
				});
		ThreadRunner.runThread(ft);

		return ft;
	}

	public static FutureTask<String> postString(String url, String string) {


		final FutureTask<String> ft = new FutureTask<>(
				()->{

					final HttpPost httpPost = new HttpPost(url);
					httpPost.setEntity(new StringEntity(string));

					try(	final CloseableHttpClient httpclient = HttpClients.createDefault();
							final CloseableHttpResponse response = httpclient.execute(httpPost);){

						return entityToString(response);
					}
				});
		ThreadRunner.runThread(ft);

		return ft;
	}

	public static <T> FutureTask<T>postForObgect(URL url, Class<T> classToReturn, Object object) {
		logger.traceEntry("url: {}; classToReturn: {}; object: {};", url, classToReturn, object);

		final FutureTask<T> ft = new FutureTask<T>(
				()->{

					final HttpPost httpPost = new HttpPost(url.toURI());
					httpPost.setHeader("Content-type", "application/json");

					Optional.ofNullable(object)
					.map(o->{
						final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(Include.NON_NULL).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
						try {
							return mapper.writer().withDefaultPrettyPrinter().writeValueAsString(object);
						} catch (JsonProcessingException e) {
							logger.catching(e);
						}
						return null;
					})
					.map(json -> {
						try {
							return new StringEntity(json);
						} catch (UnsupportedEncodingException e) {
							logger.catching(e);
						}
						return null;
					})
					.ifPresent(httpPost::setEntity);

					return httpForObject(classToReturn, httpPost);
				});
		ThreadRunner.runThread(ft);

		return ft;
	}

	public static <T> FutureTask<T> postForIrtObgect(String url, Class<T> classToReturn, List<NameValuePair> params) {
//		logger.catching(new Throwable());

		final FutureTask<T> ft = new FutureTask<T>(
				()->{

					logger.traceEntry("\n\tpostForIrtObgect - classToReturn: {}; \n\turl: {}, params: {}\n", classToReturn, url, params);

					final HttpPost httpPost = new HttpPost(url);
					setEntity(httpPost, params);

					return httpForIrtObject(classToReturn, httpPost);
				});
		ThreadRunner.runThread(ft);

		return ft;
	}

	private static <T> T httpForIrtObject(Class<T> classToReturn, HttpPost uriRequest) throws IOException, ScriptException {
		logger.traceEntry("classToReturn: {};", classToReturn);

		uriRequest.addHeader("Accept", "text/html,application/json;metadata=full;charset=utf-8;");

		String json = null;
		String text = null;
		//Execute and get the response.
		try(	final CloseableHttpClient httpclient = HttpClients.createDefault();
				final CloseableHttpResponse response = httpclient.execute(uriRequest);){

			HttpEntity entity = response.getEntity();

			if (entity != null) {

				text = EntityUtils.toString(entity);

				if(classToReturn==null || text.isEmpty() || text.matches(".*\\<[^>]+>.*"))	// If HTML page
					return null;

				json = text.contains("=") ? javaScriptToJSon(text) : textToJSON(text);
				logger.trace("classToReturn: {}; json: {}", classToReturn, json);

				final ObjectMapper mapper = new ObjectMapper();
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				if(classToReturn.equals(CalibrationRwInfo.class))
					json = checkCalibrationRwInfo(json);

				logger.debug(json);

				return mapper.readValue(json, classToReturn);
			}

		}catch(JsonParseException e) {
			logger.catching(new Throwable("\n\tclass to return: " + classToReturn + "\n\turl: " + uriRequest + "\n\tfrom:\n" + json + "\n\tresponse:\n" + text, e));

		}catch( ConnectException | UnknownHostException e) {
			logger.catching(Level.DEBUG, e);
		}

		return null;
	}

	private static String checkCalibrationRwInfo(String json) {
		final String[] split = json.split("\"dp\":\\{", 2);
		if(split.length>1) {
			final int closingBracketIndex = findClosingBracketIndex(split[1]);
			return split[0] + "\"dp\":[{" + split[1].substring(0, closingBracketIndex) + "}]" + split[1].substring(closingBracketIndex +1);
		}
		return json;
	}

	private static int findClosingBracketIndex(String string) {

		int count = 1;
		int i=0;

		for(; i<string.length(); ++i) {
			if(string.charAt(i)=='{')
				count++;
			else if(string.charAt(i)=='}')
				count--;

			if(count == 0)
				break;
		}

		return i;
	}

	public static String javaScriptToJSon(String javaScript) throws ScriptException, JsonProcessingException {
		logger.traceEntry(javaScript);
		javaScript = javaScript.replace("var ", ""); 

		final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		final ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("JavaScript");
		scriptEngine.eval("variable = " + javaScript);
	    scriptEngine.eval("json= JSON.stringify(variable);");
	    return (String) scriptEngine.get("json");
	}

	private static String textToJSON(String text) {
		logger.traceEntry("\n\t'{}'", text);
		
		StringBuffer sb = new StringBuffer("{");

		try(Scanner scanner = new Scanner(text.replaceAll("'", "\\\\'"))){
			while (scanner.hasNextLine()) {
				Optional.of(scanner.nextLine()).map(line->line.split(":", 2)).filter(split->split.length==2)
				.ifPresent(
						split->{
							sb.append("\"").append(split[0].trim()).append("\":\"").append(split[1].trim().replace("N/A", "NotAplicable")).append("\",");
						});
				  
			}
		}

		// Remove extra comma
		final int length = sb.length();
		if(length>1)
			sb.setLength(length-1);

//		logger.error(sb);
		return sb.append('}').toString();
	}

	private static <T> T httpForObject(Class<T> classToReturn, final HttpUriRequest uriRequest) throws IOException{

		if(classToReturn==null)
			return null;

		uriRequest.addHeader("Accept", "text/html,application/json;metadata=full;charset=utf-8;");

		String json = null;
		//Execute and get the response.
		try(	final CloseableHttpClient httpclient = HttpClients.createDefault();
				final CloseableHttpResponse response = httpclient.execute(uriRequest);){

			HttpEntity entity = response.getEntity();

			if (entity != null) {

				json = EntityUtils.toString(entity);
				if(json.startsWith("<!DOCTYPE html")) {
					logger.error(json);
					logger.catching(new Throwable(uriRequest.getURI().toString()));
					return null;
				}
				
				logger.debug("json: ({})", json);

				if(json.contains("not found") || json.contains("Whitelabel Error Page"))
					return null;

				final ObjectMapper mapper = new ObjectMapper();
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				final T readValue = mapper.readValue(json, classToReturn);
				logger.debug("{}", readValue);

				return readValue;
			}

		}catch(JsonParseException e) {
			logger.catching(new Throwable(json, e));
		}

		return null;
	}

	private static final String lineEnd = "\r\n";
	public static void upload(String sn, ToUpload toUpload) {
		logger.traceEntry(sn);

		HttpURLConnection connection = null;
		try {
			URL url = new URL("http", sn, "/upgrade.cgi");
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "multipart/form-data;");
			connection.setDoOutput(true);

			try(	OutputStream outputStream = connection.getOutputStream();
					DataOutputStream dataOutputStream = new DataOutputStream(outputStream);) {
				
				dataOutputStream.writeBytes("Upgrade" + lineEnd);
				dataOutputStream.writeBytes(lineEnd);

				byte[] bytes = toUpload.toBytes();
				dataOutputStream.write(bytes , 0, bytes.length);
				dataOutputStream.writeBytes(lineEnd);
				dataOutputStream.flush();
			}

			// Read Response
			try(	InputStream inputStream = connection.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));) {
				
				StringBuffer buf = new StringBuffer();
				String line;

				// When the unit accepts the update it return page with the title 'End of session'
				while ((line = reader.readLine())!=null) {
					buf.append(line).append(LINE_SEPARATOR);
				}
				logger.debug(buf);
			}
		} catch (IOException e) {
			logger.catching(e);
		}

		Optional.ofNullable(connection).ifPresent(HttpURLConnection::disconnect);
	}

	// Send post request and parse text like YAML file
	public static <T> FutureTask<T> postForIrtYaml(String url, Class<T> classToReturn, List<NameValuePair> params) {

		final FutureTask<T> ft = new FutureTask<T>(
				()->{

					logger.traceEntry("\n\tpostForIrtObgect - classToReturn: {}; \n\turl: {}, params: {}\n", classToReturn, url, params);

					final HttpPost httpPost = new HttpPost(url);
					setEntity(httpPost, params);

					return httpForIrtYaml(classToReturn, httpPost);
				});
		ThreadRunner.runThread(ft);

		return ft;
	}

	public static String postForString(String url, List<NameValuePair> params) throws IOException {
		logger.traceEntry("{}; {}", url, params);

		final HttpPost httpPost = new HttpPost(url);
		httpPost.addHeader("Accept", "text/html,application/json;metadata=full;charset=utf-8;");
		setEntity(httpPost, params);

		try(	final CloseableHttpClient httpclient = HttpClients.createDefault();
				final CloseableHttpResponse response = httpclient.execute(httpPost);){

			return entityToString(response);

		}
	}

	public static void setEntity(final HttpPost httpPost, List<NameValuePair> params) {
		Optional.ofNullable(params).map(
				p -> {
					try {

						return new UrlEncodedFormEntity(p);

					} catch (UnsupportedEncodingException e) {
						logger.catching(e);
					}
					return null;
				})
		.ifPresent(httpPost::setEntity);
	}

	public static String getForString(String url) throws InterruptedException, ExecutionException, TimeoutException{
		 return  getForString(url, 1000, TimeUnit.MILLISECONDS);
	 }

	public static String getForString(String url, int timeout, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
		logger.debug("url: {}; timeout: {}; timeUnit: {}", url, timeout, timeUnit);

		FutureTask<String> ft = getForStringFT(url);

		return ft.get(timeout, timeUnit);
	}

	public static FutureTask<String> getForStringFT(String url) {
		logger.traceEntry(url);

		Callable<String> callable = ()->{
			final HttpGet httpGet = new HttpGet(url);

			try(	final CloseableHttpClient httpclient = HttpClients.createDefault();
					final CloseableHttpResponse response = httpclient.execute(httpGet);){
				return entityToString(response);
			}

		};
		FutureTask<String> ft = new FutureTask<>(callable);
		ThreadRunner.runThread(ft);
		return ft;
	}

	private static String entityToString(final CloseableHttpResponse response) {
		return Optional.ofNullable(response.getEntity())
				.map(
						t -> {
							try {

								return EntityUtils.toString(t);

							} catch (ParseException e) {} catch (IOException e) {
								logger.catching(e);
							}
							return null;
						}).orElse(null);
	}

	private static <T> T httpForIrtYaml(Class<T> classToReturn, HttpPost uriRequest) throws IOException, ScriptException {

		uriRequest.addHeader("Accept", "text/html,application/json;metadata=full;charset=utf-8;");

		//Execute and get the response.
		try(	final CloseableHttpClient httpclient = HttpClients.createDefault();
				final CloseableHttpResponse response = httpclient.execute(uriRequest);){

			HttpEntity entity = response.getEntity();

			if (entity != null) {

				final String text = EntityUtils.toString(entity);

				if(classToReturn==null || text.isEmpty() || text.matches(".*\\<[^>]+>.*"))	// If HTML page
					return null;

				StringBuilder sb = new StringBuilder();
				try(Scanner scanner = new Scanner(text);){
					while(scanner.hasNextLine()){
						final String nextLine = scanner.nextLine().replaceAll("\\s{2,}", " ");
						sb.append(nextLine).append("\n");
					}
				}
				logger.debug(sb);

				final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				return mapper.readValue(sb.toString(), classToReturn);
			}

		}catch(JsonParseException e) {
			logger.catching(new Throwable("\n\tclass to return: " + classToReturn + "\n", e));

		}catch( ConnectException | UnknownHostException e) {
			logger.catching(Level.DEBUG, e);
		}

		return null;
	}

	public static Map<String, Integer> getAllModules(String sn) throws IOException, InterruptedException, ExecutionException, TimeoutException, ScriptException{

			final URL url = new URL("http", sn.trim(), "/diagnostics.asp?devices=1");
			logger.debug(url);

			final String html = getForString(url.toString(), 5, TimeUnit.SECONDS);
			logger.debug(html);
			final String str = Optional.of(html.indexOf("devices = [")).filter(index->index>=0)
								.flatMap(start->Optional.of(html.indexOf("]", start)).filter(index->index>=0)
										.map(stop->html.substring(start, stop + 1))).orElse(null);

			if(str!=null) {

				final String json = javaScriptToJSon(str);
				logger.debug(json);

				final ObjectMapper mapper = new ObjectMapper();
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			
				final List<?> list = mapper.readValue(json, List.class);
				return list.parallelStream().map(l->(Map<?,?>)l).filter(m->m.get("name")!=null).map(m->new AbstractMap.SimpleEntry<>((String)m.get("name"), (Integer)m.get("index"))).collect(Collectors.toMap(e->e.getKey(), e->e.getValue()));
			}

			logger.debug("No Answer.");
		return new HashMap<>();
	}

	public static Object postForSystemConfig(String sn) throws IOException{
		logger.traceEntry("sn: {}", sn);

		final List<NameValuePair> list = new ArrayList<>();
		list.add(new BasicNameValuePair("devid", "1"));
		list.add(new BasicNameValuePair("command", "config"));
		String url = new URL("http", "IRT-2508001", "/device_debug_read.cgi").toString();
		final String postForString = postForString(url, list);
		logger.debug(postForString);

		return null;
	}
}
