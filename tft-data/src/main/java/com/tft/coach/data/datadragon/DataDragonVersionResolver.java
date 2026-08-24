package com.tft.coach.data.datadragon;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tft.coach.data.spi.AdapterFetchException;

import java.util.List;
import java.util.Objects;

/**
 * Resolves a patch hint to a concrete Data Dragon version string.
 */
public class DataDragonVersionResolver {

    private static final TypeReference<List<String>> VERSION_LIST = new TypeReference<>() {};

    private final DataDragonHttpClient httpClient;
    private final ObjectMapper mapper;
    private volatile List<String> cachedVersions;

    public DataDragonVersionResolver(DataDragonHttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.mapper = new ObjectMapper();
    }

    public String resolve(String patchHint) throws AdapterFetchException {
        List<String> versions = loadVersions();
        if (patchHint == null || patchHint.isBlank()) {
            return versions.getFirst();
        }
        String hint = patchHint.trim();
        for (String version : versions) {
            if (version.equals(hint)) {
                return version;
            }
        }
        for (String version : versions) {
            if (version.startsWith(hint + ".") || version.startsWith(hint)) {
                return version;
            }
        }
        throw new AdapterFetchException("No Data Dragon version matches patch hint: " + hint);
    }

    private List<String> loadVersions() throws AdapterFetchException {
        List<String> local = cachedVersions;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (cachedVersions != null) {
                return cachedVersions;
            }
            try {
                cachedVersions = mapper.readValue(httpClient.getVersionsJson(), VERSION_LIST);
                if (cachedVersions.isEmpty()) {
                    throw new AdapterFetchException("Data Dragon versions list is empty");
                }
                return cachedVersions;
            } catch (AdapterFetchException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new AdapterFetchException("Failed to parse versions.json", ex);
            }
        }
    }
}
