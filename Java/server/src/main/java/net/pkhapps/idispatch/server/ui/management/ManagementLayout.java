package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RouterLayout;
import net.pkhapps.idispatch.server.ui.HasNavbarContent;
import net.pkhapps.idispatch.server.ui.RootLayout;
import net.pkhapps.idispatch.server.ui.TabUtils;

@JsModule("@vaadin/vaadin-lumo-styles/presets/compact.js")
@ParentLayout(RootLayout.class)
public class ManagementLayout extends VerticalLayout implements RouterLayout, HasNavbarContent {

    private final Tabs views;

    public ManagementLayout() {
        views = new Tabs();
        views.setOrientation(Tabs.Orientation.HORIZONTAL);
        views.addThemeVariants(TabsVariant.LUMO_MINIMAL);
        views.add(
                TabUtils.createNavigationTab("Municipalities", MunicipalityManagementView.class),
                TabUtils.createNavigationTab("Assignment Types", AssignmentTypeManagementView.class),
                TabUtils.createNavigationTab("Resources", ResourceManagementView.class),
                TabUtils.createNavigationTab("Resource Types", ResourceTypeManagementView.class),
                TabUtils.createNavigationTab("Destinations", DestinationManagementView.class),
                TabUtils.createNavigationTab("Users", UserManagementView.class)
        );

        getStyle().set("background-color", "var(--lumo-shade-10pct)");

        setSizeFull();
        setPadding(false);
    }

    @Override
    public Component getNavbarContent() {
        return views;
    }

    @Override
    public String getTitle() {
        return "Management Console";
    }
}
