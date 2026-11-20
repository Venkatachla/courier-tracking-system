package com.courier.gateway.security;

import java.util.List;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Component
public class AuthorizationRules {
    private static final Set<String> ANY_ROLE = Set.of("ADMIN", "CUSTOMER", "COURIER");
    private final AntPathMatcher paths = new AntPathMatcher();
    private final List<Rule> rules = List.of(
            new Rule(HttpMethod.GET, "/api/users", Set.of("ADMIN")),
            new Rule(HttpMethod.GET, "/api/users/me", ANY_ROLE),
            new Rule(HttpMethod.POST, "/api/shipments", Set.of("CUSTOMER", "ADMIN")),
            new Rule(HttpMethod.GET, "/api/shipments/my", ANY_ROLE),
            new Rule(HttpMethod.GET, "/api/shipments/*", ANY_ROLE),
            new Rule(HttpMethod.PUT, "/api/shipments/*/assign", Set.of("ADMIN")),
            new Rule(HttpMethod.PUT, "/api/shipments/*/status", Set.of("COURIER", "ADMIN")),
            new Rule(HttpMethod.PUT, "/api/shipments/*/cancel", Set.of("CUSTOMER", "ADMIN")),
            new Rule(HttpMethod.GET, "/api/tracking/*/history", ANY_ROLE),
            new Rule(HttpMethod.POST, "/api/tracking/events", Set.of("COURIER", "ADMIN")));

    public Set<String> allowedRoles(HttpMethod method, String path) {
        return rules.stream().filter(rule -> rule.method().equals(method) && paths.match(rule.path(), path))
                .map(Rule::roles).findFirst().orElse(Set.of("ADMIN"));
    }

    private record Rule(HttpMethod method, String path, Set<String> roles) {}
}
