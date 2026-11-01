package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Svg;

import static java.util.Objects.requireNonNull;

/**
 * Shows a text as a QR code, drawn on the server as SVG. Dark modules on a white background with the standard quiet
 * zone, whatever the theme, so that phone cameras read it.
 */
public class QrCode extends Composite<Svg> {

    private static final int QUIET_ZONE = 4;

    private final String text;

    /**
     * @param text      the text to encode, such as a link
     * @param ariaLabel what the code is, for screen readers
     */
    public QrCode(String text, String ariaLabel) {
        this.text = requireNonNull(text);
        var svg = getContent();
        svg.setSvg(toSvg(io.nayuki.qrcodegen.QrCode.encodeText(text, io.nayuki.qrcodegen.QrCode.Ecc.MEDIUM)));
        svg.addClassName("qr-code");
        svg.getElement().setAttribute("role", "img");
        svg.getElement().setAttribute("aria-label", requireNonNull(ariaLabel));
    }

    /**
     * The text the code encodes.
     */
    public String getText() {
        return text;
    }

    static String toSvg(io.nayuki.qrcodegen.QrCode qr) {
        var size = qr.size + QUIET_ZONE * 2;
        var path = new StringBuilder();
        for (var y = 0; y < qr.size; y++) {
            for (var x = 0; x < qr.size; x++) {
                if (qr.getModule(x, y)) {
                    path.append('M').append(x + QUIET_ZONE).append(',').append(y + QUIET_ZONE).append("h1v1h-1z");
                }
            }
        }
        return """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" shape-rendering="crispEdges">\
                <rect width="100%%" height="100%%" fill="#ffffff"/><path d="%s" fill="#000000"/></svg>"""
                .formatted(size, size, path);
    }
}
