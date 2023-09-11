package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.dom.ElementFactory;
import net.pkhapps.idispatch.server.boundary.UserManagementService;
import net.pkhapps.idispatch.server.entity.User;


public class UserManagementDialog extends AbstractManagementDialog<User> {

    public UserManagementDialog(UserManagementService service) {
        super(service);
    }

    @Override
    protected String getTitle(User entity) {
        return entity == null ? "Add User" : "Edit User";
    }

    @Override
    protected Form configureForm(User entity) {
        return new SingleEntityForm() {
            @Override
            protected void initForm(Binder<User> binder, FormLayout formLayout) {
                binder.setBean(entity == null ? new User() : entity);

                var username = new TextField("Username");
                formLayout.add(username);
                formLayout.getElement().appendChild(ElementFactory.createBr());

                formLayout.add(new Span("The default password is the username."));
                formLayout.getElement().appendChild(ElementFactory.createBr());

                var isAdmin = new Checkbox("Administrator");
                formLayout.add(isAdmin);

                var isDispatcher = new Checkbox("Dispatcher");
                formLayout.add(isDispatcher);

                var isReportReader = new Checkbox("Report Reader");
                formLayout.add(isReportReader);

                var isEnabled = new Checkbox("Enabled");
                formLayout.add(isEnabled);

                if (entity == null) {
                    binder.bind(username, User::getUsername, User::setUsername);
                    username.focus();
                } else {
                    binder.bindReadOnly(username, User::getUsername);
                }
                binder.bind(isAdmin, User::isAdmin, User::setAdmin);
                binder.bind(isDispatcher, User::isDispatcher, User::setDispatcher);
                binder.bind(isReportReader, User::isReportReader, User::setReportReader);
                binder.bind(isEnabled, User::isEnabled, User::setEnabled);
            }
        };
    }
}
