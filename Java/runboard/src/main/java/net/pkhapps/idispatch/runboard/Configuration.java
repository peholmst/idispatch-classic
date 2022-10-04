package net.pkhapps.idispatch.runboard;

public class Configuration extends net.pkhapps.idispatch.runboard.client.Configuration {

    public boolean isUndecorated() {
        return Boolean.parseBoolean(getProperty("idispatch.undecorated", "true"));
    }

    public Boolean isLowResolution() {
        return Boolean.parseBoolean(getProperty("idispatch.lowres", "false"));
    }
}
