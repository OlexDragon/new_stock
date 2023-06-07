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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.FutureTask;
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

import irt.components.beans.irt.update.Profile;

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

		uriRequest.addHeader("Accept", "text/html,application/json;metadata=full;charset=utf-8;");

		String json = null;
		//Execute and get the response.
		try(	final CloseableHttpClient httpclient = HttpClients.createDefault();
				final CloseableHttpResponse response = httpclient.execute(uriRequest);){

			HttpEntity entity = response.getEntity();

			if (entity != null) {

				final String text = EntityUtils.toString(entity);
//				logger.error(text);

				if(classToReturn==null || text.isEmpty() || text.matches(".*\\<[^>]+>.*"))	// If HTML page
					return null;

				json = text.contains("=") ? javaScriptToJSon(text) : textToJSON(text);
//				logger.error("classToReturn: {}; json: {}", classToReturn, json);

				final ObjectMapper mapper = new ObjectMapper();
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				return mapper.readValue(json, classToReturn);
			}

		}catch(JsonParseException e) {
			logger.catching(new Throwable("\n\tclass to return: " + classToReturn + "\n\turl: " + uriRequest + "\n\tfrom:\n" + json, e));

		}catch( ConnectException | UnknownHostException e) {
			logger.catching(Level.DEBUG, e);
		}

		return null;
	}

	public static String javaScriptToJSon(String javaScript) throws ScriptException, JsonProcessingException {
		logger.traceEntry(javaScript);

		final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		final ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("JavaScript");
		scriptEngine.eval("variable = " + javaScript);
	    scriptEngine.eval("json= JSON.stringify(variable);");
	    return (String) scriptEngine.get("json");
	}

	private static String textToJSON(String text) {
		logger.traceEntry("'{}'", text);
		
		StringBuffer sb = new StringBuffer("{");

		try(Scanner scanner = new Scanner(text.replaceAll("'", "\\\\'"))){
			while (scanner.hasNextLine()) {
				Optional.of(scanner.nextLine()).map(line->line.split(":", 2)).filter(split->split.length==2)
				.ifPresent(
						split->{
							sb.append("\"").append(split[0].trim()).append("\":\"").append(split[1].trim()).append("\",");
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

		uriRequest.addHeader("Accept", "text/html,application/json;metadata=full;charset=utf-8;");

		String json = null;
		//Execute and get the response.
		try(	final CloseableHttpClient httpclient = HttpClients.createDefault();
				final CloseableHttpResponse response = httpclient.execute(uriRequest);){

			HttpEntity entity = response.getEntity();

			if (entity != null) {

				json = EntityUtils.toString(entity);
				logger.debug("json: ({})", json);

				if(classToReturn==null)
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
	public static void upload(String sn, Profile profile) throws IOException {
		logger.traceEntry(sn);

		URL url = new URL("http", sn, "/upgrade.cgi");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "multipart/form-data;");
		connection.setDoOutput(true);

		try(	OutputStream outputStream = connection.getOutputStream();
				DataOutputStream dataOutputStream = new DataOutputStream(outputStream);) {
			
			dataOutputStream.writeBytes("Upgrade" + lineEnd);
			dataOutputStream.writeBytes(lineEnd);

			byte[] bytes = profile.toBytes();
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

		connection.disconnect();
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

	public static String getForString(String url) throws IOException {

		logger.traceEntry(url);

		final HttpGet httpGet = new HttpGet(url);

		try(	final CloseableHttpClient httpclient = HttpClients.createDefault();
				final CloseableHttpResponse response = httpclient.execute(httpGet);){

			return entityToString(response);

		}
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

		String json = null;
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
//				logger.error(sb);

				final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				return mapper.readValue(sb.toString(), classToReturn);
			}

		}catch(JsonParseException e) {
			logger.catching(new Throwable("\n\tclass to return: " + classToReturn + "\n\tfrom:\n" + json, e));

		}catch( ConnectException | UnknownHostException e) {
			logger.catching(Level.DEBUG, e);
		}

		return null;
	}

	public static Map<String, Integer> getAllDevices(String sn) throws IOException{
		try {

			final URL url = new URL("http", sn, "/diagnostics.asp?devices=1");
			final String html = getForString(url.toString());
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
		} catch (JsonProcessingException | MalformedURLException | ScriptException e) {
			logger.catching(e);
		}

		return new HashMap<>();
	}
}
