package org.dustyroom.be.models.state;

public enum ViewerTheme {
    DARK("Ayu Dark", "ayu-dark.properties"),
    LIGHT("Ayu Light", "ayu-light.properties");

    private final String label;
    private final String resource;

    ViewerTheme(String label, String resource) {
        this.label = label;
        this.resource = resource;
    }

    public String label() {
        return label;
    }

    public String resource() {
        return resource;
    }
}
