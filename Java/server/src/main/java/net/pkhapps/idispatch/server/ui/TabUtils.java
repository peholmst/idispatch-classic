package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.router.RouterLink;

public final class TabUtils {

    private TabUtils() {
    }

    public static Tab createNavigationTab(String viewName, Class<? extends Component> viewClass) {
        var link = new RouterLink(viewName, viewClass);
        link.setTabIndex(-1);
        return new Tab(link);
    }
}
