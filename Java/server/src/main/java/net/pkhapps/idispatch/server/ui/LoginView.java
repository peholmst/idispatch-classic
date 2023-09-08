package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("iDispatch Login")
@AnonymousAllowed
@CssImport("./styles/login-view-styles.css")
public class LoginView extends Div implements BeforeEnterObserver {

    private final LoginForm login;

    public LoginView() {
        login = new LoginForm();
        login.setAction("login");
        login.setForgotPasswordButtonVisible(false);

        addClassName("login-view");
        setSizeFull();

        add(login);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (beforeEnterEvent.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            login.setError(true);
        }
    }
}
