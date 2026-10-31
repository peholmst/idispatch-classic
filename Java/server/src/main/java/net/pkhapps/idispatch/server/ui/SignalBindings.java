package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import org.jspecify.annotations.Nullable;

/**
 * Signal bindings for component properties that Vaadin has no {@code bind…} method for.
 */
public final class SignalBindings {

    private SignalBindings() {
    }

    /**
     * Binds the field's error to the signal: the field is invalid and shows the message while the signal holds one,
     * and is valid while it holds {@code null}. Puts the field in manual validation mode, so that its own constraints
     * do not reset the invalid state. Like the {@code bind…} methods of the components, the binding is active while the
     * field is attached.
     *
     * @return a registration that removes the binding
     */
    public static <F extends Component & HasValidation> Registration bindErrorMessage(F field,
                                                                                     Signal<@Nullable String> errorMessage) {
        field.setManualValidation(true);
        return Signal.effect(field, () -> {
            var message = errorMessage.get();
            field.setInvalid(message != null);
            field.setErrorMessage(message == null ? "" : message);
        });
    }
}
