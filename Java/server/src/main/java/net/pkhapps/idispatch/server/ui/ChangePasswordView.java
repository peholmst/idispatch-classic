package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import net.pkhapps.idispatch.server.boundary.ChangePasswordService;

import java.util.Objects;

@Route(value = "changePassword", layout = RootLayout.class)
@PageTitle("iDispatch | Change Password")
@PermitAll
public class ChangePasswordView extends VerticalLayout implements HasNavbarContent {

    private final PasswordField currentPassword;
    private final PasswordField newPassword;
    private final PasswordField confirmPassword;
    private final Button changeBtn;
    private final Binder<FormModel> binder = new Binder<>();

    private final ChangePasswordService changePasswordService;

    public ChangePasswordView(ChangePasswordService changePasswordService) {
        this.changePasswordService = changePasswordService;

        currentPassword = new PasswordField("Current Password");
        currentPassword.setValueChangeMode(ValueChangeMode.TIMEOUT);
        currentPassword.setWidth("300px");

        newPassword = new PasswordField("New Password");
        newPassword.setValueChangeMode(ValueChangeMode.TIMEOUT);
        newPassword.setWidth("300px");

        confirmPassword = new PasswordField("Confirm Password");
        confirmPassword.setValueChangeMode(ValueChangeMode.TIMEOUT);
        confirmPassword.setWidth("300px");

        changeBtn = new Button("Change Password", event -> changePassword());
        changeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(currentPassword, newPassword, confirmPassword, changeBtn);

        binder.forField(currentPassword)
                .asRequired()
                .bind(FormModel::getCurrentPassword, FormModel::setCurrentPassword);
        binder.forField(newPassword)
                .asRequired()
                .bind(FormModel::getNewPassword, FormModel::setNewPassword);
        binder.forField(confirmPassword)
                .asRequired()
                .withValidator((value, context) -> Objects.equals(value, newPassword.getValue()) ? ValidationResult.ok() : ValidationResult.error("New passwords do not match"))
                .bind(FormModel::getConfirmPassword, FormModel::setConfirmPassword);
        binder.setBean(new FormModel());
    }

    private void changePassword() {
        if (binder.validate().isOk()) {
            changePasswordService.changePassword(binder.getBean().getCurrentPassword(),
                    binder.getBean().getNewPassword());
            binder.setBean(new FormModel());

            var notification = new Notification("Password changed!");
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            notification.setPosition(Notification.Position.MIDDLE);
            notification.setDuration(3000);
            notification.open();
        }
    }

    private static class FormModel {
        private String currentPassword = "";
        private String newPassword = "";
        private String confirmPassword = "";

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }

        public String getConfirmPassword() {
            return confirmPassword;
        }

        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }
    }

    @Override
    public String getTitle() {
        return "Change Password";
    }
}
