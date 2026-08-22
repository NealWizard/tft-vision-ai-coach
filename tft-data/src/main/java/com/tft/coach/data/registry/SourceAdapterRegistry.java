package com.tft.coach.data.registry;

import com.tft.coach.data.spi.FetchRequest;
import com.tft.coach.data.spi.SourceAdapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Registry of {@link SourceAdapter} implementations keyed by adapter id.
 */
public class SourceAdapterRegistry {

    private final List<SourceAdapter> adapters;

    public SourceAdapterRegistry(List<SourceAdapter> adapters) {
        this.adapters = List.copyOf(adapters);
    }

    public Collection<SourceAdapter> all() {
        return adapters;
    }

    public Optional<SourceAdapter> resolve(FetchRequest request) {
        return adapters.stream()
                .filter(adapter -> adapter.supports(request))
                .findFirst();
    }

    public Optional<SourceAdapter> findById(String adapterId) {
        return adapters.stream()
                .filter(adapter -> adapter.adapterId().equals(adapterId))
                .findFirst();
    }
}
