package net.pkhapps.idispatch.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends AbstractLockableEntity {

    @Column(name = "username", nullable = false, unique = true)
    private String username;
    @Column(name = "encoded_password", nullable = false)
    private String encodedPassword;
    @Column(name = "is_dispatcher", nullable = false)
    private boolean isDispatcher;
    @Column(name = "is_admin", nullable = false)
    private boolean isAdmin;
    @Column(name = "is_report_reader", nullable = false)
    private boolean isReportReader;
    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled;

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEncodedPassword(String encodedPassword) {
        this.encodedPassword = encodedPassword;
    }

    public void setDispatcher(boolean dispatcher) {
        isDispatcher = dispatcher;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public void setReportReader(boolean reportReader) {
        isReportReader = reportReader;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public String getUsername() {
        return username;
    }

    public String getEncodedPassword() {
        return encodedPassword;
    }

    public boolean isDispatcher() {
        return isDispatcher;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public boolean isReportReader() {
        return isReportReader;
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}
