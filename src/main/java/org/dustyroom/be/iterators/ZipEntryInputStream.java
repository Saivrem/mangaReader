package org.dustyroom.be.iterators;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;

final class ZipEntryInputStream extends FilterInputStream {
    private final ZipFile zip;

    ZipEntryInputStream(InputStream input, ZipFile zip) {
        super(input);
        this.zip = zip;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            zip.close();
        }
    }

}
