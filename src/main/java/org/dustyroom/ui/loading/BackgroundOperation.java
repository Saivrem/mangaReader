package org.dustyroom.ui.loading;

import java.util.function.BooleanSupplier;

@FunctionalInterface
public interface BackgroundOperation<T> {
    T execute(BooleanSupplier cancelled) throws Exception;
}
