package net.pkhapps.idispatch.server.dispatch.mobile;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.pkhapps.idispatch.server.dispatch.DestinationQueries;
import net.pkhapps.idispatch.server.dispatch.MobileKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates a request of the mobile app by the key in its {@code Authorization: Bearer} header, as the mobile
 * destination with that key. A request without a key, or with one that no destination has, is left unauthenticated,
 * and the security chain refuses it. The key is never taken from the address, which proxies log.
 */
final class MobileKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(MobileKeyAuthenticationFilter.class);
    private static final String BEARER = "Bearer ";

    private final DestinationQueries destinationQueries;

    MobileKeyAuthenticationFilter(DestinationQueries destinationQueries) {
        this.destinationQueries = destinationQueries;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var header = request.getHeader(HttpHeaders.AUTHORIZATION);
        var value = header != null && header.startsWith(BEARER) ? header.substring(BEARER.length()).trim() : null;
        if (MobileKey.isValid(value)) {
            var key = new MobileKey(value);
            destinationQueries.findActiveMobile(key).ifPresentOrElse(destination -> {
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new MobileAppAuthentication(destination, key.hash()));
                SecurityContextHolder.setContext(context);
            }, () -> log.debug("Refusing a mobile app request with a key that no mobile destination has"));
        }
        chain.doFilter(request, response);
    }
}
