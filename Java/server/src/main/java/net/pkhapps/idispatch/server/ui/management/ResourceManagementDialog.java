package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.SerializableRunnable;
import net.pkhapps.idispatch.server.boundary.ResourceManagementService;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.ResourceType;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class ResourceManagementDialog extends AbstractManagementDialog<Resource> {

    private final Supplier<List<ResourceType>> getResourceTypes;
    private ComboBox<ResourceType> type;

    public ResourceManagementDialog(ResourceManagementService service, Supplier<List<ResourceType>> getResourceTypes) {
        super(service);
        this.getResourceTypes = requireNonNull(getResourceTypes);
    }

    @Override
    protected String getTitle(Resource entity) {
        return entity.isNew() ? "Add Resource" : "Edit Resource";
    }

    @Override
    protected void configureForm(FormLayout form) {
        var callSign = new TextField("Call sign");
        form.add(callSign);

        type = new ComboBox<>("Type");
        type.setItemLabelGenerator(ResourceType::getFormattedDescription);
        form.add(type);

        getBinder().bind(callSign, Resource::getCallSign, Resource::setCallSign);
        getBinder().bind(type, Resource::getType, Resource::setType);

        callSign.focus();
    }

    @Override
    public void open(Resource entity, SerializableRunnable onClose) {
        type.setItems(getResourceTypes.get());
        super.open(entity, onClose);
    }
}
