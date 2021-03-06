package org.infinispan.configuration.cache;

import static org.infinispan.configuration.parsing.Element.BACKUPS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.commons.configuration.ConfigurationInfo;
import org.infinispan.commons.configuration.attributes.Attribute;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.commons.configuration.attributes.Matchable;
import org.infinispan.commons.configuration.elements.DefaultElementDefinition;
import org.infinispan.commons.configuration.elements.ElementDefinition;

/**
 * @author Mircea.Markus@jboss.com
 * @since 5.2
 */
public class SitesConfiguration implements Matchable<SitesConfiguration>, ConfigurationInfo {
   public static final AttributeDefinition<Boolean> DISABLE_BACKUPS = AttributeDefinition.builder("disable", false).immutable().build();
   public static final AttributeDefinition<Set<String>> IN_USE_BACKUP_SITES = AttributeDefinition.builder("backup-sites-in-use", null, (Class<Set<String>>) (Class<?>) Set.class).initializer(() -> new HashSet<>(2)).immutable().build();
   public static final ElementDefinition ELEMENT_DEFINITION = new DefaultElementDefinition(BACKUPS.getLocalName());

   static AttributeSet attributeDefinitionSet() {
      return new AttributeSet(SitesConfiguration.class, DISABLE_BACKUPS, IN_USE_BACKUP_SITES);
   }

   private final BackupForConfiguration backupFor;
   private final List<BackupConfiguration> allBackups;
   private final Attribute<Boolean> disableBackups;
   private final Attribute<Set<String>> inUseBackupSites;
   private final AttributeSet attributes;

   private final List<ConfigurationInfo> subElements = new ArrayList<>();

   public SitesConfiguration(AttributeSet attributes, List<BackupConfiguration> allBackups, BackupForConfiguration backupFor) {
      this.attributes = attributes.checkProtection();
      this.allBackups = Collections.unmodifiableList(allBackups);
      this.disableBackups = attributes.attribute(DISABLE_BACKUPS);
      this.inUseBackupSites = attributes.attribute(IN_USE_BACKUP_SITES);
      this.backupFor = backupFor;
      this.subElements.addAll(allBackups);
   }

   @Override
   public List<ConfigurationInfo> subElements() {
      return subElements;
   }

   @Override
   public ElementDefinition getElementDefinition() {
      return ELEMENT_DEFINITION;
   }

   /**
    * Returns true if this cache won't backup its data remotely.
    * It would still accept other sites backing up data on this site.
    */
   public boolean disableBackups() {
      return disableBackups.get();
   }

   /**
    * Returns the list of all sites where this cache might back up its data. The list of actual sites is defined by
    * {@link #inUseBackupSites}.
    */
   public List<BackupConfiguration> allBackups() {
      return allBackups;
   }

   /**
    * Returns the list of {@link BackupConfiguration} that have {@link org.infinispan.configuration.cache.BackupConfiguration#enabled()} == true.
    */
   public List<BackupConfiguration> enabledBackups() {
      return enabledBackupStream().collect(Collectors.toList());
   }

   public Stream<BackupConfiguration> enabledBackupStream() {
      return allBackups.stream().filter(BackupConfiguration::enabled);
   }

   /**
    * @return information about caches that backup data into this cache.
    */
   public BackupForConfiguration backupFor() {
      return backupFor;
   }

   public BackupFailurePolicy getFailurePolicy(String siteName) {
      for (BackupConfiguration bc : allBackups) {
         if (bc.site().equals(siteName)) {
            return bc.backupFailurePolicy();
         }
      }
      throw new IllegalStateException("There must be a site configured for " + siteName);
   }

   public boolean hasInUseBackup(String siteName) {
      for (BackupConfiguration bc : allBackups) {
         if (bc.site().equals(siteName)) {
            return bc.enabled();
         }
      }
      return false;
   }

   public boolean hasEnabledBackups() {
      return allBackups.stream().anyMatch(BackupConfiguration::enabled);
   }

   public boolean hasSyncEnabledBackups() {
      return enabledBackupStream().anyMatch(BackupConfiguration::isSyncBackup);
   }

   public Stream<BackupConfiguration> syncBackupsStream() {
      return enabledBackupStream().filter(BackupConfiguration::isSyncBackup);
   }

   public boolean hasAsyncEnabledBackups() {
      return enabledBackupStream().anyMatch(BackupConfiguration::isAsyncBackup);
   }

   public Stream<BackupConfiguration> asyncBackupsStream() {
      return enabledBackupStream().filter(BackupConfiguration::isAsyncBackup);
   }

   public Set<String> inUseBackupSites() {
      return inUseBackupSites.get();
   }

   public AttributeSet attributes() {
      return attributes;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
      result = prime * result + ((backupFor == null) ? 0 : backupFor.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      SitesConfiguration other = (SitesConfiguration) obj;
      return Objects.equals(attributes, other.attributes) &&
            Objects.equals(backupFor, other.backupFor);
   }

   @Override
   public String toString() {
      return "SitesConfiguration [backupFor=" + backupFor + ", allBackups=" + allBackups + ", attributes=" + attributes + "]";
   }
}
