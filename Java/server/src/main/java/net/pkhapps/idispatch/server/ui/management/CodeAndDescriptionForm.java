package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.textfield.TextField;
import net.pkhapps.idispatch.server.entity.AbstractEntity;

/**
 * Form for the assignment and resource types, which both have a {@code code} and a {@code description}.
 */
class CodeAndDescriptionForm<E extends AbstractEntity> extends ManagementForm<E> {

    private final TextField code;

    CodeAndDescriptionForm(Class<E> entityType) {
        super(entityType);

        code = new TextField(getTranslation("management.code"));
        var description = new TextField(getTranslation("management.description"));
        getContent().add(code, description);

        binder.forField(code).bind("code");
        binder.forField(description).bind("description");
    }

    @Override
    public void focus() {
        code.focus();
    }
}
