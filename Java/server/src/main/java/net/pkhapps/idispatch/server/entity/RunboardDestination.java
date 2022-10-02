package net.pkhapps.idispatch.server.entity;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import java.util.UUID;

@Entity
@Table(name = "runboard_destinations")
public class RunboardDestination extends Destination {

    @Column(name = "runboard_key", nullable = false, unique = true)
    @NotBlank(message = "Please enter a unique runboard key")
    private String runboardKey = UUID.randomUUID().toString();

    public String getRunboardKey() {
        return runboardKey;
    }

    public void setRunboardKey(String runboardKey) {
        this.runboardKey = runboardKey;
    }

    @Override
    public String getType() {
        return "Runboard";
    }
}
