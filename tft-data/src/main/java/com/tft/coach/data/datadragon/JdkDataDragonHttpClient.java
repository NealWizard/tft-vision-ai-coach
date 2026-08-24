package com.tft.coach.data.datadragon;

import com.tft.coach.data.spi.AdapterFetchException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * JDK {@link HttpClient} implementation for Data Dragon.
 */
public class JdkDataDragonHttpClient implements DataDragonHttpClient {

    private final HttpClient httpClient;

    public JdkDataDragonHttpClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    public JdkDataDragonHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public byte[] getBytes(String url) throws AdapterFetchException {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new AdapterFetchException("HTTP " + response.statusCode() + " for " + url);
            }
            if (response.body() == null || response.body().length == 0) {
                throw new AdapterFetchException("Empty body for " + url);
            }
            return response.body();
        } catch (AdapterFetchException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AdapterFetchException("GET failed: " + url, ex);
        }
    }

    @Override
    public String getVersionsJson() throws AdapterFetchException {
        return new String(getBytes(DataDragonUrls.VERSIONS));
    }
}
