package com.tft.coach.data.datadragon;

import com.tft.coach.data.evidence.FetchEvidence;
import com.tft.coach.data.fetch.FetchResult;
import com.tft.coach.data.fetch.SourceFetchService;
import com.tft.coach.data.registry.SourceAdapterRegistry;
import com.tft.coach.data.snapshot.FileSystemRawSnapshotStore;
import com.tft.coach.data.snapshot.RawSnapshotStore;
import com.tft.coach.data.spi.FetchRequest;
import com.tft.coach.data.spi.SourceType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * High-level Data Dragon fetch API with snapshot persistence and evidence metadata.
 */
public class DataDragonFetchService {

    private final SourceFetchService fetchService;

    public DataDragonFetchService(SourceFetchService fetchService) {
        this.fetchService = fetchService;
    }

    public DataDragonFetchOutcome fetch(DataDragonResource resource, String patch, String locale)
            throws IOException, com.tft.coach.data.spi.AdapterFetchException {
        String resourceKey = resource.resourceKey();
        String versionHint = patch == null ? "" : patch;
        String url = patch == null || patch.isBlank()
                ? DataDragonUrls.BASE + "/cdn/latest/data/" + locale + "/" + resource.fileName()
                : DataDragonUrls.dataJson(versionHint, locale, resource);

        FetchRequest request = new FetchRequest(
                SourceType.RIOT,
                DataDragonAdapter.ADAPTER_ID,
                resourceKey,
                url,
                patch,
                Map.of(DataDragonAdapter.PARAM_LOCALE, locale)
        );
        FetchResult result = fetchService.fetch(request);
        FetchEvidence evidence = FetchEvidence.fromFetchResult(result);
        return new DataDragonFetchOutcome(result, evidence);
    }

    public record DataDragonFetchOutcome(FetchResult result, FetchEvidence evidence) {}

    public static DataDragonFetchService createDefault(Path snapshotRoot, DataDragonHttpClient httpClient) {
        RawSnapshotStore store = new FileSystemRawSnapshotStore(snapshotRoot);
        SourceAdapterRegistry registry = new SourceAdapterRegistry(
                java.util.List.of(new DataDragonAdapter(httpClient)));
        return new DataDragonFetchService(new SourceFetchService(registry, store));
    }
}
