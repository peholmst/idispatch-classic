package net.pkhapps.idispatch.server.ui.management;

import net.pkhapps.idispatch.server.entity.AssignmentType;
import net.pkhapps.idispatch.server.entity.ResourceType;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the assignment type and resource type management views, which share the same code/description form.
 */
@WithMockUser(username = "types-admin", roles = "ADMIN")
class TypeManagementViewsTest extends AbstractManagementViewTest {

    @Test
    void assignmentTypes_add() {
        var code = unique("AT");
        navigate(AssignmentTypeManagementView.class);

        add();
        assertThat(editorTitle()).isEqualTo("Add Assignment Type");
        test(editorTextField("Code")).setValue(code);
        test(editorTextField("Description in Finnish")).setValue("Structure fire");
        save();

        var row = rowOf(this.<AssignmentType>grid(), AssignmentType::getCode, code);
        assertThat(test(this.<AssignmentType>grid()).getCellText(row, 1)).isEqualTo("Structure fire");
    }

    @Test
    void assignmentTypes_addWithoutCodeAndDescription_showsBothValidationErrors() {
        navigate(AssignmentTypeManagementView.class);

        add();
        save();

        assertThat(editorTextField("Code").getErrorMessage()).isEqualTo("Please enter a code");
        assertThat(editorTextField("Description in Finnish").getErrorMessage()).isEqualTo("Please enter a description in Finnish or Swedish");
    }

    @Test
    void assignmentTypes_editAndDelete() {
        var code = unique("AT");
        testData.assignmentType(code, "Original");
        navigate(AssignmentTypeManagementView.class);

        test(this.<AssignmentType>grid()).select(rowOf(this.<AssignmentType>grid(), AssignmentType::getCode, code));
        assertThat(editorTitle()).isEqualTo("Edit Assignment Type");
        test(editorTextField("Description in Finnish")).setValue("Changed");
        save();

        var row = rowOf(this.<AssignmentType>grid(), AssignmentType::getCode, code);
        assertThat(test(this.<AssignmentType>grid()).getCellText(row, 1)).isEqualTo("Changed");

        test(this.<AssignmentType>grid()).select(row);
        test(editorButton("Delete")).click();
        assertThat(containsRow(this.<AssignmentType>grid(), AssignmentType::getCode, code)).isFalse();

        includeDeleted();
        var deleted = test(this.<AssignmentType>grid()).getRow(rowOf(this.<AssignmentType>grid(), AssignmentType::getCode, code));
        assertThat(deleted.isActive()).isFalse();
        assertThat(this.<AssignmentType>grid().getPartNameGenerator().apply(deleted)).isEqualTo("deleted");
    }

    @Test
    void assignmentTypes_searchMatchesCodePrefixOrDescription() {
        var code = unique("SRCH");
        var marker = unique("Desc");
        testData.assignmentType(code, "Other text");
        testData.assignmentType(unique("AT"), "Has " + marker + " inside");
        testData.assignmentType(unique("AT"), "Unrelated");
        navigate(AssignmentTypeManagementView.class);

        search(code.substring(0, code.length() - 2).toLowerCase());
        assertThat(items(this.<AssignmentType>grid())).extracting(AssignmentType::getCode).containsExactly(code);

        search(marker.toLowerCase());
        assertThat(items(this.<AssignmentType>grid())).extracting(AssignmentType::getDescriptionFi)
                .containsExactly("Has " + marker + " inside");
    }

    @Test
    void assignmentTypes_areSortedByCode() {
        var prefix = unique("S");
        testData.assignmentType(prefix + "B", "Second");
        testData.assignmentType(prefix + "A", "First");
        navigate(AssignmentTypeManagementView.class);

        var codes = items(this.<AssignmentType>grid()).stream().map(AssignmentType::getCode)
                .filter(c -> c.startsWith(prefix)).toList();
        assertThat(codes).containsExactly(prefix + "A", prefix + "B");
    }

    @Test
    void resourceTypes_add() {
        var code = unique("RT");
        navigate(ResourceTypeManagementView.class);

        add();
        assertThat(editorTitle()).isEqualTo("Add Resource Type");
        test(editorTextField("Code")).setValue(code);
        test(editorTextField("Description in Finnish")).setValue("Rescue unit");
        save();

        var row = rowOf(this.<ResourceType>grid(), ResourceType::getCode, code);
        assertThat(test(this.<ResourceType>grid()).getCellText(row, 1)).isEqualTo("Rescue unit");
    }

    @Test
    void resourceTypes_editAndRestore() {
        var code = unique("RT");
        testData.resourceType(code, "Original");
        navigate(ResourceTypeManagementView.class);

        test(this.<ResourceType>grid()).select(rowOf(this.<ResourceType>grid(), ResourceType::getCode, code));
        assertThat(editorTitle()).isEqualTo("Edit Resource Type");
        test(editorTextField("Description in Finnish")).setValue("Changed");
        save();

        test(this.<ResourceType>grid()).select(rowOf(this.<ResourceType>grid(), ResourceType::getCode, code));
        test(editorButton("Delete")).click();
        assertThat(containsRow(this.<ResourceType>grid(), ResourceType::getCode, code)).isFalse();

        includeDeleted();
        test(this.<ResourceType>grid()).select(rowOf(this.<ResourceType>grid(), ResourceType::getCode, code));
        test(editorButton("Restore")).click();

        var restored = test(this.<ResourceType>grid()).getRow(rowOf(this.<ResourceType>grid(), ResourceType::getCode, code));
        assertThat(restored.isActive()).isTrue();
        assertThat(restored.getDescriptionFi()).isEqualTo("Changed");
    }

    @Test
    void resourceTypes_hideDeletedTypesUnlessIncluded() {
        var code = unique("RT");
        testData.deletedResourceType(code, "Gone");
        navigate(ResourceTypeManagementView.class);

        assertThat(containsRow(this.<ResourceType>grid(), ResourceType::getCode, code)).isFalse();
        includeDeleted();
        assertThat(containsRow(this.<ResourceType>grid(), ResourceType::getCode, code)).isTrue();
    }
}
