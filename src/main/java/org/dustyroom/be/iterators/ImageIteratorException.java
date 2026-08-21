package org.dustyroom.be.iterators;

import lombok.Getter;

import java.io.File;

@Getter
public class ImageIteratorException extends RuntimeException {
    private final File source;

    public ImageIteratorException(String message, File source) {
        super(message);
        this.source = source;
    }

    public ImageIteratorException(String message, File source, Throwable cause) {
        super(message, cause);
        this.source = source;
    }

}
