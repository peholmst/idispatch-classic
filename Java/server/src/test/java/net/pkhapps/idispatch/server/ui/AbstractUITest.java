package net.pkhapps.idispatch.server.ui;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import net.pkhapps.idispatch.server.TestcontainersConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Base class for browserless UI tests. Runs against the full application context and a PostgreSQL container.
 */
@SpringBootTest
@Import({TestData.class, TestcontainersConfiguration.class})
public abstract class AbstractUITest extends SpringBrowserlessTest {

    @Autowired
    protected TestData testData;

    protected Button button(String text) {
        return find(Button.class).withText(text).single();
    }

    protected Button buttonInDialog(String text) {
        return find(Button.class, openDialog()).withText(text).single();
    }

    protected Dialog openDialog() {
        return find(Dialog.class).single();
    }

    protected Optional<Dialog> findOpenDialog() {
        return find(Dialog.class).all().stream().findFirst();
    }

    protected List<String> notificationTexts() {
        return find(Notification.class).all().stream()
                .map(n -> Optional.ofNullable(test(n).getText()).orElseGet(() -> n.getElement().getTextRecursively()))
                .toList();
    }

    protected <T> List<String> columnTexts(Grid<T> grid, int column) {
        var tester = test(grid);
        return IntStream.range(0, tester.size()).mapToObj(row -> tester.getCellText(row, column)).toList();
    }

    protected <T> List<T> items(Grid<T> grid) {
        var tester = test(grid);
        var items = new ArrayList<T>();
        IntStream.range(0, tester.size()).forEach(row -> items.add(tester.getRow(row)));
        return items;
    }

    protected <T> int rowOf(Grid<T> grid, Function<T, ?> property, Object value) {
        var tester = test(grid);
        return IntStream.range(0, tester.size())
                .filter(row -> value.equals(property.apply(tester.getRow(row))))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No row with value " + value));
    }

    protected <T> boolean containsRow(Grid<T> grid, Function<T, ?> property, Object value) {
        return items(grid).stream().anyMatch(item -> value.equals(property.apply(item)));
    }

    protected Component currentView() {
        return (Component) getCurrentView();
    }

    @SuppressWarnings("unchecked")
    protected <T> Grid<T> grid(Component container) {
        return find(Grid.class, container).single();
    }
}
