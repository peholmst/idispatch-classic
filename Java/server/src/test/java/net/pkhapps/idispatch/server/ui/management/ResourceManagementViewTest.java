package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.combobox.ComboBox;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.ResourceState;
import net.pkhapps.idispatch.server.entity.ResourceType;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

@WithMockUser(username = "resource-admin", roles = "ADMIN")
class ResourceManagementViewTest extends AbstractManagementViewTest {

    @Test
    void add_createsResourceThatStartsOutOfService() {
        var typeCode = unique("RT");
        testData.resourceType(typeCode, "Ambulance");
        var callSign = unique("CS");
        navigate(ResourceManagementView.class);

        add();
        assertThat(editorTitle()).isEqualTo("Add Resource");
        test(editorTextField("Call sign")).setValue(callSign);
        test(typeField()).selectItem(typeCode + " - Ambulance");
        save();

        var row = rowOf(this.<Resource>grid(), Resource::getCallSign, callSign);
        assertThat(test(this.<Resource>grid()).getCellText(row, 1)).isEqualTo("Ambulance");
        var created = test(this.<Resource>grid()).getRow(row);
        assertThat(testData.statusOf(created).getState()).isEqualTo(ResourceState.OUT_OF_SERVICE);
    }

    @Test
    void typeField_offersActiveResourceTypesOnly() {
        var active = unique("RT");
        var inactive = unique("RT");
        testData.resourceType(active, "Active type");
        testData.deletedResourceType(inactive, "Inactive type");

        navigate(ResourceManagementView.class);
        add();

        assertThat(test(typeField()).getSuggestions())
                .contains(active + " - Active type")
                .doesNotContain(inactive + " - Inactive type");
    }

    @Test
    void edit_changesCallSign() {
        var original = unique("CS");
        var renamed = unique("CS");
        testData.resource(original, ResourceState.AVAILABLE);
        navigate(ResourceManagementView.class);

        test(this.<Resource>grid()).select(rowOf(this.<Resource>grid(), Resource::getCallSign, original));
        assertThat(editorTitle()).isEqualTo("Edit Resource");
        test(editorTextField("Call sign")).setValue(renamed);
        save();

        assertThat(containsRow(this.<Resource>grid(), Resource::getCallSign, renamed)).isTrue();
        assertThat(containsRow(this.<Resource>grid(), Resource::getCallSign, original)).isFalse();
    }

    @Test
    void search_matchesAnyPartOfCallSign() {
        var marker = unique("");
        var match = "X" + marker + "Y";
        var other = unique("CS");
        testData.resource(match, ResourceState.AVAILABLE);
        testData.resource(other, ResourceState.AVAILABLE);
        navigate(ResourceManagementView.class);

        search(marker.toLowerCase());

        assertThat(items(this.<Resource>grid()).stream().map(Resource::getCallSign)).containsExactly(match);
    }

    @Test
    void delete_hidesResource() {
        var callSign = unique("CS");
        testData.resource(callSign, ResourceState.AVAILABLE);
        navigate(ResourceManagementView.class);

        test(this.<Resource>grid()).select(rowOf(this.<Resource>grid(), Resource::getCallSign, callSign));
        test(editorButton("Delete")).click();

        assertThat(containsRow(this.<Resource>grid(), Resource::getCallSign, callSign)).isFalse();
    }

    @SuppressWarnings("unchecked")
    private ComboBox<ResourceType> typeField() {
        return find(ComboBox.class, editor()).withLabel("Type").single();
    }
}
