package com.nihao.client;

//#if MC < V26_3
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
//#else
import java.awt.FileDialog;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
//#endif

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Where "Download" puts a screenshot: a native "Save as" dialog that starts in the Downloads folder. Blocks, so call it off the render thread.
 * Up to 26.2 it is LWJGL tinyfd. 26.3 dropped tinyfd (and GLFW), and AWT is headless inside the game (vanilla Main sets java.awt.headless),
 * so there the dialog is an AWT FileDialog in a separate JVM, as {@link LoginWindow} does for the sign-in window.
 */
final class SaveDialog {
    private SaveDialog() {}

    /** The chosen path, or null when cancelled. */
    static String png(String fileName) {
        Path downloads = Path.of(System.getProperty("user.home"), "Downloads");
        Path start = (Files.isDirectory(downloads) ? downloads : Path.of(System.getProperty("user.home"))).resolve(fileName);
//#if MC < V26_3
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filter = stack.mallocPointer(1);
            filter.put(stack.UTF8("*.png")).flip();
            return TinyFileDialogs.tinyfd_saveFileDialog(Lang.t("screenshots.save_title"), start.toString(), filter, Lang.t("screenshots.png_image"));
        }
//#else
        return inOtherJvm(start);
//#endif
    }

//#if MC >= V26_3
    /**
     * The JVM needs only this class (JDK calls only), which is copied out of the mod to a temporary folder: it works from a jar and from a dev
     * folder, with either loader. shortcut: when the dialog cannot be shown (no display, no AWT) the file goes to Downloads without asking.
     */
    private static String inOtherJvm(Path start) {
        Path dir = null;
        try {
            dir = Files.createTempDirectory("nihao-save");
            Path cls = dir.resolve("com/nihao/client/SaveDialog.class");
            Files.createDirectories(cls.getParent());
            try (InputStream in = SaveDialog.class.getResourceAsStream("SaveDialog.class")) {
                Files.copy(in, cls);
            }
            String javaBin = Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java").toString();
            Process proc = new ProcessBuilder(javaBin, "-Djava.awt.headless=false", "-cp", dir.toString(), SaveDialog.class.getName(),
                    start.getParent().toString(), start.getFileName().toString(), Lang.t("screenshots.save_title")).redirectError(ProcessBuilder.Redirect.INHERIT).start();
            String out = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
            if (proc.waitFor() != 0) return start.toString();
            return out.isEmpty() ? null : new String(Base64.getDecoder().decode(out), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return start.toString();
        } finally {
            if (dir != null) {
                try (var files = Files.walk(dir)) {
                    files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
                } catch (Exception ignored) {
                    // a temporary folder left behind is harmless
                }
            }
        }
    }

    /** Entry point of that JVM: prints the chosen path (Base64, so any characters survive), nothing when cancelled. */
    public static void main(String[] args) {
        FileDialog d = new FileDialog((java.awt.Frame) null, args[2], FileDialog.SAVE);
        d.setDirectory(args[0]);
        d.setFile(args[1]);
        d.setAlwaysOnTop(true); // above the game window
        d.setVisible(true);
        if (d.getFile() != null) {
            System.out.println(Base64.getEncoder().encodeToString(Path.of(d.getDirectory(), d.getFile()).toString().getBytes(StandardCharsets.UTF_8)));
        }
        System.exit(0);
    }
//#endif
}
