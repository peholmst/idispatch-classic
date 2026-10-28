package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Span;
import net.pkhapps.idispatch.server.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WithMockUser(username = UserManagementViewTest.ADMIN, roles = "ADMIN")
class UserManagementViewTest extends AbstractManagementViewTest {

    static final String ADMIN = "user-mgmt-admin";

    @Test
    void users_cannotBeDeletedOrRestored() {
        navigate(UserManagementView.class);

        assertThat(findDeleteButton()).isEmpty();
        assertThat(findRestoreButton()).isEmpty();
    }

    @Test
    void add_createsUserWithRolesAndUsernameAsPassword() {
        var username = unique("user-").toLowerCase();
        navigate(UserManagementView.class);

        test(button("Add...")).click();
        assertThat(openDialog().getHeaderTitle()).isEqualTo("Add User");
        assertThat(find(Span.class, openDialog()).withText("The default password is the username.").exists()).isTrue();
        test(dialogTextField("Username")).setValue(username);
        test(checkbox("Dispatcher")).check();
        test(checkbox("Enabled")).check();
        test(buttonInDialog("Save")).click();

        var row = rowOf(this.<User>grid(), User::getUsername, username);
        assertThat(test(this.<User>grid()).getCellText(row, 1)).isEqualTo("No");
        assertThat(test(this.<User>grid()).getCellText(row, 2)).isEqualTo("Yes");
        assertThat(test(this.<User>grid()).getCellText(row, 3)).isEqualTo("No");
        assertThat(test(this.<User>grid()).getCellText(row, 4)).isEqualTo("Yes");
        assertThat(testData.passwordMatches(username, username)).isTrue();
    }

    @Test
    void edit_changesRolesButNotUsername() {
        var username = unique("user-").toLowerCase();
        testData.user(username, false, false, false);
        navigate(UserManagementView.class);

        test(this.<User>grid()).select(rowOf(this.<User>grid(), User::getUsername, username));
        test(editButton()).click();
        assertThat(openDialog().getHeaderTitle()).isEqualTo("Edit User");
        assertThat(dialogTextField("Username").isReadOnly()).isTrue();
        assertThat(dialogTextField("Username").getValue()).isEqualTo(username);
        test(checkbox("Report Reader")).check();
        test(buttonInDialog("Save")).click();

        assertThat(testData.findUser(username).isReportReader()).isTrue();
        assertThat(testData.passwordMatches(username, username)).isTrue();
    }

    @Test
    void edit_ownAccount_isDenied() {
        testData.user(ADMIN, true, false, false);
        navigate(UserManagementView.class);

        test(this.<User>grid()).select(rowOf(this.<User>grid(), User::getUsername, ADMIN));
        test(editButton()).click();
        test(checkbox("Dispatcher")).check();

        assertThatThrownBy(() -> test(buttonInDialog("Save")).click())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You cannot make changes to your own account");
        assertThat(testData.findUser(ADMIN).isDispatcher()).isFalse();
    }

    @Test
    void search_filtersByUsername() {
        var marker = unique("find-").toLowerCase();
        var match = "x" + marker + "y";
        testData.user(match, false, false, false);
        testData.user(unique("user-").toLowerCase(), false, false, false);
        navigate(UserManagementView.class);

        search(marker);

        assertThat(items(this.<User>grid())).extracting(User::getUsername).containsExactly(match);
    }

    private Checkbox checkbox(String label) {
        return find(Checkbox.class, openDialog()).withLabel(label).single();
    }
}
