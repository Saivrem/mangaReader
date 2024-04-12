package org.dustyroom.be.models;

import java.io.File;

public record PictureMetadata(String name, String fileName, File dir) {
}
