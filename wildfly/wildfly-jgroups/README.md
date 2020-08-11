# Infinispan Wildfly JGroups Module

An integration module for use by Infinispan XML configuration that can inject Wildfly managed JGroups channel

## Usage

Include this module in the classpath, then add the following configuration in the infinispan.xml:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<infinispan ...>
  <cache-container ...>
    <transport ...>
      <property name="channelLookup">org.infinispan.configuration.jgroups.wildfly.WildflyJGroupsChannelLookup</property>
    </transport>
  </cache-container>
</infinispan>
```

## Properties


| Property Name | Description | Default Value |
| --- | --- | --- |
| `org.infinispan.configuration.jgroups.wildfly.WildflyJGroupsChannelLookup.jndi` | the JNDI path of `org.wildfly.clustering.jgroups.ChannelFactory` instance | `java:jboss/jgroups/factory/default` |
| `org.infinispan.configuration.jgroups.wildfly.WildflyJGroupsChannelLookup.channelId` | Wildfly managed JGroups channel ID | `ISPN` |
| `org.infinispan.configuration.jgroups.wildfly.WildflyJGroupsChannelLookup.shouldConnect` | should connect the channel before using it | `true` |
| `org.infinispan.configuration.jgroups.wildfly.WildflyJGroupsChannelLookup.shouldDisconnect` | should disconnect the channel once it is done with it | `true` |
| `org.infinispan.configuration.jgroups.wildfly.WildflyJGroupsChannelLookup.shouldClose` | should close the channel once it is done with it | `true` |
