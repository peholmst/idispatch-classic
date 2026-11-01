package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.client.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

class MainView extends JFrame implements Observer {

    private static final String CLOCK = "clock";
    private static final String NOTIFICATIONS = "notifications";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Timer cardFlipperTimer;
    private final Map<Notification, NotificationView> views = new HashMap<>();
    private final boolean lowRes;
    private final Cursor blankCursor;
    private Model model;
    private final JTabbedPane notifications;
    private final ClockView clockView = new ClockView();
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final JLabel errorBanner = new JLabel();

    MainView(boolean lowRes, boolean hideCursor) {
        super("iDispatch Runboard");
        this.lowRes = lowRes;
        blankCursor = hideCursor ? Toolkit.getDefaultToolkit()
                .createCustomCursor(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), new Point(), "blank")
                : null;
        setExtendedState(MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(400, 400));
        notifications = new JTabbedPane();
        cardPanel.add(clockView, CLOCK);
        cardPanel.add(notifications, NOTIFICATIONS);
        add(cardPanel, BorderLayout.CENTER);
        // The error is shown in the main window, not in a window of its own: in a kiosk (cage on a Raspberry Pi), only
        // the main window is on screen.
        errorBanner.setOpaque(true);
        errorBanner.setBackground(Color.RED);
        errorBanner.setForeground(Color.WHITE);
        errorBanner.setHorizontalAlignment(SwingConstants.CENTER);
        errorBanner.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        errorBanner.setFont(errorBanner.getFont().deriveFont(Font.BOLD, 20.0f));
        errorBanner.setVisible(false);
        add(errorBanner, BorderLayout.NORTH);
        cardFlipperTimer = new Timer(10000, e -> flipCard());
        cardFlipperTimer.start();
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

    @Override
    public void update(Observable o, Object arg) {
        // The model is updated from the server connection's thread too, but Swing must only be touched on the event
        // dispatch thread.
        if (SwingUtilities.isEventDispatchThread()) {
            refresh();
        } else {
            SwingUtilities.invokeLater(this::refresh);
        }
    }

    private void refresh() {
        logger.info("Model updated");
        if (model != null && model.hasError()) {
            errorBanner.setText(switch (model.getErrorCode()) {
                case COMMUNICATION_ERROR -> "No connection to the server";
                case UNKNOWN_SERVER_ERROR -> "The server reported an error";
            });
            errorBanner.setVisible(true);
        } else {
            errorBanner.setVisible(false);
        }
        List<Notification> visible = model != null ? model.getVisibleNotifications() : List.of();
        Set<Notification> notificationsToRemove = new HashSet<>(views.keySet());
        visible.stream().filter(n -> !notificationsToRemove.remove(n)).forEach(this::addCard);
        notificationsToRemove.forEach(this::removeCard);
        cards.show(cardPanel, visible.isEmpty() ? CLOCK : NOTIFICATIONS);
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
