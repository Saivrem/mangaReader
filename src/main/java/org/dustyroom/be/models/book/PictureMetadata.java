package org.dustyroom.be.models.book;

import lombok.*;
import lombok.experimental.Accessors;

import java.io.File;

@Getter
@EqualsAndHashCode
@ToString
@Setter
@Accessors(chain = true)
public final class PictureMetadata {
    private String name;
    private String fileName;
    private File dir;

    @Builder
    public PictureMetadata(String name, String fileName, File dir) {
        this.name = name;
        this.fileName = fileName;
        this.dir = dir;
    }

}
