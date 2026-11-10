package org.dustyroom.be.models.book;

import java.io.File;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

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
