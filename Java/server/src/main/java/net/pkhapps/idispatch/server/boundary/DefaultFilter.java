package net.pkhapps.idispatch.server.boundary;

import org.jspecify.annotations.Nullable;

public record DefaultFilter(
        boolean includesDeleted,
        @Nullable String searchTerm
) implements ManagementService.SoftDeletableFilter, ManagementService.TextSearchFilter {
}
