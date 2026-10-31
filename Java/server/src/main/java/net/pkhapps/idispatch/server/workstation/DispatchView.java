package net.pkhapps.idispatch.server.workstation;


import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.splitlayout.SplitLayoutVariant;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import com.vaadin.flow.signals.local.ValueSignal;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.assignment.AssignmentService;
import net.pkhapps.idispatch.server.dispatch.DispatchService;
import net.pkhapps.idispatch.server.resource.ResourceQueries;
import net.pkhapps.idispatch.server.resource.ResourceStatusService;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.RootLayout;
import net.pkhapps.idispatch.server.ui.ViewTitle;
import org.jspecify.annotations.Nullable;
import org.springframework.context.event.ApplicationEventMulticaster;

@Route(value = "dws", layout = RootLayout.class)
@RolesAllowed(Roles.ROLE_DISPATCHER)
public class DispatchView extends VerticalLayout implements HasDynamicTitle {

    private final AssignmentService assignmentService;
    // The assignment open in the assignment panel and selected in the list, or null for none
    private final ValueSignal<@Nullable AssignmentId> selectedAssignment = new ValueSignal<>(null);

    public DispatchView(AssignmentService assignmentService,
                        ResourceStatusService resourceStatusService,
                        ResourceQueries resourceQueries,
                        DispatchService dispatchService,
                        ApplicationEventMulticaster eventMulticaster) {
        this.assignmentService = assignmentService;
        var assignmentPanel = new AssignmentPanel(assignmentService, resourceStatusService, resourceQueries,
                dispatchService, eventMulticaster, selectedAssignment);
        var assignmentListPanel = new AssignmentListPanel(assignmentService, eventMulticaster, selectedAssignment);
        var resourceListPanel = new ResourceListPanel(resourceStatusService, assignmentService, resourceQueries,
                eventMulticaster);
        setSizeFull();
        setMargin(false);
        setSpacing(false);
        setPadding(false);

        var newAssignment = new Button(getTranslation("dispatch.new-assignment"), event -> newAssignment());
        newAssignment.addThemeVariants(ButtonVariant.PRIMARY);

        var toolbar = new HorizontalLayout(new ViewTitle(getTranslation("nav.dispatcher")), newAssignment);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        toolbar.setWidthFull();
        toolbar.setPadding(true);
        toolbar.addClassName("toolbar");
        add(toolbar);

        var primarySplit = new SplitLayout(SplitLayout.Orientation.HORIZONTAL);
        primarySplit.setSizeFull();
        primarySplit.addThemeVariants(SplitLayoutVariant.SMALL);
        add(primarySplit);


        primarySplit.setSplitterPosition(33);

        primarySplit.addToPrimary(assignmentPanel);
        var secondarySplit = new SplitLayout(SplitLayout.Orientation.VERTICAL);
        secondarySplit.setSizeFull();
        secondarySplit.addThemeVariants(SplitLayoutVariant.SMALL);
        secondarySplit.addToPrimary(assignmentListPanel);
        secondarySplit.addToSecondary(resourceListPanel);
        primarySplit.addToSecondary(secondarySplit);
    }

    private void newAssignment() {
        // Opening the assignment refreshes the list right away, so the new assignment is there to be selected
        selectedAssignment.set(assignmentService.openAssignment());
    }

    @Override
    public String getPageTitle() {
        return "iDispatch | " + getTranslation("nav.dispatcher");
    }
}
