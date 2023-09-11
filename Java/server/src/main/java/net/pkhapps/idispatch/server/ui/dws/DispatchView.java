package net.pkhapps.idispatch.server.ui.dws;


import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.splitlayout.SplitLayoutVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.boundary.AssignmentService;
import net.pkhapps.idispatch.server.boundary.DispatchService;
import net.pkhapps.idispatch.server.boundary.ResourceStatusService;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.HasNavbarContent;
import net.pkhapps.idispatch.server.ui.RootLayout;
import org.springframework.context.event.ApplicationEventMulticaster;

@Route(value = "dws", layout = RootLayout.class)
@RolesAllowed(Roles.ROLE_DISPATCHER)
@PageTitle("iDispatch | Dispatcher")
@JsModule("@vaadin/vaadin-lumo-styles/presets/compact.js")
@CssImport("./styles/dispatch-view-styles.css")
@CssImport(value = "./styles/dispatch-view-grid-styles.css", themeFor = "vaadin-grid")
public class DispatchView extends VerticalLayout implements HasNavbarContent {

    private final AssignmentService assignmentService;
    private final AssignmentPanel assignmentPanel;
    private final AssignmentListPanel assignmentListPanel;
    private final ResourceListPanel resourceListPanel;
    private final HorizontalLayout toolbar;

    public DispatchView(AssignmentService assignmentService,
                        ResourceStatusService resourceStatusService,
                        DispatchService dispatchService,
                        ApplicationEventMulticaster eventMulticaster) {
        this.assignmentService = assignmentService;
        assignmentPanel = new AssignmentPanel(assignmentService, resourceStatusService, dispatchService, eventMulticaster);
        assignmentListPanel = new AssignmentListPanel(assignmentService, eventMulticaster, assignmentPanel::setAssignmentId);
        resourceListPanel = new ResourceListPanel(resourceStatusService, eventMulticaster);
        getStyle().set("background-color", "var(--lumo-shade-10pct)");
        setSizeFull();
        setMargin(false);
        setSpacing(false);
        setPadding(false);

        toolbar = new HorizontalLayout();
        toolbar.getStyle().set("margin-left", "var(--lumo-space-m)");
        toolbar.setSizeUndefined();

        var newAssignment = new Button("New Assignment", event -> newAssignment());
        newAssignment.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        toolbar.add(newAssignment);

        var primarySplit = new SplitLayout(SplitLayout.Orientation.HORIZONTAL);
        primarySplit.setSizeFull();
        primarySplit.addThemeVariants(SplitLayoutVariant.LUMO_SMALL);
        add(primarySplit);


        primarySplit.setSplitterPosition(33);

        primarySplit.addToPrimary(assignmentPanel);
        var secondarySplit = new SplitLayout(SplitLayout.Orientation.VERTICAL);
        secondarySplit.setSizeFull();
        secondarySplit.addThemeVariants(SplitLayoutVariant.LUMO_SMALL);
        secondarySplit.addToPrimary(assignmentListPanel);
        secondarySplit.addToSecondary(resourceListPanel);
        primarySplit.addToSecondary(secondarySplit);
    }

    private void newAssignment() {
        var newId = assignmentService.openAssignment();
        assignmentListPanel.selectAssignmentById(newId);
    }

    @Override
    public Component getNavbarContent() {
        return toolbar;
    }

    @Override
    public String getTitle() {
        return "Dispatcher";
    }
}
