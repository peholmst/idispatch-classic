package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.client.DispatcherClientException;
import net.pkhapps.idispatch.runboard.client.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

class MainView extends JFrame implements Observer {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Timer cardFlipperTimer;
    private final Map<Notification, NotificationView> views = new HashMap<>();
    private final boolean lowRes;
    private final Cursor blankCursor;
    private Model model;
    private final JTabbedPane notifications;
    private final ClockView clockView = new ClockView();
    private JDialog errorDialog;

    MainView(boolean lowRes, boolean hideCursor) {
        super("iDispatch Runboard");
        this.lowRes = lowRes;
        blankCursor = hideCursor ? Toolkit.getDefaultToolkit()
                .createCustomCursor(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), new Point(), "blank")
                : null;
        setExtendedState(MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(400, 400));
        notifications = new JTabbedPane();
        add(notifications, BorderLayout.CENTER);
        cardFlipperTimer = new Timer(10000, e -> flipCard());
        cardFlipperTimer.start();
        setGlassPane(clockView);
        hideCursorIn(this);
    }

    /**
     * Gives the component and everything in it the blank cursor, when the cursor is hidden. Components such as text
     * panes set a cursor of their own instead of inheriting the window's, so each one must be given it.
     */
    private void hideCursorIn(Component component) {
        if (blankCursor == null) {
            return;
        }
        component.setCursor(blankCursor);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                hideCursorIn(child);
            }
        }
    }

    private void flipCard() {
        if (notifications.getTabCount() > 0) {
            notifications.setSelectedIndex((notifications.getSelectedIndex() + 1) % notifications.getTabCount());
        }
    }

    public void setModel(Model model) {
        if (this.model != null) {
            this.model.deleteObserver(this);
        }
        this.model = model;
        if (this.model != null) {
            this.model.addObserver(this);
        }
        update(model, null);
    }

    private void showErrorMessage(DispatcherClientException.ErrorCode errorCode) {
        if (errorDialog != null) {
            clearErrorMessage();
        }

        JPanel errorPanel = new JPanel();
        errorPanel.setBackground(Color.RED);
        errorPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel errorLabel = new JLabel(errorCode.name());
        errorLabel.setFont(errorPanel.getFont().deriveFont(20.0f));
        errorPanel.add(errorLabel);

        errorDialog = new JDialog(this);
        errorDialog.setAlwaysOnTop(true);
        errorDialog.setModal(false);
        errorDialog.setUndecorated(true);
        errorDialog.add(errorPanel);
        hideCursorIn(errorDialog);
        errorDialog.pack();
        errorDialog.setLocationRelativeTo(this);
        errorDialog.setVisible(true);
    }

    private void clearErrorMessage() {
        if (errorDialog != null) {
            errorDialog.setVisible(false);
            errorDialog.dispose();
            errorDialog = null;
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        logger.info("Model updated");
        if (model != null && model.hasError()) {
            showErrorMessage(model.getErrorCode());
        } else {
            clearErrorMessage();
        }
        Set<Notification> notificationsToRemove = new HashSet<>(views.keySet());
        if (model != null) {
            model.getVisibleNotifications().stream().filter(n -> !notificationsToRemove.remove(n)).forEach(this::addCard);
        }
        notificationsToRemove.forEach(this::removeCard);
        clockView.setVisible(model.getVisibleNotifications().isEmpty());
    }

    private void removeCard(Notification notification) {
        logger.info("Removing card for notification {}", notification);
        NotificationView view = views.remove(notification);
        notifications.remove(view);
    }

    private void addCard(Notification notification) {
        logger.info("Adding card for notification {}", notification);
        final NotificationView view = new NotificationView(notification, lowRes);
        hideCursorIn(view);
        views.put(notification, view);
        notifications.addTab(String.format("Assignment %d (Notification %d)", notification.getAssignmentId(), notification.getId()), view);
    }
}
