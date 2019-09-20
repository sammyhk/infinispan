package org.infinispan.client.hotrod.configuration;

import java.util.Properties;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.FailoverRequestBalancingStrategy;
import org.infinispan.client.hotrod.ProtocolVersion;
import org.infinispan.client.hotrod.impl.consistenthash.ConsistentHash;
import org.infinispan.commons.marshall.Marshaller;

/**
 * AbstractConfigurationChildBuilder.
 *
 * @author Tristan Tarrant
 * @since 5.3
 */
public abstract class AbstractConfigurationChildBuilder implements ConfigurationChildBuilder {
   final ConfigurationBuilder builder;

   protected AbstractConfigurationChildBuilder(ConfigurationBuilder builder) {
      this.builder = builder;
   }

   @Override
   public ServerConfigurationBuilder addServer() {
      return builder.addServer();
   }

   @Override
   public ClusterConfigurationBuilder addCluster(String clusterName) {
      return builder.addCluster(clusterName);
   }

   @Override
   public ConfigurationBuilder addServers(String servers) {
      return builder.addServers(servers);
   }

   @Override
   public ExecutorFactoryConfigurationBuilder asyncExecutorFactory() {
      return builder.asyncExecutorFactory();
   }

   @Override
   public ConfigurationBuilder balancingStrategy(String balancingStrategy) {
      return builder.balancingStrategy(balancingStrategy);
   }

   @Override
   public ConfigurationBuilder balancingStrategy(Class<? extends FailoverRequestBalancingStrategy> balancingStrategy) {
      return builder.balancingStrategy(balancingStrategy);
   }

   @Override
   public ConfigurationBuilder balancingStrategy(FailoverRequestBalancingStrategy balancingStrategy) {
      return builder.balancingStrategy(balancingStrategy);
   }

   @Override
   public ConfigurationBuilder balancingStrategy(Supplier<FailoverRequestBalancingStrategy> balancingStrategyFactory) {
      return builder.balancingStrategy(balancingStrategyFactory);
   }

   @Override
   public ConfigurationBuilder classLoader(ClassLoader classLoader) {
      return builder.classLoader(classLoader);
   }

   @Override
   public ConfigurationBuilder clientIntelligence(ClientIntelligence clientIntelligence) {
      return builder.clientIntelligence(clientIntelligence);
   }

   @Override
   public ConnectionPoolConfigurationBuilder connectionPool() {
      return builder.connectionPool();
   }

   @Override
   public ConfigurationBuilder connectionTimeout(int connectionTimeout) {
      return builder.connectionTimeout(connectionTimeout);
   }

   @Override
   public ConfigurationBuilder consistentHashImpl(int version, Class<? extends ConsistentHash> consistentHashClass) {
      return builder.consistentHashImpl(version, consistentHashClass);
   }

   @Override
   public ConfigurationBuilder consistentHashImpl(int version, String consistentHashClass) {
      return builder.consistentHashImpl(version, consistentHashClass);
   }

   @Override
   public ConfigurationBuilder forceReturnValues(boolean forceReturnValues) {
      return builder.forceReturnValues(forceReturnValues);
   }

   @Override
   public ConfigurationBuilder keySizeEstimate(int keySizeEstimate) {
      return builder.keySizeEstimate(keySizeEstimate);
   }

   @Override
   public ConfigurationBuilder marshaller(String marshaller) {
      return builder.marshaller(marshaller);
   }

   @Override
   public ConfigurationBuilder marshaller(Class<? extends Marshaller> marshaller) {
      return builder.marshaller(marshaller);
   }

   @Override
   public ConfigurationBuilder marshaller(Marshaller marshaller) {
      return builder.marshaller(marshaller);
   }

   /**
    * @deprecated Use {@link #version(ProtocolVersion)} instead.
    */
   @Deprecated
   @Override
   public ConfigurationBuilder protocolVersion(String protocolVersion) {
      return builder.version(ProtocolVersion.parseVersion(protocolVersion));
   }

   @Override
   public ConfigurationBuilder version(ProtocolVersion protocolVersion) {
      return builder.version(protocolVersion);
   }

   @Override
   public ConfigurationBuilder socketTimeout(int socketTimeout) {
      return builder.socketTimeout(socketTimeout);
   }

   @Override
   public SecurityConfigurationBuilder security() {
      return builder.security();
   }

   @Override
   public ConfigurationBuilder tcpNoDelay(boolean tcpNoDelay) {
      return builder.tcpNoDelay(tcpNoDelay);
   }

   @Override
   public ConfigurationBuilder tcpKeepAlive(boolean tcpKeepAlive) {
      return builder.tcpKeepAlive(tcpKeepAlive);
   }

   @Override
   public ConfigurationBuilder valueSizeEstimate(int valueSizeEstimate) {
      return builder.valueSizeEstimate(valueSizeEstimate);
   }

   @Override
   public ConfigurationBuilder maxRetries(int retriesPerServer) {
      return builder.maxRetries(retriesPerServer);
   }

   @Override
   public ConfigurationBuilder addJavaSerialWhiteList(String... regExs) {
      return builder.addJavaSerialWhiteList(regExs);
   }

   @Override
   public ConfigurationBuilder batchSize(int batchSize) {
      return builder.batchSize(batchSize);
   }

   @Override
   public StatisticsConfigurationBuilder statistics() {
      return builder.statistics();
   }

   @Override
   public TransactionConfigurationBuilder transaction() {
      return builder.transaction();
   }

   @Override
   public ConfigurationBuilder withProperties(Properties properties) {
      return builder.withProperties(properties);
   }

   @Override
   public Configuration build() {
      return builder.build();
   }

}
