package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarTester;
import com.vaadin.flow.component.textfield.TextArea;
import net.pkhapps.idispatch.server.entity.Bilingual;
import net.pkhapps.idispatch.server.entity.Destination;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.ResourceState;
import net.pkhapps.idispatch.server.entity.RunboardDestination;
import net.pkhapps.idispatch.server.entity.SmsDestination;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Locale;
import java.util.function.Predicate;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

@WithMockUser(username = "destination-admin", roles = "ADMIN")
class DestinationManagementViewTest extends AbstractManagementViewTest {

    @Test
    void add_offersRunboardAndSmsDestinations() {
        navigate(DestinationManagementView.class);

        assertThat(test(MenuBarTester.class, addMenu()).getItemTexts("Add..."))
                .containsExactly("Runboard Destination", "SMS Destination");

        addDestination("SMS Destination");
        assertThat(editorTitle()).isEqualTo("Add SMS Destination");
        addDestination("Runboard Destination");
        assertThat(editorTitle()).isEqualTo("Add Runboard Destination");
    }

    @Test
    void add_createsRunboardDestinationForResources() {
        var key = unique("key-");
        var callSign = unique("CS");
        var description = unique("Station hall ");
        testData.resource(callSign, ResourceState.AVAILABLE);
        navigate(DestinationManagementView.class);

        addDestination("Runboard Destination");
        assertThat(languageField().getValue()).as("New destinations are in Finnish").isEqualTo(Bilingual.FINNISH);
        test(editorTextField("Description")).setValue(description);
        test(languageField()).selectItem("Swedish");
        test(editorTextField("Runboard Key"))
                .setValue(key);
        test(resourcesField()).selectItem(callSign);
        save();

        var row = rowOf(this.<Destination>grid(), Destination::getId,
                destinationId(d -> d instanceof RunboardDestination r && key.equals(r.getRunboardKey())));
        assertThat(test(this.<Destination>grid()).getCellText(row, 0)).isEqualTo("Runboard Destination");
        assertThat(test(this.<Destination>grid()).getCellText(row, 1)).isEqualTo(description);
        assertThat(test(this.<Destination>grid()).getCellText(row, 2)).isEqualTo("Swedish");
        assertThat(test(this.<Destination>grid()).getCellText(row, 3)).isEqualTo(callSign);
        assertThat(test(this.<Destination>grid()).getRow(row).getLocale()).isEqualTo(Bilingual.SWEDISH);
    }

    @Test
    void add_withoutDescription_storesNoDescription() {
        var callSign = unique("CS");
        testData.resource(callSign, ResourceState.AVAILABLE);
        navigate(DestinationManagementView.class);

        var key = addRunboardDestination(callSign);

        var id = destinationId(d -> d instanceof RunboardDestination r && key.equals(r.getRunboardKey()));
        var created = test(this.<Destination>grid()).getRow(rowOf(this.<Destination>grid(), Destination::getId, id));
        assertThat(created.getDescription()).isNull();
    }

    @Test
    void edit_showsAndUpdatesDescription_andSearchMatchesIt() {
        var callSign = unique("CS");
        testData.resource(callSign, ResourceState.AVAILABLE);
        var description = unique("Garage ");
        navigate(DestinationManagementView.class);
        var key = addRunboardDestination(callSign);

        var id = destinationId(d -> d instanceof RunboardDestination r && key.equals(r.getRunboardKey()));
        test(this.<Destination>grid()).select(rowOf(this.<Destination>grid(), Destination::getId, id));
        assertThat(editorTextField("Description").getValue()).isEmpty();
        test(editorTextField("Description")).setValue(description);
        save();

        search(description.toLowerCase());
        assertThat(items(this.<Destination>grid())).extracting(Destination::getDescription)
                .containsExactly(description);
    }

    @Test
    void add_createsSmsDestinationFromCommaAndLineSeparatedNumbers() {
        var callSign = unique("CS");
        testData.resource(callSign, ResourceState.AVAILABLE);
        var number = "+358" + Math.abs(callSign.hashCode());
        navigate(DestinationManagementView.class);

        addDestination("SMS Destination");
        test(find(TextArea.class, editor()).withLabel("Phone numbers").single())
                .setValue(number + "1, " + number + "2\n" + number + "3");
        test(resourcesField()).selectItem(callSign);
        save();

        var id = destinationId(d -> d instanceof SmsDestination s && s.getPhoneNumbers().contains(number + "1"));
        var created = (SmsDestination) test(this.<Destination>grid())
                .getRow(rowOf(this.<Destination>grid(), Destination::getId, id));
        assertThat(created.getPhoneNumbers()).containsExactlyInAnyOrder(number + "1", number + "2", number + "3");
        assertThat(created.getType()).isEqualTo("SMS Destination");
    }

    @Test
    void edit_smsDestination_showsSmsFormWithNumbers() {
        var callSign = unique("CS");
        testData.resource(callSign, ResourceState.AVAILABLE);
        var number = "+358" + Math.abs(callSign.hashCode());
        navigate(DestinationManagementView.class);
        addDestination("SMS Destination");
        test(find(TextArea.class, editor()).withLabel("Phone numbers").single()).setValue(number);
        test(resourcesField()).selectItem(callSign);
        save();

        var id = destinationId(d -> d instanceof SmsDestination s && s.getPhoneNumbers().contains(number));
        test(this.<Destination>grid()).select(rowOf(this.<Destination>grid(), Destination::getId, id));

        assertThat(editorTitle()).isEqualTo("Edit SMS Destination");
        assertThat(find(TextArea.class, editor()).withLabel("Phone numbers").single().getValue()).isEqualTo(number);
        assertThat(resourcesField().getValue()).extracting(Resource::getCallSign).containsExactly(callSign);
    }

    @Test
    void add_smsDestinationWithoutNumbersOrResources_showsValidationErrors() {
        navigate(DestinationManagementView.class);

        addDestination("SMS Destination");
        save();

        assertThat(resourcesField().getErrorMessage()).isEqualTo("Please select at least one resource");
        assertThat(isEditorOpen()).isTrue();
    }

    @Test
    void search_matchesResourceCallSign_andDeletedDestinationsAreHidden() {
        var marker = unique("");
        var callSign = "D" + marker;
        testData.resource(callSign, ResourceState.AVAILABLE);
        var other = unique("CS");
        testData.resource(other, ResourceState.AVAILABLE);
        navigate(DestinationManagementView.class);
        var matchKey = addRunboardDestination(callSign);
        addRunboardDestination(other);

        search(marker.toLowerCase());
        assertThat(items(this.<Destination>grid())).extracting(d -> ((RunboardDestination) d).getRunboardKey())
                .containsExactly(matchKey);

        test(this.<Destination>grid()).select(0);
        test(editorButton("Delete")).click();
        assertThat(items(this.<Destination>grid())).isEmpty();

        includeDeleted();
        assertThat(items(this.<Destination>grid())).extracting(Destination::isActive).containsExactly(false);
    }

    private String addRunboardDestination(String callSign) {
        var key = unique("key-");
        addDestination("Runboard Destination");
        test(editorTextField("Runboard Key")).setValue(key);
        test(resourcesField()).selectItem(callSign);
        save();
        return key;
    }

    private MenuBar addMenu() {
        return find(MenuBar.class, currentView()).all().stream()
                .filter(menuBar -> !menuBar.hasClassName("view-options"))
                .findFirst()
                .orElseThrow();
    }

    private void addDestination(String type) {
        test(MenuBarTester.class, addMenu()).clickItem("Add...", type);
    }

    @SuppressWarnings("unchecked")
    private ComboBox<Locale> languageField() {
        return find(ComboBox.class, editor()).withLabel("Language").single();
    }

    @SuppressWarnings("unchecked")
    private MultiSelectComboBox<Resource> resourcesField() {
        return find(MultiSelectComboBox.class, editor()).withLabel("Resources").single();
    }

    private Long destinationId(Predicate<Destination> predicate) {
        return items(this.<Destination>grid()).stream().filter(predicate).findFirst()
                .map(Destination::getId)
                .orElseThrow(() -> new AssertionError("No matching destination"));
    }
}
