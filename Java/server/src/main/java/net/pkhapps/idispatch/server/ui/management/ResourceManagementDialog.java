package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import net.pkhapps.idispatch.server.boundary.ResourceManagementService;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.ResourceType;
import org.springframework.lang.Nullable;

public class ResourceManagementDialog extends AbstractManagementDialog<Resource> {

    public ResourceManagementDialog(ResourceManagementService service) {
        super(service);
    }

    @Override
    protected String getTitle(@Nullable Resource entity) {
        return entity == null ? "Add Resource" : "Edit Resource";
    }

    @Override
    public ResourceManagementService service() {
        return (ResourceManagementService) super.service();
    }

    @Override
    protected Form configureForm(@Nullable Resource entity) {
        return new SingleEntityForm() {
            @Override
            protected void initForm(Binder<Resource> binder, FormLayout formLayout) {
                binder.setBean(entity == null ? new Resource() : entity);

                var callSign = new TextField("Call sign");
                formLayout.add(callSign);

                var type = new ComboBox<ResourceType>("Type");
                type.setItemLabelGenerator(ResourceType::getFormattedDescription);
                type.setItems(service().findApplicableTypes());
                formLayout.add(type);

                binder.bind(callSign, Resource::getCallSign, Resource::setCallSign);
                binder.bind(type, Resource::getType, Resource::setType);

                callSign.focus();
            }
        };
    }
}
