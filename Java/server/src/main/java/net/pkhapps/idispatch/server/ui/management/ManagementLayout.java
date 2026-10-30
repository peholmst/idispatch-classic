package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RouterLayout;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.RootLayout;
import net.pkhapps.idispatch.server.ui.TabUtils;
import net.pkhapps.idispatch.server.ui.ViewTitle;

import java.util.List;

@ParentLayout(RootLayout.class)
@RolesAllowed(Roles.ROLE_ADMIN)
public class ManagementLayout extends VerticalLayout implements RouterLayout {

    private static final List<Section> SECTIONS = List.of(
            new Section("management.municipalities", MunicipalityManagementView.class),
            new Section("management.assignment-types", AssignmentTypeManagementView.class),
            new Section("management.resources", ResourceManagementView.class),
            new Section("management.resource-types", ResourceTypeManagementView.class),
            new Section("management.destinations", DestinationManagementView.class)
    );

    public ManagementLayout() {
        var views = new Tabs();
        views.setOrientation(Tabs.Orientation.HORIZONTAL);
        SECTIONS.forEach(section -> views.add(TabUtils.createNavigationTab(getTranslation(section.labelKey()), section.view())));

        // The section views are added below the toolbar
        var toolbar = new HorizontalLayout(new ViewTitle(getTranslation("management.title")), views);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        toolbar.setWidthFull();
        toolbar.setPadding(true);
        toolbar.addClassName("toolbar");
        add(toolbar);

        setSizeFull();
        setPadding(false);
    }

    /**
     * Returns the views of the management console's sections, in the order of its tabs.
     */
    public static List<Class<? extends Component>> getSectionViews() {
        return SECTIONS.stream().<Class<? extends Component>>map(Section::view).toList();
    }

    /**
     * Returns the translation key of the label of the section that the given view shows.
     */
    static String getSectionLabelKey(Class<? extends Component> view) {
        return SECTIONS.stream()
                .filter(section -> section.view().equals(view))
                .map(Section::labelKey)
                .findFirst()
                .orElseThrow();
    }

    private record Section(String labelKey, Class<? extends Component> view) {
    }
}
