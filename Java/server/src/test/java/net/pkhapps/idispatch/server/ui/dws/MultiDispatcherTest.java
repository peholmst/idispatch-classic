package net.pkhapps.idispatch.server.ui.dws;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.SecuredBrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import net.pkhapps.idispatch.server.TestcontainersConfiguration;
import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.ResourceState;
import net.pkhapps.idispatch.server.entity.ResourceStatus;
import net.pkhapps.idispatch.server.events.ResourceStatusChanged;
import net.pkhapps.idispatch.server.ui.RootView;
import net.pkhapps.idispatch.server.ui.TestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.stream.IntStream;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two dispatchers working at the same time, each in their own session. Changes made by one dispatcher are pushed
 * to the other dispatcher's open view through application events.
 */
@SpringBootTest
@Import({TestData.class, TestcontainersConfiguration.class})
class MultiDispatcherTest {

    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    TestData testData;

    private SecuredBrowserlessApplicationContext<Authentication> app;
    private BrowserlessUIContext alice;
    private BrowserlessUIContext bob;

    @BeforeEach
    void setUp() {
        app = SpringBrowserlessApplicationContext.createSecured(applicationContext, RootView.class);
        alice = app.newUser("alice", "DISPATCHER").newWindow();
        bob = app.newUser("bob", "DISPATCHER").newWindow();
        alice.navigate(DispatchView.class);
        bob.navigate(DispatchView.class);
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void assignmentOpenedByOneDispatcher_appearsForTheOther() {
        var before = assignmentIds(bob);

        alice.test(button(alice, "New Assignment")).click();
        bob.roundTrip();

        var opened = assignmentIds(alice).stream().filter(id -> !before.contains(id)).toList();
        assertThat(opened).hasSize(1);
        assertThat(assignmentIds(bob)).containsAll(opened);
    }

    @Test
    void resourceReservedByOneDispatcher_showsAsReservedForTheOther() {
        var resource = testData.resource(unique("CS"), ResourceState.AVAILABLE);
        // Re-open Bob's view so that its resource list contains the new resource
        bob.navigate(DispatchView.class);

        alice.test(button(alice, "New Assignment")).click();
        reserve(alice, resource);
        bob.roundTrip();

        assertThat(resourceState(bob, resource)).isEqualTo("Reserved");
    }

    @Test
    void eventCausedByAnotherUser_refreshesViewAsTheViewOwner() {
        var resource = testData.resource(unique("CS"), ResourceState.AT_STATION);
        var admin = app.newUser("carol", "ADMIN").newWindow();

        // The event is published while an administrator, who may not use the dispatcher services, is active
        admin.activate();
        applicationContext.publishEvent(new ResourceStatusChanged(this, testData.statusOf(resource)));
        bob.roundTrip();

        assertThat(resourceState(bob, resource)).isEqualTo("At station");
    }

    private void reserve(BrowserlessUIContext window, Resource resource) {
        var panel = window.find(AssignmentPanel.class).single();
        @SuppressWarnings("unchecked")
        ComboBox<Resource> available = window.find(ComboBox.class, panel).all().stream()
                .filter(c -> c.getLabel() == null).findFirst().orElseThrow();
        window.test(available).selectItem(resource.getCallSign());
        window.test(window.find(Button.class, panel).all().stream()
                .filter(b -> b.getIcon() != null
                        && "vaadin:plus-circle".equals(b.getIcon().getElement().getAttribute("icon")))
                .findFirst().orElseThrow()).click();
    }

    private Button button(BrowserlessUIContext window, String text) {
        return window.find(Button.class).withText(text).single();
    }

    @SuppressWarnings("unchecked")
    private List<Long> assignmentIds(BrowserlessUIContext window) {
        Grid<Assignment> grid = window.find(Grid.class, window.find(AssignmentListPanel.class).single()).single();
        var tester = window.test(grid);
        return IntStream.range(0, tester.size()).mapToObj(row -> tester.getRow(row).getId()).toList();
    }

    @SuppressWarnings("unchecked")
    private String resourceState(BrowserlessUIContext window, Resource resource) {
        Grid<ResourceStatus> grid = window.find(Grid.class, window.find(ResourceListPanel.class).single()).single();
        var tester = window.test(grid);
        var row = IntStream.range(0, tester.size())
                .filter(r -> tester.getRow(r).getResource().getCallSign().equals(resource.getCallSign()))
                .findFirst().orElseThrow();
        return ((Button) tester.getCellComponent(row, 1)).getText();
    }
}
