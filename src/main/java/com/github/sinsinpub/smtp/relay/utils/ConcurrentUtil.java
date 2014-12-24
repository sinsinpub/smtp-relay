package com.github.sinsinpub.smtp.relay.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.math.NumberUtils;

/**
 * 简化ConcurrentCollection和Atomic类型操作的工具
 * 
 * @author sin_sin
 */
public abstract class ConcurrentUtil {

    /**
     * 使ConcurrentMap内的AtomicLong+1
     * 
     * @param map
     * @param key
     */
    public static void incrementLong(ConcurrentMap<String, AtomicLong> map,
            String key) {
        map.putIfAbsent(key, new AtomicLong(0));
        map.get(key).getAndIncrement();
    }

    /**
     * 使ConcurrentMap内的AtomicLong增加指定差
     * 
     * @param map
     * @param key
     * @param delta
     */
    public static void addLong(ConcurrentMap<String, AtomicLong> map,
            String key, long delta) {
        map.putIfAbsent(key, new AtomicLong(0));
        map.get(key).getAndAdd(delta);
    }

    /**
     * 使ConcurrentMap内的AtomicLong置为指定值
     * 
     * @param map
     * @param key
     * @param newValue
     */
    public static void setLong(ConcurrentMap<String, AtomicLong> map,
            String key, long newValue) {
        map.putIfAbsent(key, new AtomicLong(0));
        map.get(key).getAndSet(newValue);
    }

    /**
     * 将计数器ConcurrentMap转换为字符串Map
     * 
     * @param map
     * @return
     */
    public static Map<String, String> toStringMap(ConcurrentMap<String, ?> map) {
        Map<String, String> temp = new HashMap<String, String>(map.size());
        for (Entry<String, ?> entry : map.entrySet()) {
            temp.put(entry.getKey(), entry.getValue().toString());
        }
        return temp;
    }

    /**
     * Gets a string list based on an iterator.
     * <p>
     * As the wrapped Iterator is traversed, an LinkedList of its string values is
     * created. At the end, the list is returned.
     * 
     * @param iterator the iterator to use, not null
     * @return a list of the iterator string contents
     * @throws NullPointerException if iterator parameter is null
     */
    public static List<String> toStringList(Iterator<?> iterator) {
        if (iterator == null) {
            throw new NullPointerException("Iterator must not be null");
        }
        List<String> list = new LinkedList<String>();
        while (iterator.hasNext()) {
            list.add(String.valueOf(iterator.next()));
        }
        return list;
    }

    /**
     * Sort map order by number type values of entries
     * 
     * @param <K> type of map key
     * @param <V> number type of map value
     * @param map map to sort
     * @param order >=0 means ascend, <0 means descend
     * @return Sorted list of entry string
     */
    public static <K, V extends Number> List<String> sortEntriesByNumberValues(
            Map<K, V> map, final int order) {
        if (map == null)
            return new ArrayList<String>(0);
        List<Map.Entry<K, V>> list = new ArrayList<Map.Entry<K, V>>(
                map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {
                V n1 = e1.getValue();
                V n2 = e2.getValue();
                int orderSign = order >= 0 ? 1 : -1;
                if (n1 == null && n2 == null)
                    return 0;
                else if (n1 == null || n2 == null)
                    return n1 == null ? orderSign : -orderSign;
                return NumberUtils.compare(n1.doubleValue(), n2.doubleValue())
                        * orderSign;
            }
        });
        return toStringList(list.iterator());
    }

}
