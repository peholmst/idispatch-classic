package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import net.pkhapps.idispatch.server.boundary.AssignmentTypeManagementService;
import net.pkhapps.idispatch.server.entity.AssignmentType;

public class AssignmentTypeManagementDialog extends AbstractManagementDialog<AssignmentType> {

    public AssignmentTypeManagementDialog(AssignmentTypeManagementService service) {
        super(service);
    }

    @Override
    protected String getTitle(AssignmentType entity) {
        return entity.isNew() ? "Add Assignment Type" : "Edit Assignment Type";
    }

    @Override
    protected void configureForm(FormLayout form) {
        var code = new TextField("Code");
        form.add(code);

        var description = new TextField("Description");
        form.add(description);

        getBinder().bind(code, AssignmentType::getCode, AssignmentType::setCode);
        getBinder().bind(description, AssignmentType::getDescription, AssignmentType::setDescription);

        code.focus();
    }
}
