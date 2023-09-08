package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.router.RouterLink;

public final class TabUtils {

    private TabUtils() {
    }

    public static Tab createNavigationTab(String viewName, Class<? extends Component> viewClass) {
        return createNavigationTab(null, viewName, viewClass);
    }


    public static Tab createNavigationTab(VaadinIcon viewIcon, String viewName, Class<? extends Component> viewClass) {
        var link = new RouterLink();
        if (viewIcon != null) {
            var icon = viewIcon.create();
            icon.getStyle()
                    .set("box-sizing", "border-box")
                    .set("margin-inline-end", "var(--lumo-space-m)")
                    .set("padding", "var(--lumo-space-xs)");
            link.add(icon, new Span(viewName));
        } else {
            link.add(viewName);
        }

        link.setRoute(viewClass);
        link.setTabIndex(-1);

        return new Tab(link);
    }
}
