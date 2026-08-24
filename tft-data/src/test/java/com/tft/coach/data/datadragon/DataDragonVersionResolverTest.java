package com.tft.coach.data.datadragon;

import com.tft.coach.data.spi.AdapterFetchException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataDragonVersionResolverTest {

    @Test
    void resolvesExactAndPartialPatch() throws Exception {
        DataDragonVersionResolver resolver = new DataDragonVersionResolver(new DataDragonHttpClient() {
            @Override
            public byte[] getBytes(String url) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getVersionsJson() {
                return "[\"16.16.1\",\"14.23.1\"]";
            }
        });

        assertEquals("16.16.1", resolver.resolve("16.16.1"));
        assertEquals("14.23.1", resolver.resolve("14.23"));
        assertEquals("16.16.1", resolver.resolve(null));
    }

    @Test
    void dataJsonUrlUsesVersionAndLocale() {
        String url = DataDragonUrls.dataJson("16.16.1", "en_US", DataDragonResource.TRAIT);
        assertEquals("https://ddragon.leagueoflegends.com/cdn/16.16.1/data/en_US/tft-trait.json", url);
    }
}
