package com.tft.coach.data.opgg;

import com.tft.coach.data.spi.AdapterFetchException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class JdkOpGgStatsHttpClient implements OpGgStatsHttpClient {

    private static final String USER_AGENT = "tft-vision-ai-coach/0.1 (stats-adapter)";

    private final HttpClient httpClient;

    public JdkOpGgStatsHttpClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    public JdkOpGgStatsHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public byte[] getBytes(String url) throws AdapterFetchException {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
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
}
