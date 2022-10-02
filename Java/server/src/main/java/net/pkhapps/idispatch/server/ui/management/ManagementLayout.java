package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;

@JsModule("@vaadin/vaadin-lumo-styles/presets/compact.js")
public class ManagementLayout extends AppLayout implements RouterLayout {

    public ManagementLayout() {
        var toggle = new DrawerToggle();
        var title = new H1("iDispatch Management Console");
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0");
        var tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.add(
                createTab(VaadinIcon.BUILDING, "Municipalities", MunicipalityManagementView.class),
                createTab(VaadinIcon.FIRE, "Assignment Types", AssignmentTypeManagementView.class),
                createTab(VaadinIcon.AMBULANCE, "Resources", ResourceManagementView.class),
                createTab(VaadinIcon.SWORD, "Resource Types", ResourceTypeManagementView.class),
                createTab(VaadinIcon.BOLT, "Destinations", DestinationManagementView.class)
        );

        addToDrawer(tabs);
        addToNavbar(toggle, title);
    }

    private Tab createTab(VaadinIcon viewIcon, String viewName, Class<? extends Component> viewClass) {
        var icon = viewIcon.create();
        icon.getStyle()
                .set("box-sizing", "border-box")
                .set("margin-inline-end", "var(--lumo-space-m)")
                .set("margin-inline-start", "var(--lumo-space-xs)")
                .set("padding", "var(--lumo-space-xs)");

        var link = new RouterLink();
        link.add(icon, new Span(viewName));
        link.setRoute(viewClass);
        link.setTabIndex(-1);

        return new Tab(link);
    }
}
