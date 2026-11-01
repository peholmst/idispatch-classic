package net.pkhapps.idispatch.runboard;

import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;
import java.util.concurrent.CompletableFuture;

class ClockView extends JComponent {

    // The second hand jumps once a second, so repaint once a second, just after the second changes.
    private final Timer repaintTimer = new Timer(0, e -> {
        repaint();
        scheduleRepaint();
    });

    // The address can change (DHCP, a cable plugged into another network), so look it up again now and then. The lookup
    // goes through the network interfaces, so it runs off the event dispatch thread.
    private final Timer addressTimer = new Timer(30_000, e -> lookUpAddress());
    private volatile String address = "";

    public ClockView() {
        repaintTimer.setRepeats(false);
        addressTimer.setInitialDelay(0);
        scheduleRepaint();
        addressTimer.start();
    }

    private void lookUpAddress() {
        CompletableFuture.supplyAsync(LocalAddresses::lookup).thenAccept(found -> SwingUtilities.invokeLater(() -> {
            if (!found.equals(address)) {
                address = found;
                repaint();
            }
        }));
    }

    private void scheduleRepaint() {
        repaintTimer.setInitialDelay((int) (1010 - System.currentTimeMillis() % 1000));
        repaintTimer.restart();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            scheduleRepaint();
            addressTimer.restart();
        } else {
            repaintTimer.stop();
            addressTimer.stop();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        var g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        var now = LocalTime.now();

        var side = Math.min(g.getClipBounds().width, g.getClipBounds().height) - 20;
        var x = (g.getClipBounds().width - side) / 2;
        var y = (g.getClipBounds().height - side) / 2;
        var radius = side / 2;

        var origo_x = g.getClipBounds().width / 2;
        var origo_y = g.getClipBounds().height / 2;

        g2.setBackground(Color.BLACK);
        g2.fillRect(0, 0, g.getClipBounds().width, g.getClipBounds().height);

        // Fill the face of the clock
        {
            g2.setColor(Color.LIGHT_GRAY);
            g2.setStroke(new BasicStroke(11));
            g2.fillOval(x, y, side, side);
        }

        // Draw tick marks
        {
            g2.setStroke(new BasicStroke(21, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(Color.BLACK);
            for (int i = 0; i < 360; i += 30) {
                var radians = i * Math.PI / 180;
                var sin = Math.sin(radians);
                var cos = Math.cos(radians);
                var y1 = (int) (origo_y - (radius - 2) * sin);
                var y2 = (int) (origo_y - (radius - 10) * sin);
                var x1 = (int) (origo_x + (radius - 2) * cos);
                var x2 = (int) (origo_x + (radius - 10) * cos);
                g2.drawLine(x1, y1, x2, y2);
            }
        }

        // Draw the hour hand
        {
            g2.setStroke(new BasicStroke(31, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(Color.BLACK);
            var hourAngle = (((now.getHour() % 12) * 60 + now.getMinute()) / 720d) * Math.PI * 2;
            var hourLength = radius * 0.5;
            var sin = Math.sin(hourAngle);
            var cos = Math.cos(hourAngle);
            var x2 = (int) (origo_x + hourLength * sin);
            var y2 = (int) (origo_y - hourLength * cos);
            g2.drawLine(origo_x, origo_y, x2, y2);
        }

        // Draw the minute hand
        {
            g2.setStroke(new BasicStroke(21, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(Color.DARK_GRAY);
            var minuteAngle = (now.getMinute() / 60d) * Math.PI * 2;
            var minuteLength = radius * 0.9;
            var sin = Math.sin(minuteAngle);
            var cos = Math.cos(minuteAngle);
            var x2 = (int) (origo_x + minuteLength * sin);
            var y2 = (int) (origo_y - minuteLength * cos);
            g2.drawLine(origo_x, origo_y, x2, y2);
        }

        // Draw the second hand
        {
            g2.setStroke(new BasicStroke(9, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(Color.RED);
            var secondAngle = (now.getSecond() / 60d) * Math.PI * 2;
            var secondLength = radius * 0.8;
            var sin = Math.sin(secondAngle);
            var cos = Math.cos(secondAngle);
            var x2 = (int) (origo_x + secondLength * sin);
            var y2 = (int) (origo_y - secondLength * cos);
            g2.drawLine(origo_x, origo_y, x2, y2);
        }

        // Show the IP address in the bottom left corner, for whoever needs to log in over SSH for maintenance
        {
            var text = address.isEmpty() ? "No IP address" : address;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(getFont().deriveFont(Font.PLAIN, 14f));
            g2.setColor(Color.GRAY);
            g2.drawString(text, 10, getHeight() - 10 - g2.getFontMetrics().getDescent());
        }
    }
}
