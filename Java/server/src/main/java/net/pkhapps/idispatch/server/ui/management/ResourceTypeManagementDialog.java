package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import net.pkhapps.idispatch.server.boundary.ResourceTypeManagementService;
import net.pkhapps.idispatch.server.entity.ResourceType;

public class ResourceTypeManagementDialog extends AbstractManagementDialog<ResourceType> {

    protected ResourceTypeManagementDialog(ResourceTypeManagementService service) {
        super(service);
    }

    @Override
    protected String getTitle(ResourceType entity) {
        return entity.isNew() ? "Add ResourceType" : "Edit Resource Type";
    }

    @Override
    protected void configureForm(FormLayout form) {
        var code = new TextField("Code");
        form.add(code);

        var description = new TextField("Description");
        form.add(description);

        getBinder().bind(code, ResourceType::getCode, ResourceType::setCode);
        getBinder().bind(description, ResourceType::getDescription, ResourceType::setDescription);

        code.focus();
    }
}
