package net.pkhapps.idispatch.server.resource;

import com.vaadin.flow.i18n.I18NProvider;
import net.pkhapps.idispatch.server.ui.Formatters;

public final class ResourceFormatters {
    private ResourceFormatters() {
    }

    public static String formatResourceType(ResourceTypeListItem resourceType) {
        return resourceType == null ? null
                : resourceType.code() + " - " + resourceType.description(Formatters.currentLocale());
    }

    public static String formatResourceState(ResourceState resourceState) {
        return resourceState == null ? null : I18NProvider.translate("resource-state." + resourceState.name());
    }

    public static String resourceStateClassName(ResourceState resourceState) {
        return resourceState == null ? null : String.format("resource-state-%s", resourceState.name().toLowerCase());
    }
}
