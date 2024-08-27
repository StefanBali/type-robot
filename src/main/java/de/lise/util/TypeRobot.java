package de.lise.util;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.NativeInputEvent;
import com.github.kwhat.jnativehook.dispatcher.SwingDispatchService;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

public class TypeRobot extends JFrame implements NativeKeyListener, WindowListener {
    private final JLabel globalShortcut;
    private boolean isInDefineShortcutMode;
    private Shortcut shortcut;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TypeRobot::new);
    }

    public TypeRobot() {
        setTitle("Type-Robot");
        setLayout(new BorderLayout());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(400, 120);
        setLocationRelativeTo(null);
        setResizable(false);
        addWindowListener(this);

        JLabel warning = new JLabel("The following keys will not work: ß?\\´`äÄöÖüÜ", SwingConstants.CENTER);
        warning.setFont(warning.getFont().deriveFont(Font.BOLD));
        warning.setPreferredSize(new Dimension(0, 30));
        add(warning, BorderLayout.NORTH);

        globalShortcut = new JLabel("Click to set a shortcut", SwingConstants.CENTER);
        globalShortcut.setPreferredSize(new Dimension(0, 50));
        globalShortcut.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                isInDefineShortcutMode = true;
                globalShortcut.setText("Type the shortcut");
            }
        });
        add(globalShortcut, BorderLayout.SOUTH);

        GlobalScreen.setEventDispatcher(new SwingDispatchService());

        setVisible(true);
    }

    @Override
    public void windowOpened(WindowEvent e) {
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException ex) {
            ex.printStackTrace();
        }
        GlobalScreen.addNativeKeyListener(this);
    }

    @Override
    public void windowClosed(WindowEvent e) {
        try {
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException ex) {
            ex.printStackTrace();
        }
        System.exit(0);
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        if (isInDefineShortcutMode) {
            shortcut = new Shortcut(e.getModifiers(), e.getKeyChar());
            globalShortcut.setText("Shortcut: " + NativeInputEvent.getModifiersText(e.getModifiers()) + "+" + e.getKeyChar());
            isInDefineShortcutMode = false;
            return;
        }
        if (shortcut == null) {
            return;
        }
        String textToType = getTextToType();
        if (textToType == null) {
            return;
        }
        if (e.getModifiers() == shortcut.modifiers() && e.getKeyChar() == shortcut.keyChar()) {
            Robot robot;
            try {
                robot = new Robot();
            } catch (AWTException ex) {
                ex.printStackTrace();
                return;
            }
            robot.delay(1000);
            textToType.chars().forEach(value -> {
                KeyCommand keyCommand = mapToKeyCommand(value);
                if (keyCommand.modifier() != null) {
                    robot.keyPress(keyCommand.modifier());
                }
                try {
                    robot.keyPress(keyCommand.keyEvent());
                } catch (IllegalArgumentException ex) {
                    if (keyCommand.modifier() != null) {
                        robot.keyRelease(keyCommand.modifier());
                    }
                    System.out.println("ERROR: " + (char) value);
                    return;
                }
                robot.keyRelease(keyCommand.keyEvent());
                robot.delay(10);
                if (keyCommand.modifier() != null) {
                    robot.keyRelease(keyCommand.modifier());
                }
            });
        }
    }

    private String getTextToType() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
            try {
                return (String) clipboard.getData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException | IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private KeyCommand mapToKeyCommand(int charValue) {
        if (Character.isUpperCase(charValue)) {
            return KeyCommand.shift(KeyEvent.getExtendedKeyCodeForChar(charValue));
        }
        return switch (charValue) {
            case '°' -> KeyCommand.shift(KeyEvent.VK_CIRCUMFLEX);
            case '!' -> KeyCommand.shift(KeyEvent.VK_1);
            case '"' -> KeyCommand.shift(KeyEvent.VK_2);
            case '§' -> KeyCommand.shift(KeyEvent.VK_3);
            case '$' -> KeyCommand.shift(KeyEvent.VK_4);
            case '%' -> KeyCommand.shift(KeyEvent.VK_5);
            case '&' -> KeyCommand.shift(KeyEvent.VK_6);
            case '/' -> KeyCommand.shift(KeyEvent.VK_7);
            case '(' -> KeyCommand.shift(KeyEvent.VK_8);
            case ')' -> KeyCommand.shift(KeyEvent.VK_9);
            case '=' -> KeyCommand.shift(KeyEvent.VK_0);
            case '>' -> KeyCommand.shift(KeyEvent.VK_LESS);
            case ';' -> KeyCommand.shift(KeyEvent.VK_COMMA);
            case ':' -> KeyCommand.shift(KeyEvent.VK_PERIOD);
            case '_' -> KeyCommand.shift(KeyEvent.VK_MINUS);
            case '*' -> KeyCommand.shift(KeyEvent.VK_PLUS);
            case '\'' -> KeyCommand.shift(KeyEvent.VK_NUMBER_SIGN);
            case '{' -> KeyCommand.altGraph(KeyEvent.VK_7);
            case '[' -> KeyCommand.altGraph(KeyEvent.VK_8);
            case ']' -> KeyCommand.altGraph(KeyEvent.VK_9);
            case '}' -> KeyCommand.altGraph(KeyEvent.VK_0);
            case '@' -> KeyCommand.altGraph(KeyEvent.VK_Q);
            case '€' -> KeyCommand.altGraph(KeyEvent.VK_E);
            case '|' -> KeyCommand.altGraph(KeyEvent.VK_LESS);
            case '~' -> KeyCommand.altGraph(KeyEvent.VK_PLUS);
            default -> KeyCommand.of(KeyEvent.getExtendedKeyCodeForChar(charValue));
        };
    }

    private record Shortcut(int modifiers, char keyChar) {
    }

    private record KeyCommand(Integer modifier, int keyEvent) {
        static KeyCommand of(int keyEvent) {
            return new KeyCommand(null, keyEvent);
        }

        static KeyCommand shift(int keyEvent) {
            return new KeyCommand(KeyEvent.VK_SHIFT, keyEvent);
        }

        static KeyCommand altGraph(int keyEvent) {
            return new KeyCommand(KeyEvent.VK_ALT_GRAPH, keyEvent);
        }
    }

    @Override
    public void windowActivated(WindowEvent e) { /* Do Nothing */ }
    @Override
    public void windowClosing(WindowEvent e) { /* Do Nothing */ }
    @Override
    public void windowDeactivated(WindowEvent e) { /* Do Nothing */ }
    @Override
    public void windowDeiconified(WindowEvent e) { /* Do Nothing */ }
    @Override
    public void windowIconified(WindowEvent e) { /* Do Nothing */ }
}
