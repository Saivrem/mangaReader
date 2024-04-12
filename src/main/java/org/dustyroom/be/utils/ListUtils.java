package org.dustyroom.be.utils;

import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class ListUtils {

    public static <T> T getFirstOrDefault(List<T> list, T def) {
        if (list == null || list.isEmpty()) return def;
        return list.get(0);
    }

    public static <T> T getLastOrDefault(List<T> list, T def) {
        if (list == null || list.isEmpty()) return def;
        return list.get(list.size() - 1);
    }
}
