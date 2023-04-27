package com.namsung.xgpsmanager.data;

import com.namsung.xgpsmanager.XGPSParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SatellitesInfoMap<K1, K2, K3, V> implements Cloneable {
    private ConcurrentHashMap<K1, HashMap<K2, HashMap<K3, V>>> map;

    public SatellitesInfoMap() {
        map = new ConcurrentHashMap<>();
    }

    public SatellitesInfoMap clone() {
        try {
            SatellitesInfoMap obj = (SatellitesInfoMap) super.clone();
            return obj;
        } catch (CloneNotSupportedException ignored) {}
        return null;
    }

    public void put(K1 key1, K2 key2, K3 key3, V value) {
        HashMap<K2, HashMap<K3, V>> innerMap1 = map.get(key1);
        if (innerMap1 == null) {
            innerMap1 = new HashMap<>();
            map.put(key1, innerMap1);
        }
        HashMap<K3, V> innerMap2 = innerMap1.get(key2);
        if (innerMap2 == null) {
            innerMap2 = new HashMap<>();
            innerMap1.put(key2, innerMap2);
        }
        innerMap2.put(key3, value);
    }

    public void put(K1 key1, K2 key2, HashMap<K3, V> map3) {
        HashMap<K2, HashMap<K3, V>> innerMap1 = map.get(key1);
        if (innerMap1 == null) {
            innerMap1 = new HashMap<>();
            map.put(key1, innerMap1);
        }
        innerMap1.put(key2, map3);
    }

    public Set<K1> keySet() {
        return map.keySet();
    }

    public V get(K1 key1, K2 key2, K3 key3) {
        HashMap<K2, HashMap<K3, V>> innerMap1 = map.get(key1);
        if (innerMap1 == null) {
            return null;
        }
        Map<K3, V> innerMap2 = innerMap1.get(key2);
        if (innerMap2 == null) {
            return null;
        }
        return innerMap2.get(key3);
    }

    public Map<K3, V> get(K1 key1, K2 key2) {
        HashMap<K2, HashMap<K3, V>> innerMap1 = map.get(key1);
        if (innerMap1 == null) {
            return null;
        }
        HashMap<K3, V> innerMap2 = innerMap1.get(key2);
        if (innerMap2 == null) {
            return null;
        }
        return innerMap2;
    }

    public List<K2> getKeyList(K1 key1) {
        HashMap<K2, HashMap<K3, V>> innerMap1 = map.get(key1);
        if (innerMap1 == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(innerMap1.keySet());
    }

    public List<K3> getKeyList(K1 key1, K2 key2) {
        HashMap<K2, HashMap<K3, V>> innerMap1 = map.get(key1);
        if (innerMap1 == null) {
            return new ArrayList<>();
        }
        HashMap<K3, V> innerMap2 = innerMap1.get(key2);
        if (innerMap2 == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(innerMap2.keySet());
    }

    public HashMap<K3, V> getAll(K1 key) {
        HashMap<K2, HashMap<K3, V>> innerMap1 = (HashMap<K2, HashMap<K3, V>>) map.get(key).clone();
        if (innerMap1 == null) {
            return null;
        }

        HashMap<K3, V> mergedMap = new HashMap<>();
        List<K2> keys = new ArrayList<>(innerMap1.keySet());
        // TODO : merge 2 signals
        for (K2 k : keys) {
            Map<K3, V> map3 = (Map<K3, V>) innerMap1.get(k).clone();
            if (map3 == null) continue;
            for (K3 k3 : map3.keySet()) {
                if (mergedMap.containsKey(k3)) {
                    if (((SatellitesInfo) Objects.requireNonNull(mergedMap.get(k3))).SNR < ((SatellitesInfo) Objects.requireNonNull(map3.get(k3))).SNR) {
                        mergedMap.put(k3, map3.get(k3));
                    }
                } else {
                    mergedMap.put(k3, map3.get(k3));
                }
            }
        }

        return mergedMap;
    }
}
