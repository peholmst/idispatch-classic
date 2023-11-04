package net.pkhapps.idispatch.runboard;

import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;

public class ClockView extends JComponent {

    private final Timer repaintTimer = new Timer(1000, e -> repaint());

    public ClockView() {
        repaintTimer.start();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            repaintTimer.start();
        } else {
            repaintTimer.stop();
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
            var hourAngle = ((now.getHour() % 12) / 12d) * Math.PI * 2;
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
    }
}
