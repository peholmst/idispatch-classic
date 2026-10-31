package net.pkhapps.idispatch.runboard;


import net.pkhapps.idispatch.runboard.client.ServerConnection;

import javax.swing.*;

public class App {

    private MainView mainView;
    private ServerConnection connection;
    private Model model;

    private App() {
        final Configuration configuration = new Configuration();
        model = new Model(configuration.getNotificationShownFor());
        connection = new ServerConnection(configuration, model);
        mainView = new MainView(configuration.isLowResolution(), configuration.isCursorHidden());
        mainView.setModel(model);
        mainView.setUndecorated(configuration.isUndecorated());
        mainView.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final Outputs outputs = new Outputs(configuration);
        outputs.initialize();
        // The flashing light is on while notifications are shown.
        model.addObserver((o, arg) -> outputs.setLight(!model.getVisibleNotifications().isEmpty()));
        final Alarm alarm = new Alarm(new Speech(configuration), outputs, configuration.getAlertTimings(),
                configuration.getAlarmVolume());
        model.addObserver(alarm);
        // Release the speech engine's native resources before the JVM exits, or their destructors can crash it, and
        // leave the amplifier and the flashing light off.
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().name("shutdown").unstarted(() -> {
            alarm.close();
            outputs.close();
        }));
    }

    public static void main(String[] args) throws Exception {
        // Without a desktop environment, as in a kiosk on a Raspberry Pi, Java on Linux draws text without antialiasing.
        if (System.getProperty("os.name").startsWith("Linux")
                && System.getProperty("awt.useSystemAAFontSettings") == null) {
            System.setProperty("awt.useSystemAAFontSettings", "on");
        }
        UIManager.setLookAndFeel(
                UIManager.getCrossPlatformLookAndFeelClassName());
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new App().start();
            }
        });
    }

    void start() {
        mainView.setVisible(true);
        connection.start();
    }
}
