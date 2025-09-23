package irt.components.services;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

public class HttpPool {

	private static final PoolingHttpClientConnectionManager connManager;
	private static final CloseableHttpClient httpClient;

	static {

		connManager = new PoolingHttpClientConnectionManager();
		connManager.setMaxTotal(100); // Max total connections
		connManager.setDefaultMaxPerRoute(20); // Max per route

		httpClient = HttpClients.custom()
				.setConnectionManager(connManager)
				.build();
	}

	public static CloseableHttpClient getHttpClient() {
		return httpClient;
	}

	public static Optional<HttpEntity> getEntity(HttpRequestBase request) throws ClientProtocolException, IOException {
		return Optional.ofNullable(httpClient.execute(request).getEntity());
	}

	public static String requestToString(HttpRequestBase request) throws ClientProtocolException, IOException {
		return getEntity(request).map(tuStringWithCatch()).orElse(null);
    }

	private static Function<HttpEntity, String> tuStringWithCatch() {
		return entry->{
			try {
				return EntityUtils.toString(entry);
			} catch (ParseException | IOException e) {
				throw new HttpPoolException("Exception while parsing entity to string", e);
			}
		};
	}

	public static class HttpPoolException extends RuntimeException {
		private static final long serialVersionUID = 4953937699981711558L;

		public HttpPoolException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
