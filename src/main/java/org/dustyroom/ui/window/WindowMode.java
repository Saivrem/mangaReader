package org.dustyroom.ui.window;

/**
 * Application-owned state of the undecorated viewer window.
 * Iconification is deliberately not represented here because it is managed by
 * the host window manager through {@link java.awt.Frame#ICONIFIED}.
 */
public enum WindowMode {
    NORMAL,
    MAXIMIZED,
    FULLSCREEN
}
