package org.infinispan.configuration.jgroups.wildfly;

import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.remoting.transport.jgroups.JGroupsChannelLookup;
import org.jgroups.JChannel;
import org.wildfly.clustering.jgroups.ChannelFactory;

/**
 * JGroupsChannelLookup implementation that lookup the JGroups channel factory from JNDI.
 * Useful when integrated with Wildfly to reuse the existing JGroups stack managed by Wildfly.
 * Note the default JNDI path of JGroups stack in Wildfly is "java:jboss/jgroups/factory/default"
 *
 * @author Sammy Chu
 * @since 12.0
 */
public class WildflyJGroupsChannelLookup implements JGroupsChannelLookup {

   public static final String DEFAULT_JNDI = "java:jboss/jgroups/factory/default";
   public static final String DEFAULT_CHANNEL_ID = "ISPN";
   public static final String JNDI_PRPERTY_NAME = WildflyJGroupsChannelLookup.class.getName() + ".jndi";
   public static final String CHANNEL_ID_PROPERTY_NAME = WildflyJGroupsChannelLookup.class.getName() + ".channelId";
   public static final String SHOULD_CONNECT_PROPERTY_NAME = WildflyJGroupsChannelLookup.class.getName() + ".shouldConnect";
   public static final String SHOULD_DISCONNECT_PROPERTY_NAME = WildflyJGroupsChannelLookup.class.getName() + ".shouldDisconnect";
   public static final String SHOULD_CLOSE_PROPERTY_NAME = WildflyJGroupsChannelLookup.class.getName() + ".shouldClose";

   private String jndi = DEFAULT_JNDI;
   private String channelId = DEFAULT_CHANNEL_ID;
   private boolean shouldConnect = true;
   private boolean shouldDisconnect = true;
   private boolean shouldClose = true;

   protected static String getPropertyValue(Properties properties, String key, String defaultValue) {
      if (properties != null) {
         return properties.getProperty(key, defaultValue);
      }
      return defaultValue;
   }

   @Override
   public JChannel getJGroupsChannel(Properties properties) {
      jndi = getPropertyValue(properties, JNDI_PRPERTY_NAME, DEFAULT_JNDI);
      if (jndi == null || jndi.isEmpty()) {
         throw new CacheConfigurationException(String.format(
               "Property name '%s' to lookup instance of org.wildfly.clustering.jgroups.ChannelFactory is required",
               JNDI_PRPERTY_NAME));
      }
      channelId = getPropertyValue(properties, CHANNEL_ID_PROPERTY_NAME, DEFAULT_CHANNEL_ID);
      shouldConnect = Boolean.parseBoolean(getPropertyValue(properties, SHOULD_CONNECT_PROPERTY_NAME, "true"));
      shouldDisconnect = Boolean.parseBoolean(getPropertyValue(properties, SHOULD_DISCONNECT_PROPERTY_NAME, "true"));
      shouldClose = Boolean.parseBoolean(getPropertyValue(properties, SHOULD_CLOSE_PROPERTY_NAME, "true"));

      try {
         ChannelFactory channedlFactory = InitialContext.doLookup(jndi);
         return channedlFactory.createChannel(channelId);
      } catch (NamingException e) {
         throw new CacheConfigurationException(String.format(
               "Configured channel factory '%s' cannot be resolved from JNDI", jndi), e);
      } catch (Exception e) {
         throw new CacheConfigurationException(String.format(
               "Configured channel factory '%s' fail to create channel '%s'", jndi, channelId), e);
      }
   }

   @Override
   public boolean shouldConnect() {
      return shouldConnect;
   }

   @Override
   public boolean shouldDisconnect() {
      return shouldDisconnect;
   }

   @Override
   public boolean shouldClose() {
      return shouldClose;
   }
}
