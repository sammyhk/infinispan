package org.infinispan.query.backend;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.hibernate.search.util.common.impl.Futures;
import org.infinispan.AdvancedCache;
import org.infinispan.commands.FlagAffectedCommand;
import org.infinispan.commands.SegmentSpecificCommand;
import org.infinispan.commands.VisitableCommand;
import org.infinispan.commands.functional.ReadWriteKeyCommand;
import org.infinispan.commands.functional.ReadWriteKeyValueCommand;
import org.infinispan.commands.functional.ReadWriteManyCommand;
import org.infinispan.commands.functional.ReadWriteManyEntriesCommand;
import org.infinispan.commands.functional.WriteOnlyKeyCommand;
import org.infinispan.commands.functional.WriteOnlyKeyValueCommand;
import org.infinispan.commands.functional.WriteOnlyManyCommand;
import org.infinispan.commands.functional.WriteOnlyManyEntriesCommand;
import org.infinispan.commands.write.ClearCommand;
import org.infinispan.commands.write.ComputeCommand;
import org.infinispan.commands.write.ComputeIfAbsentCommand;
import org.infinispan.commands.write.DataWriteCommand;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.commands.write.PutMapCommand;
import org.infinispan.commands.write.RemoveCommand;
import org.infinispan.commands.write.ReplaceCommand;
import org.infinispan.commands.write.WriteCommand;
import org.infinispan.commons.util.IntSet;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.impl.FlagBitSets;
import org.infinispan.context.impl.TxInvocationContext;
import org.infinispan.distribution.DistributionInfo;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.distribution.ch.KeyPartitioner;
import org.infinispan.encoding.DataConversion;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.interceptors.DDAsyncInterceptor;
import org.infinispan.interceptors.InvocationSuccessAction;
import org.infinispan.query.logging.Log;
import org.infinispan.search.mapper.mapping.SearchMapping;
import org.infinispan.search.mapper.mapping.SearchMappingHolder;
import org.infinispan.search.mapper.work.SearchIndexer;
import org.infinispan.transaction.xa.GlobalTransaction;
import org.infinispan.util.concurrent.BlockingManager;
import org.infinispan.util.logging.LogFactory;

/**
 * This interceptor will be created when the System Property "infinispan.query.indexLocalOnly" is "false"
 * <p>
 * This type of interceptor will allow the indexing of data even when it comes from other caches within a cluster.
 * <p>
 * However, if the a cache would not be putting the data locally, the interceptor will not index it.
 *
 * @author Navin Surtani
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2011 Red Hat Inc.
 * @author Marko Luksa
 * @author anistor@redhat.com
 * @since 4.0
 */
public final class QueryInterceptor extends DDAsyncInterceptor {
   private static final Log log = LogFactory.getLog(QueryInterceptor.class, Log.class);
   private static final boolean trace = log.isTraceEnabled();

   static final Object UNKNOWN = new Object() {
      @Override
      public String toString() {
         return "<UNKNOWN>";
      }
   };

   @Inject DistributionManager distributionManager;
   @Inject BlockingManager blockingManager;
   @Inject protected KeyPartitioner keyPartitioner;

   private final SearchMappingHolder searchMappingHolder;
   private final KeyTransformationHandler keyTransformationHandler;
   private final AtomicBoolean stopping = new AtomicBoolean(false);
   private final ConcurrentMap<GlobalTransaction, Map<Object, Object>> txOldValues;
   private final DataConversion valueDataConversion;
   private final DataConversion keyDataConversion;
   private final boolean isPersistenceEnabled;

   private final InvocationSuccessAction<ClearCommand> processClearCommand = this::processClearCommand;
   private final boolean isManualIndexing;
   private final AdvancedCache<?, ?> cache;
   private final Map<String, Class<?>> indexedClasses;
   private SegmentListener segmentListener;

   public QueryInterceptor(SearchMappingHolder searchMappingHolder, KeyTransformationHandler keyTransformationHandler,
                           boolean isManualIndexing, ConcurrentMap<GlobalTransaction, Map<Object, Object>> txOldValues,
                           AdvancedCache<?, ?> cache, Map<String, Class<?>> indexedClasses) {
      this.searchMappingHolder = searchMappingHolder;
      this.keyTransformationHandler = keyTransformationHandler;
      this.isManualIndexing = isManualIndexing;
      this.txOldValues = txOldValues;
      this.valueDataConversion = cache.getValueDataConversion();
      this.keyDataConversion = cache.getKeyDataConversion();
      this.isPersistenceEnabled = cache.getCacheConfiguration().persistence().usingStores();
      this.cache = cache;
      this.indexedClasses = Collections.unmodifiableMap(indexedClasses);
   }

   @Start
   protected void start() {
      stopping.set(false);
      boolean isClustered = cache.getCacheConfiguration().clustering().cacheMode().isClustered();
      if (isClustered) {
         segmentListener = new SegmentListener(cache, this::purgeIndex, blockingManager);
         this.cache.addListener(segmentListener);
      }
   }

   public void prepareForStopping() {
      if (segmentListener != null) cache.removeListener(segmentListener);
      stopping.set(true);
   }

   private boolean shouldModifyIndexes(FlagAffectedCommand command, InvocationContext ctx, Object key) {
      if (isManualIndexing) return false;

      if (distributionManager == null || key == null) {
         return true;
      }

      DistributionInfo info = distributionManager.getCacheTopology().getDistribution(key);
      // If this is a backup node we should modify the entry in the remote context
      return info.isPrimary() || info.isWriteOwner() &&
            (ctx.isInTxScope() || !ctx.isOriginLocal() || command != null && command.hasAnyFlag(FlagBitSets.PUT_FOR_STATE_TRANSFER));
   }

   public BlockingManager getBlockingManager() {
      return blockingManager;
   }

   private Object handleDataWriteCommand(InvocationContext ctx, DataWriteCommand command) {
      if (command.hasAnyFlag(FlagBitSets.SKIP_INDEXING)) {
         return invokeNext(ctx, command);
      }
      CacheEntry entry = ctx.lookupEntry(command.getKey());
      if (ctx.isInTxScope()) {
         // replay of modifications on remote node by EntryWrappingVisitor
         if (entry != null && !entry.isChanged() && (entry.getValue() != null || !unreliablePreviousValue(command))) {
            Map<Object, Object> oldValues = registerOldValues((TxInvocationContext) ctx);
            oldValues.putIfAbsent(command.getKey(), entry.getValue());
         }
         return invokeNext(ctx, command);
      } else {
         Object prev = entry != null ? entry.getValue() : UNKNOWN;
         if (prev == null && unreliablePreviousValue(command)) {
            prev = UNKNOWN;
         }
         Object oldValue = prev;
         return invokeNextThenApply(ctx, command, (rCtx, cmd, rv) -> {
            if (!cmd.isSuccessful()) {
               return rv;
            }
            CacheEntry entry2 = entry != null ? entry : rCtx.lookupEntry(cmd.getKey());
            if (entry2 != null && entry2.isChanged()) {
               // TODO: need to reduce the scope of the blocking thread to less if possible later as part of
               // https://issues.redhat.com/browse/ISPN-11731
               return asyncValue(blockingManager.runBlocking(() -> processChange(rCtx, cmd, cmd.getKey(), oldValue,
                     entry2.getValue()), cmd)
                     .thenApply(ignore -> rv));
            }
            return rv;
         });
      }
   }

   private Map<Object, Object> registerOldValues(TxInvocationContext ctx) {
      return txOldValues.computeIfAbsent(ctx.getGlobalTransaction(), gid -> {
         ctx.getCacheTransaction().addListener(() -> txOldValues.remove(gid));
         return new HashMap<>();
      });
   }

   private Object handleManyWriteCommand(InvocationContext ctx, WriteCommand command) {
      if (command.hasAnyFlag(FlagBitSets.SKIP_INDEXING)) {
         return invokeNext(ctx, command);
      }
      if (ctx.isInTxScope()) {
         Map<Object, Object> oldValues = registerOldValues((TxInvocationContext) ctx);
         for (Object key : command.getAffectedKeys()) {
            CacheEntry entry = ctx.lookupEntry(key);
            if (entry != null && !entry.isChanged() && (entry.getValue() != null || !unreliablePreviousValue(command))) {
               oldValues.putIfAbsent(key, entry.getValue());
            }
         }
         return invokeNext(ctx, command);
      } else {
         Map<Object, Object> oldValues = new HashMap<>();
         for (Object key : command.getAffectedKeys()) {
            CacheEntry entry = ctx.lookupEntry(key);
            if (entry != null && (entry.getValue() != null || !unreliablePreviousValue(command))) {
               oldValues.put(key, entry.getValue());
            }
         }
         return invokeNextThenAccept(ctx, command, (rCtx, cmd, rv) -> {
            if (!cmd.isSuccessful()) {
               return;
            }
            for (Object key : cmd.getAffectedKeys()) {
               CacheEntry entry = rCtx.lookupEntry(key);
               // If the entry is null we are not an owner and won't index it
               if (entry != null && entry.isChanged()) {
                  Object oldValue = oldValues.getOrDefault(key, UNKNOWN);
                  processChange(rCtx, cmd, key, oldValue, entry.getValue());
               }
            }
         });
      }
   }

   private boolean unreliablePreviousValue(WriteCommand command) {
      // alternative approach would be changing the flag and forcing load type in an interceptor before EWI
      return isPersistenceEnabled && (command.loadType() == VisitableCommand.LoadType.DONT_LOAD
            || command.hasAnyFlag(FlagBitSets.SKIP_CACHE_LOAD));
   }

   @Override
   public Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command) {
      return handleDataWriteCommand(ctx, command);
   }

   @Override
   public Object visitRemoveCommand(InvocationContext ctx, RemoveCommand command) {
      return handleDataWriteCommand(ctx, command);
   }

   @Override
   public Object visitReplaceCommand(InvocationContext ctx, ReplaceCommand command) {
      return handleDataWriteCommand(ctx, command);
   }

   @Override
   public Object visitComputeCommand(InvocationContext ctx, ComputeCommand command) {
      return handleDataWriteCommand(ctx, command);
   }

   @Override
   public Object visitComputeIfAbsentCommand(InvocationContext ctx, ComputeIfAbsentCommand command) {
      return handleDataWriteCommand(ctx, command);
   }

   @Override
   public Object visitPutMapCommand(InvocationContext ctx, PutMapCommand command) {
      return handleManyWriteCommand(ctx, command);
   }

   @Override
   public Object visitClearCommand(InvocationContext ctx, ClearCommand command) {
      return invokeNextThenAccept(ctx, command, processClearCommand);
   }

   @Override
   public Object visitReadWriteKeyCommand(InvocationContext ctx, ReadWriteKeyCommand command) {
      return handleDataWriteCommand(ctx, command);
   }

   @Override
   public Object visitWriteOnlyKeyCommand(InvocationContext ctx, WriteOnlyKeyCommand command) {
      return handleDataWriteCommand(ctx, command);
   }

   @Override
   public Object visitReadWriteKeyValueCommand(InvocationContext ctx, ReadWriteKeyValueCommand command) {
      return handleDataWriteCommand(ctx, command);
   }

   @Override
   public Object visitWriteOnlyManyEntriesCommand(InvocationContext ctx, WriteOnlyManyEntriesCommand command) {
      return handleManyWriteCommand(ctx, command);
   }

   @Override
   public Object visitWriteOnlyKeyValueCommand(InvocationContext ctx, WriteOnlyKeyValueCommand command) {
      return handleDataWriteCommand(ctx, command);
   }

   @Override
   public Object visitWriteOnlyManyCommand(InvocationContext ctx, WriteOnlyManyCommand command) {
      return handleManyWriteCommand(ctx, command);
   }

   @Override
   public Object visitReadWriteManyCommand(InvocationContext ctx, ReadWriteManyCommand command) {
      return handleManyWriteCommand(ctx, command);
   }

   @Override
   public Object visitReadWriteManyEntriesCommand(InvocationContext ctx, ReadWriteManyEntriesCommand command) {
      return handleManyWriteCommand(ctx, command);
   }

   /**
    * Remove all entries from all known indexes
    */
   public void purgeAllIndexes() {
      SearchMapping searchMapping = searchMappingHolder.getSearchMapping();
      if (searchMapping == null) {
         return;
      }

      searchMapping.scopeAll().workspace().purge();
   }

   public void purgeIndex(Class<?> entityType) {
      SearchMapping searchMapping = searchMappingHolder.getSearchMapping();
      if (searchMapping == null) {
         return;
      }

      searchMapping.scope(entityType).workspace().purge();
   }

   /**
    * Removes from the index the entries corresponding to the supplied segments, if the index is local.
    */
   void purgeIndex(IntSet segments) {
      if (segments == null || segments.isEmpty()) return;

      SearchMapping searchMapping = searchMappingHolder.getSearchMapping();
      if (searchMapping == null) {
         return;
      }

      Set<String> routingKeys = segments.intStream().boxed().map(Objects::toString).collect(Collectors.toSet());
      searchMapping.scopeAll().workspace().purge(routingKeys);
   }

   /**
    * Remove entries from all indexes by key.
    */
   void removeFromIndexes(Object key, int segment) {
      Futures.unwrappedExceptionJoin(getSearchIndexer().purge(keyToString(key, segment), segment+""));
   }

   // Method that will be called when data needs to be removed from Lucene.
   private void removeFromIndexes(Object value, Object key, int segment) {
      Futures.unwrappedExceptionJoin(getSearchIndexer().delete(keyToString(key, segment), value));
   }

   private void updateIndexes(boolean usingSkipIndexCleanupFlag, Object value, Object key, int segment) {
      // Note: it's generally unsafe to assume there is no previous entry to cleanup: always use UPDATE
      // unless the specific flag is allowing this.
      if (usingSkipIndexCleanupFlag) {
         Futures.unwrappedExceptionJoin(getSearchIndexer().add(keyToString(key, segment), value));
      } else {
         Futures.unwrappedExceptionJoin(getSearchIndexer().addOrUpdate(keyToString(key, segment), value));
      }
   }

   /**
    * The indexed classes.
    *
    * @deprecated since 11
    */
   @Deprecated
   public Map<String, Class<?>> indexedEntities() {
      return indexedClasses;
   }

   private SearchIndexer getSearchIndexer() {
      return searchMappingHolder.getSearchMapping().getSearchIndexer();
   }

   private Object extractValue(Object storedValue) {
      return valueDataConversion.extractIndexable(storedValue);
   }

   private Object extractKey(Object storedKey) {
      return keyDataConversion.extractIndexable(storedKey);
   }

   private String keyToString(Object key, int segment) {
      return keyTransformationHandler.keyToString(key, segment);
   }

   public KeyTransformationHandler getKeyTransformationHandler() {
      return keyTransformationHandler;
   }

   void processChange(InvocationContext ctx, FlagAffectedCommand command, Object storedKey, Object storedOldValue, Object storedNewValue) {
      int segment = SegmentSpecificCommand.extractSegment(command, storedKey, keyPartitioner);
      Object key = extractKey(storedKey);
      Object oldValue = storedOldValue == UNKNOWN ? UNKNOWN : extractValue(storedOldValue);
      Object newValue = extractValue(storedNewValue);
      boolean skipIndexCleanup = command != null && command.hasAnyFlag(FlagBitSets.SKIP_INDEX_CLEANUP);
      if (!skipIndexCleanup) {
         if (oldValue == UNKNOWN) {
            if (shouldModifyIndexes(command, ctx, storedKey)) {
               removeFromIndexes(key, segment);
            }
         } else if (searchMappingHolder.isIndexedType(oldValue) && (newValue == null || shouldRemove(newValue, oldValue))
               && shouldModifyIndexes(command, ctx, storedKey)) {
            removeFromIndexes(oldValue, key, segment);
         } else if (trace) {
            log.tracef("Index cleanup not needed for %s -> %s", oldValue, newValue);
         }
      } else if (trace) {
         log.tracef("Skipped index cleanup for command %s", command);
      }
      if (searchMappingHolder.isIndexedType(newValue)) {
         if (shouldModifyIndexes(command, ctx, storedKey)) {
            // This means that the entry is just modified so we need to update the indexes and not add to them.
            updateIndexes(skipIndexCleanup, newValue, key, segment);
         } else {
            if (trace) {
               log.tracef("Not modifying index for %s (%s)", storedKey, command);
            }
         }
      } else if (trace) {
         log.tracef("Update not needed for %s", newValue);
      }
   }

   private boolean shouldRemove(Object value, Object previousValue) {
      return value != null && previousValue != null && value.getClass() != previousValue.getClass();
   }

   private void processClearCommand(InvocationContext ctx, ClearCommand command, Object rv) {
      if (shouldModifyIndexes(command, ctx, null)) {
         purgeAllIndexes();
      }
   }

   public boolean isStopping() {
      return stopping.get();
   }
}
