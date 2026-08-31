package com.tft.coach.meta;

import com.tft.coach.data.meta.CompStat;
import com.tft.coach.data.meta.MetaSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MetaTrendService {

    public Optional<MetaTrend> trend(StoredMetaSnapshot from, StoredMetaSnapshot to) {
        if (from == null || to == null) {
            return Optional.empty();
        }
        MetaSnapshot a = from.snapshot();
        MetaSnapshot b = to.snapshot();
        Map<String, CompStat> before = index(a);
        Map<String, CompStat> after = index(b);
        List<MetaTrend.CompTrend> deltas = new ArrayList<>();
        for (String id : after.keySet()) {
            CompStat next = after.get(id);
            CompStat prev = before.get(id);
            if (prev == null) {
                continue;
            }
            deltas.add(new MetaTrend.CompTrend(
                    id,
                    next.pickRate() - prev.pickRate(),
                    next.top4Rate() - prev.top4Rate()));
        }
        List<String> evidence = List.of(
                "evidence:meta:" + from.id(),
                "evidence:meta:" + to.id());
        return Optional.of(new MetaTrend(b.patch(), a.timeWindow(), b.timeWindow(), deltas, evidence));
    }

    private static Map<String, CompStat> index(MetaSnapshot snapshot) {
        Map<String, CompStat> map = new LinkedHashMap<>();
        for (CompStat comp : snapshot.comps()) {
            map.put(comp.compId(), comp);
        }
        return map;
    }
}
