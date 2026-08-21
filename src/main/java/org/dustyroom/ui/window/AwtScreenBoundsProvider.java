package org.dustyroom.ui.window;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;

final class AwtScreenBoundsProvider implements ScreenBoundsProvider {
    @Override
    public List<ScreenBounds> screens() {
        GraphicsDevice[] devices = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getScreenDevices();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        List<ScreenBounds> result = new ArrayList<>(devices.length);

        for (GraphicsDevice device : devices) {
            GraphicsConfiguration configuration = device.getDefaultConfiguration();
            Rectangle bounds = configuration.getBounds();
            Insets insets = toolkit.getScreenInsets(configuration);
            result.add(new ScreenBounds(device, bounds, WindowGeometry.usableBounds(bounds, insets)));
        }
        return List.copyOf(result);
    }
}
