package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.textfield.PasswordField;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChangePasswordViewTest extends AbstractUITest {

    @Test
    @WithMockUser(username = "pw-success")
    void changePassword_withValidInput_changesPasswordAndClearsForm() {
        testData.user("pw-success", false, false, false);
        testData.setPassword("pw-success", "old-secret");
        navigate(ChangePasswordView.class);

        fillIn("old-secret", "new-secret", "new-secret");
        test(button("Change Password")).click();

        assertThat(testData.passwordMatches("pw-success", "new-secret")).isTrue();
        assertThat(notificationTexts()).contains("Password changed!");
        assertThat(field("Current Password").getValue()).isEmpty();
        assertThat(field("New Password").getValue()).isEmpty();
        assertThat(field("Confirm Password").getValue()).isEmpty();
    }

    @Test
    @WithMockUser(username = "pw-mismatch")
    void changePassword_withMismatchingConfirmation_showsErrorAndKeepsPassword() {
        testData.user("pw-mismatch", false, false, false);
        testData.setPassword("pw-mismatch", "old-secret");
        navigate(ChangePasswordView.class);

        fillIn("old-secret", "new-secret", "something-else");
        test(button("Change Password")).click();

        assertThat(field("Confirm Password").isInvalid()).isTrue();
        assertThat(field("Confirm Password").getErrorMessage()).isEqualTo("New passwords do not match");
        assertThat(testData.passwordMatches("pw-mismatch", "old-secret")).isTrue();
        assertThat(notificationTexts()).doesNotContain("Password changed!");
    }

    @Test
    @WithMockUser(username = "pw-empty")
    void changePassword_withEmptyFields_marksAllFieldsAsRequired() {
        testData.user("pw-empty", false, false, false);
        navigate(ChangePasswordView.class);

        test(button("Change Password")).click();

        assertThat(field("Current Password").isInvalid()).isTrue();
        assertThat(field("New Password").isInvalid()).isTrue();
        assertThat(field("Confirm Password").isInvalid()).isTrue();
        assertThat(notificationTexts()).doesNotContain("Password changed!");
    }

    @Test
    @WithMockUser(username = "pw-wrong-current")
    void changePassword_withWrongCurrentPassword_isDeniedAndKeepsPassword() {
        testData.user("pw-wrong-current", false, false, false);
        testData.setPassword("pw-wrong-current", "old-secret");
        navigate(ChangePasswordView.class);

        fillIn("not-the-password", "new-secret", "new-secret");

        // In the running application, the session error handler turns this into an "Access has been denied"
        // notification (see CustomErrorHandlerTest).
        assertThatThrownBy(() -> test(button("Change Password")).click())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Current password is incorrect");
        assertThat(testData.passwordMatches("pw-wrong-current", "old-secret")).isTrue();
        assertThat(notificationTexts()).doesNotContain("Password changed!");
    }

    @Test
    @WithMockUser(username = "pw-title")
    void changePasswordView_showsTitleInNavbar() {
        testData.user("pw-title", false, false, false);
        navigate(ChangePasswordView.class);

        assertThat(find(H2.class).single().getText()).isEqualTo("Change Password");
    }

    private void fillIn(String current, String newPassword, String confirm) {
        test(field("Current Password")).setValue(current);
        test(field("New Password")).setValue(newPassword);
        test(field("Confirm Password")).setValue(confirm);
    }

    private PasswordField field(String label) {
        return find(PasswordField.class).withLabel(label).single();
    }
}
