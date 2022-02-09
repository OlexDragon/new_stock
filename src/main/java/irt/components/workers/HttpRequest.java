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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import irt.components.beans.irt.update.Profile;

public class HttpRequest {
	private final static Logger logger = LogManager.getLogger();

	public static <T> FutureTask<T> getForObgect(String url, Class<T> classToReturn) {
//		logger.error("classToReturn: {}; url: {}", classToReturn, url);

		final FutureTask<T> ft = new FutureTask<T>(
				()->{

					final HttpUriRequest uriRequest = new HttpGet(url);
			    	return httpForObject(classToReturn, uriRequest);
				});
		ThreadRunner.runThread(ft);

		return ft;
	}

	public static <T> FutureTask<T> postForObgect(String url, Class<T> classToReturn, List<NameValuePair> params) {
//		logger.error("*** postForObgect classToReturn: {}; url: {}", classToReturn, url);

		final FutureTask<T> ft = new FutureTask<T>(
				()->{

					final HttpPost httpPost = new HttpPost(url);
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

					return httpForObject(classToReturn, httpPost);
				});
		ThreadRunner.runThread(ft);

		return ft;
	}

	public static <T> FutureTask<T> postForIrtObgect(String url, Class<T> classToReturn, List<NameValuePair> params) {
//		logger.error("*** postForIrtObgect classToReturn: {}; url: {}, params: {}", classToReturn, url, params);

		final FutureTask<T> ft = new FutureTask<T>(
				()->{

					final HttpPost httpPost = new HttpPost(url);
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

					return httpForIrtObject(classToReturn, httpPost);
				});
		ThreadRunner.runThread(ft);

		return ft;
	}

	private static <T> T httpForIrtObject(Class<T> classToReturn, HttpPost uriRequest) throws IOException {

		uriRequest.addHeader("Accept", "text/html,application/json;metadata=full;charset=utf-8;");

		String json = null;
		//Execute and get the response.
		try(	final CloseableHttpClient httpclient = HttpClients.createDefault();
				final CloseableHttpResponse response = httpclient.execute(uriRequest);){

			HttpEntity entity = response.getEntity();

			if (entity != null) {

				json = EntityUtils.toString(entity).trim();

				if(classToReturn==null || json.matches(".*\\<[^>]+>.*"))	// If HTML page
					return null;

				final ObjectMapper mapper = new ObjectMapper();
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

//				logger.error(json);
				json = javaScriptToJSon(json);
//				logger.error(json);
				final T readValue = mapper.readValue(json, classToReturn);
//				logger.error("*** httpForIrtObject: {}", readValue);

				return readValue;
			}

		}catch(JsonParseException e) {
			logger.catching(new Throwable(json, e));
		}catch (ConnectException e) {
			logger.catching(Level.DEBUG, e);
		}

		return null;
	}

	private static String javaScriptToJSon(String javaScript) {
//		logger.error("*** javaScriptToJSon input json: ({})", javaScript);

		final int index = javaScript.indexOf("boards");
		if(index>0) {
			javaScript = javaScript.substring(0, index);
			final int lastIndexOf = javaScript.lastIndexOf(",");
			javaScript = javaScript.substring(0, lastIndexOf);
			javaScript += '}';
		}

		if(javaScript.endsWith(";"))
			javaScript = javaScript.substring(0, javaScript.lastIndexOf("}") + 1);

		// Remove declaration of JavaScript variable
		if(javaScript.contains("="))
			javaScript = javaScript.substring(javaScript.indexOf("{"));

		if(javaScript.endsWith("}")) 
			return addQuotation(javaScript.replaceAll("'", "\""));

		else
			return textToJSON(javaScript);
	}

	private static String addQuotation(String javaScript) {
//		logger.error("*** addQuotation input: ({})", javaScript);

		final String json = Optional.of(javaScript).map(js->js.split(":")).filter(split->split.length>1).map(Arrays::stream).orElseGet(()->Stream.empty())

				.map(
						str->{
							if(str.contains(",")) {

								final String[] split = str.split(",",2);
								return split[0] + ",\"" + split[1].trim() + '\"';

							}else if(str.contains("{")) {

								final String[] split = str.split("\\{",2);
								final String trim = split[1].trim();

								if(trim.isEmpty())
									return split[0] + "{";

								return split[0] + "{\"" + trim + '\"';

							} 
							return str;
						})
				.collect(Collectors.joining(":"));

//		logger.error("*** addQuotation output: '{}'", json);
		return json.isEmpty() ? "{}" : json;
	}

	private static String textToJSON(String json) {
//		logger.error("*** textToJSON input: ({})", json);
		
		StringBuffer sb = new StringBuffer("{");

		try(Scanner scanner = new Scanner(json.replaceAll("'", "\\\\'"))){
			while (scanner.hasNextLine()) {
				Optional.of(scanner.nextLine()).map(line->line.split(":", 2)).filter(split->split.length==2)
				.ifPresent(
						split->{
							sb.append("\"").append(split[0].trim()).append("\":\"").append(split[1].trim()).append("\",");
						});
				  
			}
		}

		// Remove extra comma
		sb.setLength(sb.length()-1);

//		logger.error("*** textToJSON output: {}", sb);
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

				if(classToReturn==null)
					return null;

				final ObjectMapper mapper = new ObjectMapper();
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

//				logger.error("json: ({})", json);

				final T readValue = mapper.readValue(json, classToReturn);
//			    			logger.error("{}", readValue);

				return readValue;
			}

		}catch(JsonParseException e) {
			logger.catching(new Throwable(json, e));
		}

		return null;
	}

	private static final String lineEnd = "\r\n";
	public static void upload(String sn, Profile profile) throws IOException {

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
				buf.append(line).append(System.getProperty("line.separator"));
			}
//			logger.error(buf);
		}

		connection.disconnect();
	}
}
