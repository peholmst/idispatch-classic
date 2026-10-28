package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.theme.lumo.Lumo;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.HasNavbarContent;
import net.pkhapps.idispatch.server.ui.RootLayout;
import net.pkhapps.idispatch.server.ui.TabUtils;

import java.util.List;
import java.util.Optional;

@StyleSheet(Lumo.COMPACT_STYLESHEET)
@ParentLayout(RootLayout.class)
@RolesAllowed(Roles.ROLE_ADMIN)
public class ManagementLayout extends VerticalLayout implements RouterLayout, HasNavbarContent {

    private static final List<Section> SECTIONS = List.of(
            new Section("Municipalities", MunicipalityManagementView.class),
            new Section("Assignment Types", AssignmentTypeManagementView.class),
            new Section("Resources", ResourceManagementView.class),
            new Section("Resource Types", ResourceTypeManagementView.class),
            new Section("Destinations", DestinationManagementView.class)
    );

    private final Tabs views;

    public ManagementLayout() {
        views = new Tabs();
        views.setOrientation(Tabs.Orientation.HORIZONTAL);
        views.addThemeVariants(TabsVariant.LUMO_MINIMAL);
        SECTIONS.forEach(section -> views.add(TabUtils.createNavigationTab(section.label(), section.view())));

        getStyle().set("background-color", "var(--lumo-shade-10pct)");

        setSizeFull();
        setPadding(false);
    }

    /**
     * Returns the views of the management console's sections, in the order of its tabs.
     */
    public static List<Class<? extends Component>> getSectionViews() {
        return SECTIONS.stream().<Class<? extends Component>>map(Section::view).toList();
    }

    @Override
    public Optional<Component> getNavbarContent() {
        return Optional.of(views);
    }

    @Override
    public String getTitle() {
        return "Management Console";
    }

    private record Section(String label, Class<? extends Component> view) {
    }
}
