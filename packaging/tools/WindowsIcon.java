import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/** Encode a multi-size, PNG-backed ICO using only the JDK (no packaging-time Python dependencies). */
public class WindowsIcon {
    public static void main(String[] args) throws Exception {
        BufferedImage source = ImageIO.read(Path.of(args[0]).toFile());
        int[] sizes = {16, 24, 32, 48, 64, 128, 256};
        var encoded = new ArrayList<byte[]>();
        for (int size : sizes) {
            BufferedImage current = source;
            while (current.getWidth() > size) {
                int nextSize = Math.max(size, current.getWidth() / 2);
                BufferedImage next = new BufferedImage(nextSize, nextSize, BufferedImage.TYPE_INT_ARGB_PRE);
                Graphics2D graphics = next.createGraphics();
                try {
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    graphics.drawImage(current, 0, 0, nextSize, nextSize, null);
                } finally { graphics.dispose(); }
                current = next;
            }
            var bytes = new ByteArrayOutputStream();
            ImageIO.write(current, "png", bytes);
            encoded.add(bytes.toByteArray());
        }
        int offset = 6 + sizes.length * 16;
        var buffer = ByteBuffer.allocate(offset + encoded.stream().mapToInt(b -> b.length).sum()).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) 0).putShort((short) 1).putShort((short) sizes.length);
        for (int i = 0; i < sizes.length; i++) {
            buffer.put((byte) sizes[i]).put((byte) sizes[i]).put((byte) 0).put((byte) 0);
            buffer.putShort((short) 1).putShort((short) 32).putInt(encoded.get(i).length).putInt(offset);
            offset += encoded.get(i).length;
        }
        for (byte[] bytes : encoded) buffer.put(bytes);
        Path output = Path.of(args[1]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.write(output, buffer.array());
    }
}
