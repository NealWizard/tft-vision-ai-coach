package com.tft.coach.meta;

import com.tft.coach.data.meta.MetaSnapshot;
import com.tft.coach.data.opgg.OpGgMetaSnapshotParser;
import com.tft.coach.data.spi.AdapterFetchException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public final class FixtureMetaSnapshots {

    public static final String PATCH = "set18-18.1";

    private FixtureMetaSnapshots() {}

    public static void seed(MetaSnapshotStore store) {
        store.save(new StoredMetaSnapshot("meta-fixture-24h", load("meta/fixtures/snapshot-24h.json")));
        store.save(new StoredMetaSnapshot("meta-fixture-7d", load("meta/fixtures/snapshot-7d.json")));
    }

    public static MetaSnapshot load(String resource) {
        try (InputStream in = FixtureMetaSnapshots.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing meta fixture: " + resource);
            }
            return new OpGgMetaSnapshotParser().parse(in.readAllBytes());
        } catch (IOException | AdapterFetchException ex) {
            throw new IllegalStateException("Failed to load meta fixture: " + resource, ex);
        }
    }

    public static List<String> fixtureIds() {
        return List.of("meta-fixture-24h", "meta-fixture-7d");
    }
}
