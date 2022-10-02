package net.pkhapps.idispatch.server.boundary;

import net.pkhapps.idispatch.server.entity.Resource;

import java.io.Serializable;
import java.time.Instant;

public class ReportResourceDTO implements Serializable {

    public static final String PROP_CALL_SIGN = "callSign";
    public static final String PROP_DISPATCHED = "dispatched";
    public static final String PROP_EN_ROUTE = "enRoute";
    public static final String PROP_ON_SCENE = "onScene";
    public static final String PROP_AVAILABLE = "available";
    public static final String PROP_AT_STATION = "atStation";

    private final Resource resource;
    private final Instant dispatched;
    private final Instant enRoute;
    private final Instant onScene;
    private final Instant available;
    private final Instant atStation;

    public ReportResourceDTO(Resource resource, Instant dispatched, Instant enRoute, Instant onScene, Instant available, Instant atStation) {
        this.resource = resource;
        this.dispatched = dispatched;
        this.enRoute = enRoute;
        this.onScene = onScene;
        this.available = available;
        this.atStation = atStation;
    }

    public Resource getResource() {
        return resource;
    }

    public String getCallSign() {
        return resource.getCallSign();
    }

    public Instant getDispatched() {
        return dispatched;
    }

    public Instant getEnRoute() {
        return enRoute;
    }

    public Instant getOnScene() {
        return onScene;
    }

    public Instant getAvailable() {
        return available;
    }

    public Instant getAtStation() {
        return atStation;
    }

    public static class Builder {

        private Resource resource;
        private Instant dispatched;
        private Instant enRoute;
        private Instant onScene;
        private Instant available;
        private Instant atStation;

        public Builder(Resource resource) {
            this.resource = resource;
        }

        public Instant getDispatched() {
            return dispatched;
        }

        public Builder setDispatched(Instant dispatched) {
            this.dispatched = dispatched;
            return this;
        }

        public Instant getEnRoute() {
            return enRoute;
        }

        public Builder setEnRoute(Instant enRoute) {
            this.enRoute = enRoute;
            return this;
        }

        public Instant getOnScene() {
            return onScene;
        }

        public Builder setOnScene(Instant onScene) {
            this.onScene = onScene;
            return this;
        }

        public Instant getAvailable() {
            return available;
        }

        public Builder setAvailable(Instant available) {
            this.available = available;
            return this;
        }

        public Instant getAtStation() {
            return atStation;
        }

        public Builder setAtStation(Instant atStation) {
            this.atStation = atStation;
            return this;
        }

        public ReportResourceDTO build() {
            return new ReportResourceDTO(resource, dispatched, enRoute, onScene, available, atStation);
        }


    }
}
