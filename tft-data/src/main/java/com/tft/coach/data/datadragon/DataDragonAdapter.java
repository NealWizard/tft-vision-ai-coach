package com.tft.coach.data.datadragon;

import com.tft.coach.data.spi.AdapterFetchException;
import com.tft.coach.data.spi.AdapterFetchPayload;
import com.tft.coach.data.spi.FetchRequest;
import com.tft.coach.data.spi.SourceAdapter;
import com.tft.coach.data.spi.SourceType;

import java.time.Instant;
import java.util.Objects;

/**
 * Fetches TFT static JSON from Riot Data Dragon CDN.
 */
public class DataDragonAdapter implements SourceAdapter {

    public static final String ADAPTER_ID = "riot-datadragon";
    public static final String PARAM_LOCALE = "locale";
    public static final String DEFAULT_LOCALE = "zh_CN";

    private final DataDragonHttpClient httpClient;
    private final DataDragonVersionResolver versionResolver;

    public DataDragonAdapter(DataDragonHttpClient httpClient) {
        this(httpClient, new DataDragonVersionResolver(httpClient));
    }

    public DataDragonAdapter(DataDragonHttpClient httpClient, DataDragonVersionResolver versionResolver) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.versionResolver = Objects.requireNonNull(versionResolver, "versionResolver");
    }

    @Override
    public SourceType sourceType() {
        return SourceType.RIOT;
    }

    @Override
    public String adapterId() {
        return ADAPTER_ID;
    }

    @Override
    public boolean supports(FetchRequest request) {
        return request.sourceType() == SourceType.RIOT
                && ADAPTER_ID.equals(request.sourceId())
                && isKnownResource(request.resourceKey());
    }

    @Override
    public AdapterFetchPayload fetch(FetchRequest request) throws AdapterFetchException {
        DataDragonResource resource = DataDragonResource.fromResourceKey(request.resourceKey());
        String version = versionResolver.resolve(request.patch());
        String locale = request.params().getOrDefault(PARAM_LOCALE, DEFAULT_LOCALE);
        String url = DataDragonUrls.dataJson(version, locale, resource);
        byte[] body = httpClient.getBytes(url);
        return new AdapterFetchPayload(body, "application/json", Instant.now(), version);
    }

    private static boolean isKnownResource(String resourceKey) {
        try {
            DataDragonResource.fromResourceKey(resourceKey);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
