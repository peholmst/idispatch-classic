package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import net.pkhapps.idispatch.server.boundary.ResourceTypeManagementService;
import net.pkhapps.idispatch.server.entity.ResourceType;
import org.springframework.lang.Nullable;

public class ResourceTypeManagementDialog extends AbstractManagementDialog<ResourceType> {

    protected ResourceTypeManagementDialog(ResourceTypeManagementService service) {
        super(service);
    }

    @Override
    protected String getTitle(@Nullable ResourceType entity) {
        return entity == null ? getTranslation("management.resource-type.add") : getTranslation("management.resource-type.edit");
    }

    @Override
    protected Form configureForm(@Nullable ResourceType entity) {
        return new SingleEntityForm() {
            @Override
            protected void initForm(Binder<ResourceType> binder, FormLayout formLayout) {
                binder.setBean(entity == null ? new ResourceType() : entity);

                var code = new TextField(getTranslation("management.code"));
                formLayout.add(code);

                var description = new TextField(getTranslation("management.description"));
                formLayout.add(description);

                binder.bind(code, ResourceType::getCode, ResourceType::setCode);
                binder.bind(description, ResourceType::getDescription, ResourceType::setDescription);

                code.focus();
            }
        };
    }
}
