package edu.zju.approval.auth;

import edu.zju.approval.user.UserQueryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserQueryService userQueryService;

    public DatabaseUserDetailsService(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var record = userQueryService.findAuthenticationRecordByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Authentication failed"));
        var profile = userQueryService.findProfileById(record.id())
                .orElseThrow(() -> new UsernameNotFoundException("Authentication failed"));
        var authorities = profile.roles().stream()
                .filter(role -> "ACTIVE".equals(role.status()))
                .map(role -> new SimpleGrantedAuthority(authorityName(role.roleKey())))
                .toList();

        return User.withUsername(record.username())
                .password(record.passwordHash())
                .authorities(authorities)
                .disabled(!"ACTIVE".equals(record.status()))
                .build();
    }

    static String authorityName(String roleKey) {
        return roleKey.startsWith("ROLE_") ? roleKey : "ROLE_" + roleKey;
    }
}
