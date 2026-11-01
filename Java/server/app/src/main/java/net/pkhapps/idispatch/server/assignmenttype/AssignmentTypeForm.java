package net.pkhapps.idispatch.server.assignmenttype;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.signals.Signal;
import net.pkhapps.idispatch.server.shared.BilingualText;
import net.pkhapps.idispatch.server.ui.management.RecordForm;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Creates the {@link AssignmentTypeDetails} from a code and a description in Finnish and Swedish, of which one is
 * enough.
 */
class AssignmentTypeForm extends RecordForm<AssignmentTypeDetails> {

    private final TextField code;
    private final Signal<Optional<AssignmentTypeDetails>> draft;

    AssignmentTypeForm(@Nullable AssignmentType assignmentType) {
        code = new TextField(getTranslation("management.code"));
        code.setRequiredIndicatorVisible(true);
        var descriptionFi = new TextField(getTranslation("management.description-fi"));
        var descriptionSv = new TextField(getTranslation("management.description-sv"));
        getContent().add(code, descriptionFi, descriptionSv);

        var codeTouched = touchedSignal();
        var descriptionTouched = touchedSignal();
        var codeValue = bind(code, assignmentType == null ? "" : assignmentType.code().value(), codeTouched);
        var finnish = bind(descriptionFi, assignmentType == null ? "" : assignmentType.description().finnish(),
                descriptionTouched);
        var swedish = bind(descriptionSv, assignmentType == null ? "" : assignmentType.description().swedish(),
                descriptionTouched);

        var codeError = codeValue.map(value -> AssignmentTypeCode.isValid(value)
                ? null : getTranslation("management.code-required"));
        var descriptionError = Signal.computed(() -> BilingualText.isValid(finnish.get(), swedish.get())
                ? null : getTranslation("management.description-required"));
        showError(code, codeError, codeTouched);
        showError(descriptionFi, descriptionError, descriptionTouched);
        showError(descriptionSv, descriptionError, descriptionTouched);

        draft = Signal.computed(() -> codeError.get() == null && descriptionError.get() == null
                ? Optional.of(new AssignmentTypeDetails(new AssignmentTypeCode(codeValue.get()),
                new BilingualText(finnish.get(), swedish.get())))
                : Optional.empty());
    }

    @Override
    protected Signal<Optional<AssignmentTypeDetails>> draft() {
        return draft;
    }

    @Override
    public void focus() {
        code.focus();
    }
}
