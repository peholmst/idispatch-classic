package net.pkhapps.idispatch.server.shared.management;

import org.jspecify.annotations.Nullable;

public record DefaultFilter(
        boolean includesDeleted,
        @Nullable String searchTerm
) implements ManagementService.SoftDeletableFilter, ManagementService.TextSearchFilter {
}
