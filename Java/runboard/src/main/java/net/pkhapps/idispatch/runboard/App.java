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
        model.addObserver(new Alarm(new Speech(configuration), configuration.getAlertTimings()));
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
