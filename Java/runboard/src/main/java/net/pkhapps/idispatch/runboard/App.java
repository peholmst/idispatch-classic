package net.pkhapps.idispatch.runboard;


import net.pkhapps.idispatch.runboard.client.ServerConnection;

import javax.swing.*;

public class App {

    private MainView mainView;
    private ServerConnection connection;
    private Model model;

    private App() {
        final Configuration configuration = new Configuration();
        model = new Model();
        connection = new ServerConnection(configuration, model);
        mainView = new MainView(configuration.isLowResolution());
        mainView.setModel(model);
        mainView.setUndecorated(configuration.isUndecorated());
        mainView.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final Alarm alarm = new Alarm(new Speech(configuration), configuration.getAlertTimings());
        model.addObserver(alarm);
        // Release the speech engine's native resources before the JVM exits, or their destructors can crash it.
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().name("shutdown").unstarted(alarm::close));
    }

    public static void main(String[] args) throws Exception {
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
