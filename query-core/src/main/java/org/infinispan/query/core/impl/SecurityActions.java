package org.infinispan.query.core.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.infinispan.AdvancedCache;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.security.AuthorizationManager;
import org.infinispan.security.Security;

/**
 * SecurityActions for the org.infinispan.query.core.impl package.
 * <p>
 * Do not move. Do not change class and method visibility to avoid being called from other {@link
 * java.security.CodeSource}s, thus granting privilege escalation to external code.
 *
 * @author anistor@redhat.com
 * @since 10.1
 */
final class SecurityActions {

   private SecurityActions() {
   }

   private static <T> T doPrivileged(PrivilegedAction<T> action) {
      return System.getSecurityManager() != null ?
            AccessController.doPrivileged(action) : Security.doPrivileged(action);
   }

   static AuthorizationManager getCacheAuthorizationManager(AdvancedCache<?, ?> cache) {
      return doPrivileged(cache::getAuthorizationManager);
   }

   static ComponentRegistry getCacheComponentRegistry(AdvancedCache<?, ?> cache) {
      return doPrivileged(cache::getComponentRegistry);
   }

   static GlobalComponentRegistry getGlobalComponentRegistry(AdvancedCache<?, ?> cache) {
      return doPrivileged(() -> cache.getCacheManager().getGlobalComponentRegistry());
   }
}
