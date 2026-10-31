package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.textfield.TextField;
import net.pkhapps.idispatch.server.entity.AbstractEntity;

/**
 * Form for the assignment and resource types, which both have a {@code code} and a description in Finnish
 * ({@code descriptionFi}) and Swedish ({@code descriptionSv}).
 */
class CodeAndDescriptionForm<E extends AbstractEntity> extends ManagementForm<E> {

    private final TextField code;

    CodeAndDescriptionForm(Class<E> entityType) {
        super(entityType);

        code = new TextField(getTranslation("management.code"));
        var descriptionFi = new TextField(getTranslation("management.description-fi"));
        var descriptionSv = new TextField(getTranslation("management.description-sv"));
        getContent().add(code, descriptionFi, descriptionSv);

        binder.forField(code).bind("code");
        bindBilingual(descriptionFi, "descriptionFi", descriptionSv, "descriptionSv", "management.description-required");
    }

    @Override
    public void focus() {
        code.focus();
    }
}
