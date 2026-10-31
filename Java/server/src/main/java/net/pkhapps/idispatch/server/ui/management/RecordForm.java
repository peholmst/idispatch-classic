package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base class for the forms in the detail area of a {@link RecordManagementView}. Each field is bound to a signal, and
 * the subclass derives from those signals an error per field and the domain value that the form creates, such as the
 * details of a record. The form never holds an invalid domain value: {@link #draft()} is empty for as long as any field
 * has an error.
 * <p>
 * A field shows its error once the user has changed it, or once the user has tried to save. Fields whose errors depend
 * on each other, such as the two versions of a bilingual text, share one "touched" signal so that changing either shows
 * the error on both.
 *
 * @param <D> the domain value the form creates
 */
public abstract class RecordForm<D> extends Composite<FormLayout> {

    private final ValueSignal<Boolean> readOnly = new ValueSignal<>(false);
    private final ValueSignal<Boolean> saveAttempted = new ValueSignal<>(false);
    private final List<Signal<Boolean>> touched = new ArrayList<>();

    protected RecordForm() {
        var formLayout = getContent();
        formLayout.setAutoResponsive(true);
        formLayout.setExpandColumns(true);
        formLayout.setExpandFields(true);
    }

    /**
     * Returns a new signal that becomes {@code true} when the user changes any field bound with it.
     */
    protected ValueSignal<Boolean> touchedSignal() {
        var signal = new ValueSignal<>(false);
        touched.add(signal);
        return signal;
    }

    /**
     * Binds the field to a new signal with the given initial value, and makes it read-only with the form.
     *
     * @param touched set when the user changes the field; see {@link #touchedSignal()}
     * @return the signal holding the field's value
     */
    protected <V> ValueSignal<V> bind(HasValueAndElement<?, V> field, V initialValue, ValueSignal<Boolean> touched) {
        var value = new ValueSignal<>(initialValue);
        field.bindValue(value, value::set);
        field.bindReadOnly(readOnly);
        field.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                touched.set(true);
            }
        });
        return value;
    }

    /**
     * Shows the error under the field, once the field has been touched or the user has tried to save.
     *
     * @param error the error message, or {@code null} while the field is valid
     */
    protected void showError(TextField field, Signal<@Nullable String> error, Signal<Boolean> touched) {
        // The form decides what is valid, so the field must not reset the invalid state with its own constraints
        field.setManualValidation(true);
        Signal.effect(field, () -> {
            var message = saveAttempted.get() || touched.get() ? error.get() : null;
            field.setInvalid(message != null);
            field.setErrorMessage(message == null ? "" : message);
        });
    }

    /**
     * The domain value the fields make, or empty while any field has an error.
     */
    protected abstract Signal<Optional<D>> draft();

    /**
     * Shows the errors of every field, and returns the domain value if there are none.
     */
    public Optional<D> validate() {
        saveAttempted.set(true);
        return draft().peek();
    }

    /**
     * Whether the user has changed any field.
     */
    public boolean hasChanges() {
        return touched.stream().anyMatch(Signal::peek);
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly.set(readOnly);
    }

    public abstract void focus();
}
