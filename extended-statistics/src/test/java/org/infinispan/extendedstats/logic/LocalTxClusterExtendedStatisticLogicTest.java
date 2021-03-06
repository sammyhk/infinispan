package org.infinispan.extendedstats.logic;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.infinispan.extendedstats.CacheStatisticCollector.convertNanosToMicro;
import static org.infinispan.extendedstats.CacheStatisticCollector.convertNanosToSeconds;
import static org.infinispan.extendedstats.container.ExtendedStatistic.ABORT_RATE;
import static org.infinispan.extendedstats.container.ExtendedStatistic.ALL_GET_EXECUTION;
import static org.infinispan.extendedstats.container.ExtendedStatistic.ARRIVAL_RATE;
import static org.infinispan.extendedstats.container.ExtendedStatistic.ASYNC_COMPLETE_NOTIFY_TIME;
import static org.infinispan.extendedstats.container.ExtendedStatistic.CLUSTERED_GET_COMMAND_SIZE;
import static org.infinispan.extendedstats.container.ExtendedStatistic.COMMIT_COMMAND_SIZE;
import static org.infinispan.extendedstats.container.ExtendedStatistic.COMMIT_EXECUTION_TIME;
import static org.infinispan.extendedstats.container.ExtendedStatistic.LOCAL_COMMIT_EXECUTION_TIME;
import static org.infinispan.extendedstats.container.ExtendedStatistic.LOCAL_EXEC_NO_CONT;
import static org.infinispan.extendedstats.container.ExtendedStatistic.LOCAL_GET_EXECUTION;
import static org.infinispan.extendedstats.container.ExtendedStatistic.LOCAL_PREPARE_EXECUTION_TIME;
import static org.infinispan.extendedstats.container.ExtendedStatistic.LOCAL_PUT_EXECUTION;
import static org.infinispan.extendedstats.container.ExtendedStatistic.LOCAL_ROLLBACK_EXECUTION_TIME;
import static org.infinispan.extendedstats.container.ExtendedStatistic.LOCK_HOLD_TIME;
import static org.infinispan.extendedstats.container.ExtendedStatistic.LOCK_HOLD_TIME_LOCAL;
import static org.infinispan.extendedstats.container.ExtendedStatistic.LOCK_HOLD_TIME_REMOTE;
import static org.infinispan.extendedstats.container.ExtendedStatistic.LOCK_HOLD_TIME_SUCCESS_LOCAL_TX;
import static org.infinispan.extendedstats.container.ExtendedStatistic.LOCK_WAITING_TIME;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_ABORTED_RO_TX;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_ABORTED_WR_TX;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_ASYNC_COMPLETE_NOTIFY;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_COMMITTED_RO_TX;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_COMMITTED_TX;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_COMMITTED_WR_TX;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_COMMIT_COMMAND;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_GET;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_GETS_RO_TX;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_GETS_WR_TX;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_HELD_LOCKS;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_HELD_LOCKS_SUCCESS_LOCAL_TX;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_LOCAL_COMMITTED_TX;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_LOCK_FAILED_DEADLOCK;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_LOCK_FAILED_TIMEOUT;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_LOCK_PER_LOCAL_TX;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_LOCK_PER_REMOTE_TX;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_NODES_COMMIT;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_NODES_COMPLETE_NOTIFY;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_NODES_GET;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_NODES_PREPARE;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_NODES_ROLLBACK;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_PREPARE_COMMAND;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_PUT;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_PUTS_WR_TX;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_REMOTE_GET;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_REMOTE_GETS_RO_TX;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_REMOTE_GETS_WR_TX;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_REMOTE_PUT;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_REMOTE_PUTS_WR_TX;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_ROLLBACK_COMMAND;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_SYNC_COMMIT;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_SYNC_GET;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_SYNC_PREPARE;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_SYNC_ROLLBACK;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_WAITED_FOR_LOCKS;
import static org.infinispan.extendedstats.container.ExtendedStatistic.NUM_WRITE_SKEW;
import static org.infinispan.extendedstats.container.ExtendedStatistic.PREPARE_COMMAND_SIZE;
import static org.infinispan.extendedstats.container.ExtendedStatistic.PREPARE_EXECUTION_TIME;
import static org.infinispan.extendedstats.container.ExtendedStatistic.REMOTE_COMMIT_EXECUTION_TIME;
import static org.infinispan.extendedstats.container.ExtendedStatistic.REMOTE_GET_EXECUTION;
import static org.infinispan.extendedstats.container.ExtendedStatistic.REMOTE_PREPARE_EXECUTION_TIME;
import static org.infinispan.extendedstats.container.ExtendedStatistic.REMOTE_PUT_EXECUTION;
import static org.infinispan.extendedstats.container.ExtendedStatistic.REMOTE_ROLLBACK_EXECUTION_TIME;
import static org.infinispan.extendedstats.container.ExtendedStatistic.RESPONSE_TIME;
import static org.infinispan.extendedstats.container.ExtendedStatistic.ROLLBACK_EXECUTION_TIME;
import static org.infinispan.extendedstats.container.ExtendedStatistic.RO_TX_ABORTED_EXECUTION_TIME;
import static org.infinispan.extendedstats.container.ExtendedStatistic.RO_TX_SUCCESSFUL_EXECUTION_TIME;
import static org.infinispan.extendedstats.container.ExtendedStatistic.SUCCESSFUL_WRITE_TX_PERCENTAGE;
import static org.infinispan.extendedstats.container.ExtendedStatistic.SYNC_COMMIT_TIME;
import static org.infinispan.extendedstats.container.ExtendedStatistic.SYNC_GET_TIME;
import static org.infinispan.extendedstats.container.ExtendedStatistic.SYNC_PREPARE_TIME;
import static org.infinispan.extendedstats.container.ExtendedStatistic.SYNC_ROLLBACK_TIME;
import static org.infinispan.extendedstats.container.ExtendedStatistic.THROUGHPUT;
import static org.infinispan.extendedstats.container.ExtendedStatistic.WRITE_SKEW_PROBABILITY;
import static org.infinispan.extendedstats.container.ExtendedStatistic.WRITE_TX_PERCENTAGE;
import static org.infinispan.extendedstats.container.ExtendedStatistic.WR_TX_ABORTED_EXECUTION_TIME;
import static org.infinispan.extendedstats.container.ExtendedStatistic.WR_TX_SUCCESSFUL_EXECUTION_TIME;
import static org.infinispan.extendedstats.container.ExtendedStatistic.values;
import static org.infinispan.test.TestingUtil.extractField;
import static org.infinispan.test.TestingUtil.extractLockManager;
import static org.infinispan.test.TestingUtil.replaceField;
import static org.infinispan.test.TestingUtil.sleepThread;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.infinispan.commons.time.TimeService;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.extendedstats.CacheStatisticCollector;
import org.infinispan.extendedstats.CacheStatisticManager;
import org.infinispan.extendedstats.container.ConcurrentGlobalContainer;
import org.infinispan.extendedstats.container.ExtendedStatistic;
import org.infinispan.extendedstats.wrappers.ExtendedStatisticInterceptor;
import org.infinispan.extendedstats.wrappers.ExtendedStatisticLockManager;
import org.infinispan.interceptors.impl.TxInterceptor;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.SingleCacheManagerTest;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.util.EmbeddedTimeService;
import org.infinispan.util.concurrent.IsolationLevel;
import org.testng.annotations.Test;

/**
 * @author Pedro Ruivo
 * @since 6.0
 */
@Test(groups = "functional", testName = "extendedstats.logic.LocalTxClusterExtendedStatisticLogicTest")
public class LocalTxClusterExtendedStatisticLogicTest extends SingleCacheManagerTest {

   private static final int SLEEP_TIME = 500;
   private static final TimeService TEST_TIME_SERVICE = new EmbeddedTimeService() {
      @Override
      public long time() {
         return 0;
      }

      @Override
      public long timeDuration(long startTimeNanos, TimeUnit outputTimeUnit) {
         assertEquals(startTimeNanos, 0, "Start timestamp must be zero!");
         assertEquals(outputTimeUnit, NANOSECONDS, "TimeUnit is different from expected");
         return 1;
      }

      @Override
      public long timeDuration(long startTimeNanos, long endTimeNanos, TimeUnit outputTimeUnit) {
         assertEquals(startTimeNanos, 0, "Start timestamp must be zero!");
         assertEquals(endTimeNanos, 0, "End timestamp must be zero!");
         assertEquals(outputTimeUnit, NANOSECONDS, "TimeUnit is different from expected");
         return 1;
      }
   };
   private static final double MICROSECONDS = convertNanosToMicro(TEST_TIME_SERVICE.timeDuration(0, NANOSECONDS));
   private static final double SECONDS = convertNanosToSeconds(TEST_TIME_SERVICE.timeDuration(0, NANOSECONDS));
   //private final TransactionInterceptor[] transactionInterceptors = new TransactionInterceptor[NUM_NODES];
   private final List<Object> keys = new ArrayList<>(128);
   private ExtendedStatisticInterceptor extendedStatisticInterceptor;

   public final void testPutTxAndReadOnlyTx() throws Exception {
      testStats(WriteOperation.PUT, 2, 7, 3, 4, 5, false);
   }

   public final void testPutTxAndReadOnlyTxRollback() throws Exception {
      testStats(WriteOperation.PUT, 3, 6, 2, 5, 4, true);
   }

   public final void testConditionalPutTxAndReadOnlyTx() throws Exception {
      testStats(WriteOperation.PUT_IF, 4, 5, 4, 6, 3, false);
   }

   public final void testConditionalPutTxAndReadOnlyTxRollback() throws Exception {
      testStats(WriteOperation.PUT_IF, 5, 4, 5, 7, 2, true);
   }

   public final void testReplaceTxAndReadOnlyTx() throws Exception {
      testStats(WriteOperation.REPLACE, 2, 7, 3, 4, 5, false);
   }

   public final void testReplaceTxAndReadOnlyTxRollback() throws Exception {
      testStats(WriteOperation.REPLACE, 3, 6, 2, 5, 4, true);
   }

   public final void testConditionalReplaceTxAndReadOnlyTx() throws Exception {
      testStats(WriteOperation.REPLACE_IF, 4, 5, 4, 6, 3, false);
   }

   public final void testConditionalReplaceTxAndReadOnlyTxRollback() throws Exception {
      testStats(WriteOperation.REPLACE_IF, 5, 4, 5, 7, 2, true);
   }

   public final void testRemoveTxAndReadOnlyTx() throws Exception {
      testStats(WriteOperation.REMOVE, 2, 7, 3, 4, 5, false);
   }

   public final void testRemoveTxAndReadOnlyTxRollback() throws Exception {
      testStats(WriteOperation.REMOVE, 3, 6, 2, 5, 4, true);
   }

   public final void testConditionalRemoveTxAndReadOnlyTx() throws Exception {
      testStats(WriteOperation.REMOVE_IF, 4, 5, 4, 6, 3, false);
   }

   public final void testConditionalRemoveTxAndReadOnlyTxRollback() throws Exception {
      testStats(WriteOperation.REMOVE_IF, 5, 4, 5, 7, 2, true);
   }

   @Override
   protected void setup() throws Exception {
      super.setup();
      CacheStatisticManager manager = extractField(extendedStatisticInterceptor, "cacheStatisticManager");
      CacheStatisticCollector collector = extractField(manager, "cacheStatisticCollector");
      ConcurrentGlobalContainer globalContainer = extractField(collector, "globalContainer");
      replaceField(TEST_TIME_SERVICE, "timeService", manager, CacheStatisticManager.class);
      replaceField(TEST_TIME_SERVICE, "timeService", collector, CacheStatisticCollector.class);
      replaceField(TEST_TIME_SERVICE, "timeService", globalContainer, ConcurrentGlobalContainer.class);
      replaceField(TEST_TIME_SERVICE, "timeService", extendedStatisticInterceptor, ExtendedStatisticInterceptor.class);
      replaceField(TEST_TIME_SERVICE, "timeService", extractLockManager(cache()), ExtendedStatisticLockManager.class);
   }

   @Override
   protected EmbeddedCacheManager createCacheManager() {
      ConfigurationBuilder builder = getDefaultClusteredCacheConfig(CacheMode.LOCAL, true);
      builder.locking().isolationLevel(IsolationLevel.REPEATABLE_READ)
            .lockAcquisitionTimeout(0);
      builder.clustering().hash().numOwners(1);
      builder.transaction().recovery().disable();
      extendedStatisticInterceptor = new ExtendedStatisticInterceptor();
      builder.customInterceptors().addInterceptor().interceptor(extendedStatisticInterceptor)
            .after(TxInterceptor.class);
      return TestCacheManagerFactory.createCacheManager(builder);
   }

   private void testStats(WriteOperation operation, int numOfWriteTx, int numOfWrites, int numOfReadsPerWriteTx,
                          int numOfReadOnlyTx, int numOfReadPerReadTx, boolean abort)
         throws Exception {
      for (int i = 1; i <= (numOfReadsPerWriteTx + numOfWrites) * numOfWriteTx + numOfReadPerReadTx * numOfReadOnlyTx; ++i) {
         cache().put(getKey(i), getInitValue(i));
      }
      sleepThread(SLEEP_TIME);
      resetStats();
      int localGetsReadTx = 0;
      int localGetsWriteTx = 0;
      int localPuts = 0;
      int localLocks = 0;
      int numOfLocalWriteTx = 0;

      int keyIndex = 0;
      //write tx
      for (int tx = 1; tx <= numOfWriteTx; ++tx) {
         tm().begin();
         for (int i = 1; i <= numOfReadsPerWriteTx; ++i) {
            keyIndex++;
            Object key = getKey(keyIndex);
            localGetsWriteTx++;
            assertEquals(cache().get(key), getInitValue(keyIndex));
         }
         for (int i = 1; i <= numOfWrites; ++i) {
            keyIndex++;
            Object key = operation == WriteOperation.PUT_IF ? getKey(-keyIndex) : getKey(keyIndex);
            switch (operation) {
               case PUT:
                  cache().put(key, getValue(keyIndex));
                  break;
               case PUT_IF:
                  cache().putIfAbsent(key, getValue(keyIndex));
                  break;
               case REPLACE:
                  cache().replace(key, getValue(keyIndex));
                  break;
               case REPLACE_IF:
                  cache().replace(key, getInitValue(keyIndex), getValue(keyIndex));
                  break;
               case REMOVE:
                  cache().remove(key);
                  break;
               case REMOVE_IF:
                  cache().remove(key, getInitValue(keyIndex));
                  break;
               default:
                  //nothing
            }
            localPuts++;
            if (!abort) {
               localLocks++;
            }
         }
         numOfLocalWriteTx++;
         if (abort) {
            tm().rollback();
         } else {
            tm().commit();

         }
      }
      sleepThread(SLEEP_TIME);

      //read tx
      for (int tx = 1; tx <= numOfReadOnlyTx; ++tx) {
         tm().begin();
         for (int i = 1; i <= numOfReadPerReadTx; ++i) {
            keyIndex++;
            Object key = getKey(keyIndex);
            localGetsReadTx++;
            assertEquals(cache().get(key), getInitValue(keyIndex));
         }
         if (abort) {
            tm().rollback();
         } else {
            tm().commit();

         }
      }
      sleepThread(SLEEP_TIME);

      EnumSet<ExtendedStatistic> statsToValidate = getStatsToValidate();

      assertTxValues(statsToValidate, numOfLocalWriteTx, numOfReadOnlyTx, abort);
      assertLockingValues(statsToValidate, localLocks, numOfLocalWriteTx, abort);
      assertAccessesValues(statsToValidate, localGetsReadTx, localGetsWriteTx, localPuts, numOfWriteTx, numOfReadOnlyTx, abort);

      assertAttributeValue(NUM_WRITE_SKEW, statsToValidate, 0);
      assertAttributeValue(WRITE_SKEW_PROBABILITY, statsToValidate, 0);

      assertAllStatsValidated(statsToValidate);
      resetStats();
   }

   private Object getKey(int i) {
      if (i < 0) {
         return "KEY_" + i;
      }
      for (int j = keys.size(); j <= i; ++j) {
         keys.add("KEY_" + (j + 1));
      }
      return keys.get(i - 1);
   }

   private Object getInitValue(int i) {
      return "INIT_" + i;
   }

   private Object getValue(int i) {
      return "VALUE_" + i;
   }

   private void assertTxValues(EnumSet<ExtendedStatistic> statsToValidate, int numOfWriteTx,
                               int numOfReadTx, boolean abort) {
      log.infof("Check Tx value: writeTx=%s, readTx=%s, abort?=%s", numOfWriteTx, numOfReadTx, abort);
      if (abort) {
         assertAttributeValue(NUM_COMMITTED_RO_TX, statsToValidate, 0); //not exposed via JMX
         assertAttributeValue(NUM_COMMITTED_WR_TX, statsToValidate, 0); //not exposed via JMX
         assertAttributeValue(NUM_ABORTED_WR_TX, statsToValidate, numOfWriteTx); //not exposed via JMX
         assertAttributeValue(NUM_ABORTED_RO_TX, statsToValidate, numOfReadTx); //not exposed via JMX
         assertAttributeValue(NUM_COMMITTED_TX, statsToValidate, 0);
         assertAttributeValue(NUM_LOCAL_COMMITTED_TX, statsToValidate, 0);
         assertAttributeValue(LOCAL_EXEC_NO_CONT, statsToValidate, 0);
         assertAttributeValue(WRITE_TX_PERCENTAGE, statsToValidate, numOfWriteTx * 1.0 / (numOfWriteTx + numOfReadTx));
         assertAttributeValue(SUCCESSFUL_WRITE_TX_PERCENTAGE, statsToValidate, 0);
         assertAttributeValue(WR_TX_ABORTED_EXECUTION_TIME, statsToValidate, numOfWriteTx != 0 ? MICROSECONDS : 0);
         assertAttributeValue(RO_TX_ABORTED_EXECUTION_TIME, statsToValidate, numOfReadTx); //not exposed via JMX
         assertAttributeValue(WR_TX_SUCCESSFUL_EXECUTION_TIME, statsToValidate, 0);
         assertAttributeValue(RO_TX_SUCCESSFUL_EXECUTION_TIME, statsToValidate, 0);
         assertAttributeValue(ABORT_RATE, statsToValidate, 1);
         assertAttributeValue(ARRIVAL_RATE, statsToValidate, (numOfWriteTx + numOfReadTx) / SECONDS);
         assertAttributeValue(THROUGHPUT, statsToValidate, 0);
         assertAttributeValue(ROLLBACK_EXECUTION_TIME, statsToValidate, numOfReadTx != 0 || numOfWriteTx != 0 ? MICROSECONDS : 0);
         assertAttributeValue(NUM_ROLLBACK_COMMAND, statsToValidate, numOfReadTx + numOfWriteTx);
         assertAttributeValue(LOCAL_ROLLBACK_EXECUTION_TIME, statsToValidate, numOfReadTx != 0 || numOfWriteTx != 0 ? MICROSECONDS : 0);
         assertAttributeValue(REMOTE_ROLLBACK_EXECUTION_TIME, statsToValidate, 0);
         assertAttributeValue(COMMIT_EXECUTION_TIME, statsToValidate, 0);
         assertAttributeValue(NUM_COMMIT_COMMAND, statsToValidate, 0);
         assertAttributeValue(LOCAL_COMMIT_EXECUTION_TIME, statsToValidate, 0);
         assertAttributeValue(REMOTE_COMMIT_EXECUTION_TIME, statsToValidate, 0);
         assertAttributeValue(PREPARE_EXECUTION_TIME, statsToValidate, 0); // //not exposed via JMX
         assertAttributeValue(NUM_PREPARE_COMMAND, statsToValidate, 0);
         assertAttributeValue(LOCAL_PREPARE_EXECUTION_TIME, statsToValidate, 0);
         assertAttributeValue(REMOTE_PREPARE_EXECUTION_TIME, statsToValidate, 0);
         assertAttributeValue(NUM_SYNC_PREPARE, statsToValidate, 0);
         assertAttributeValue(SYNC_PREPARE_TIME, statsToValidate, 0);
         assertAttributeValue(NUM_SYNC_COMMIT, statsToValidate, 0);
         assertAttributeValue(SYNC_COMMIT_TIME, statsToValidate, 0);
         assertAttributeValue(NUM_SYNC_ROLLBACK, statsToValidate, 0);
         assertAttributeValue(SYNC_ROLLBACK_TIME, statsToValidate, 0);
         assertAttributeValue(ASYNC_COMPLETE_NOTIFY_TIME, statsToValidate, 0);
         assertAttributeValue(NUM_ASYNC_COMPLETE_NOTIFY, statsToValidate, 0);
         assertAttributeValue(NUM_NODES_PREPARE, statsToValidate, 0);
         assertAttributeValue(NUM_NODES_COMMIT, statsToValidate, 0);
         assertAttributeValue(NUM_NODES_ROLLBACK, statsToValidate, 0);
         assertAttributeValue(NUM_NODES_COMPLETE_NOTIFY, statsToValidate, 0);
         assertAttributeValue(RESPONSE_TIME, statsToValidate, 0);
      } else {
         assertAttributeValue(NUM_COMMITTED_RO_TX, statsToValidate, numOfReadTx); //not exposed via JMX
         assertAttributeValue(NUM_COMMITTED_WR_TX, statsToValidate, numOfWriteTx); //not exposed via JMX
         assertAttributeValue(NUM_ABORTED_WR_TX, statsToValidate, 0); //not exposed via JMX
         assertAttributeValue(NUM_ABORTED_RO_TX, statsToValidate, 0); //not exposed via JMX
         assertAttributeValue(NUM_COMMITTED_TX, statsToValidate, numOfWriteTx + numOfReadTx);
         assertAttributeValue(NUM_LOCAL_COMMITTED_TX, statsToValidate, numOfReadTx + numOfWriteTx);
         assertAttributeValue(LOCAL_EXEC_NO_CONT, statsToValidate, numOfWriteTx != 0 ? MICROSECONDS : 0);
         assertAttributeValue(WRITE_TX_PERCENTAGE, statsToValidate, (numOfWriteTx * 1.0) / (numOfReadTx + numOfWriteTx));
         assertAttributeValue(SUCCESSFUL_WRITE_TX_PERCENTAGE, statsToValidate, (numOfReadTx + numOfWriteTx) > 0 ? (numOfWriteTx * 1.0) / (numOfReadTx + numOfWriteTx) : 0);
         assertAttributeValue(WR_TX_ABORTED_EXECUTION_TIME, statsToValidate, 0);
         assertAttributeValue(RO_TX_ABORTED_EXECUTION_TIME, statsToValidate, 0); //not exposed via JMX
         assertAttributeValue(WR_TX_SUCCESSFUL_EXECUTION_TIME, statsToValidate, numOfWriteTx != 0 ? MICROSECONDS : 0);
         assertAttributeValue(RO_TX_SUCCESSFUL_EXECUTION_TIME, statsToValidate, numOfReadTx != 0 ? MICROSECONDS : 0);
         assertAttributeValue(ABORT_RATE, statsToValidate, 0);
         assertAttributeValue(ARRIVAL_RATE, statsToValidate, (numOfWriteTx + numOfReadTx) / SECONDS);
         assertAttributeValue(THROUGHPUT, statsToValidate, (numOfWriteTx + numOfReadTx) / SECONDS);
         assertAttributeValue(ROLLBACK_EXECUTION_TIME, statsToValidate, 0);
         assertAttributeValue(NUM_ROLLBACK_COMMAND, statsToValidate, 0);
         assertAttributeValue(LOCAL_ROLLBACK_EXECUTION_TIME, statsToValidate, 0);
         assertAttributeValue(REMOTE_ROLLBACK_EXECUTION_TIME, statsToValidate, 0);
         assertAttributeValue(COMMIT_EXECUTION_TIME, statsToValidate, (numOfReadTx != 0 || numOfWriteTx != 0) ? MICROSECONDS : 0);
         assertAttributeValue(NUM_COMMIT_COMMAND, statsToValidate, numOfReadTx + numOfWriteTx);
         assertAttributeValue(LOCAL_COMMIT_EXECUTION_TIME, statsToValidate, (numOfReadTx != 0 || numOfWriteTx != 0) ? MICROSECONDS : 0);
         assertAttributeValue(REMOTE_COMMIT_EXECUTION_TIME, statsToValidate, 0);
         assertAttributeValue(PREPARE_EXECUTION_TIME, statsToValidate, numOfReadTx + numOfWriteTx); // //not exposed via JMX
         assertAttributeValue(NUM_PREPARE_COMMAND, statsToValidate, numOfReadTx + numOfWriteTx);
         assertAttributeValue(LOCAL_PREPARE_EXECUTION_TIME, statsToValidate, (numOfReadTx != 0 || numOfWriteTx != 0) ? MICROSECONDS : 0);
         assertAttributeValue(REMOTE_PREPARE_EXECUTION_TIME, statsToValidate, 0);
         assertAttributeValue(NUM_SYNC_PREPARE, statsToValidate, 0);
         assertAttributeValue(SYNC_PREPARE_TIME, statsToValidate, 0);
         assertAttributeValue(NUM_SYNC_COMMIT, statsToValidate, 0);
         assertAttributeValue(SYNC_COMMIT_TIME, statsToValidate, 0);
         assertAttributeValue(NUM_SYNC_ROLLBACK, statsToValidate, 0);
         assertAttributeValue(SYNC_ROLLBACK_TIME, statsToValidate, 0);
         assertAttributeValue(ASYNC_COMPLETE_NOTIFY_TIME, statsToValidate, 0);
         assertAttributeValue(NUM_ASYNC_COMPLETE_NOTIFY, statsToValidate, 0);
         assertAttributeValue(NUM_NODES_PREPARE, statsToValidate, 0);
         assertAttributeValue(NUM_NODES_COMMIT, statsToValidate, 0);
         assertAttributeValue(NUM_NODES_ROLLBACK, statsToValidate, 0);
         assertAttributeValue(NUM_NODES_COMPLETE_NOTIFY, statsToValidate, 0);
         assertAttributeValue(RESPONSE_TIME, statsToValidate, numOfReadTx != 0 || numOfWriteTx != 0 ? MICROSECONDS : 0);
      }
   }

   private void assertLockingValues(EnumSet<ExtendedStatistic> statsToValidate, int numOfLocks,
                                    int numOfWriteTx, boolean abort) {
      log.infof("Check Locking value. locks=%s, writeTx=%s, abort?=%s", numOfLocks, numOfWriteTx, abort);
      //remote puts always acquire locks
      assertAttributeValue(LOCK_HOLD_TIME_LOCAL, statsToValidate, numOfLocks != 0 ? MICROSECONDS : 0);
      assertAttributeValue(LOCK_HOLD_TIME_REMOTE, statsToValidate, 0);
      assertAttributeValue(NUM_LOCK_PER_LOCAL_TX, statsToValidate, numOfWriteTx != 0 ? numOfLocks * 1.0 / numOfWriteTx : 0);
      assertAttributeValue(NUM_LOCK_PER_REMOTE_TX, statsToValidate, 0);
      assertAttributeValue(LOCK_HOLD_TIME_SUCCESS_LOCAL_TX, statsToValidate, 0);
      assertAttributeValue(NUM_HELD_LOCKS_SUCCESS_LOCAL_TX, statsToValidate, !abort && numOfWriteTx != 0 ? numOfLocks * 1.0 / numOfWriteTx : 0);
      assertAttributeValue(LOCK_HOLD_TIME, statsToValidate, numOfLocks != 0 ? MICROSECONDS : 0);
      assertAttributeValue(NUM_HELD_LOCKS, statsToValidate, numOfLocks);
      assertAttributeValue(NUM_WAITED_FOR_LOCKS, statsToValidate, 0);
      assertAttributeValue(LOCK_WAITING_TIME, statsToValidate, 0);
      assertAttributeValue(NUM_LOCK_FAILED_TIMEOUT, statsToValidate, 0);
      assertAttributeValue(NUM_LOCK_FAILED_DEADLOCK, statsToValidate, 0);
   }

   private void assertAccessesValues(EnumSet<ExtendedStatistic> statsToValidate, int getsReadTx, int getsWriteTx, int puts,
                                     int numOfWriteTx, int numOfReadTx, boolean abort) {
      log.infof("Check accesses values. getsReadTx=%s, getsWriteTx=%s, puts=%s, writeTx=%s, readTx=%s, abort?=%s",
                getsReadTx, getsWriteTx, puts, numOfWriteTx, numOfReadTx, abort);
      assertAttributeValue(NUM_REMOTE_PUT, statsToValidate, 0);
      assertAttributeValue(REMOTE_PUT_EXECUTION, statsToValidate, 0);
      assertAttributeValue(LOCAL_PUT_EXECUTION, statsToValidate, 0);
      assertAttributeValue(NUM_PUT, statsToValidate, puts);
      assertAttributeValue(NUM_PUTS_WR_TX, statsToValidate, !abort && numOfWriteTx != 0 ? puts * 1.0 / numOfWriteTx : 0);
      assertAttributeValue(NUM_REMOTE_PUTS_WR_TX, statsToValidate, 0);

      assertAttributeValue(NUM_REMOTE_GET, statsToValidate, 0);
      assertAttributeValue(NUM_GET, statsToValidate, getsReadTx + getsWriteTx);
      assertAttributeValue(NUM_GETS_RO_TX, statsToValidate, !abort && numOfReadTx != 0 ? getsReadTx * 1.0 / numOfReadTx : 0);
      assertAttributeValue(NUM_GETS_WR_TX, statsToValidate, !abort && numOfWriteTx != 0 ? getsWriteTx * 1.0 / numOfWriteTx : 0);
      assertAttributeValue(NUM_REMOTE_GETS_WR_TX, statsToValidate, 0);
      assertAttributeValue(NUM_REMOTE_GETS_RO_TX, statsToValidate, 0);
      assertAttributeValue(ALL_GET_EXECUTION, statsToValidate, getsReadTx + getsWriteTx);
      //always zero because the all get execution and the rtt is always 1 (all get execution - rtt == 0)
      assertAttributeValue(LOCAL_GET_EXECUTION, statsToValidate, getsReadTx != 0 || getsWriteTx != 0 ? MICROSECONDS : 0);
      assertAttributeValue(REMOTE_GET_EXECUTION, statsToValidate, 0);
      assertAttributeValue(NUM_SYNC_GET, statsToValidate, 0);
      assertAttributeValue(SYNC_GET_TIME, statsToValidate, 0);
      assertAttributeValue(NUM_NODES_GET, statsToValidate, 0);
   }

   private void resetStats() {
      extendedStatisticInterceptor.resetStatistics();
      for (ExtendedStatistic extendedStatistic : values()) {
         assertEquals(extendedStatisticInterceptor.getAttribute(extendedStatistic), 0.0, "Attribute " + extendedStatistic +
               " is not zero after reset");
      }

   }

   private void assertAttributeValue(ExtendedStatistic attr, EnumSet<ExtendedStatistic> statsToValidate,
                                     double txExecutorValue) {
      assertTrue(statsToValidate.contains(attr), "Attribute " + attr + " already validated");
      assertEquals(extendedStatisticInterceptor.getAttribute(attr), txExecutorValue, "Attribute " + attr +
            " has wrong value for cache.");
      statsToValidate.remove(attr);
   }

   private EnumSet<ExtendedStatistic> getStatsToValidate() {
      EnumSet<ExtendedStatistic> statsToValidate = EnumSet.allOf(ExtendedStatistic.class);
      //TODO fix this
      statsToValidate.removeAll(EnumSet.of(PREPARE_COMMAND_SIZE, COMMIT_COMMAND_SIZE,
                                           CLUSTERED_GET_COMMAND_SIZE));
      return statsToValidate;
   }

   private void assertAllStatsValidated(EnumSet<ExtendedStatistic> statsToValidate) {
      assertTrue(statsToValidate.isEmpty(), "Stats not validated: " + statsToValidate + ".");
   }

   private enum WriteOperation {
      PUT,
      PUT_IF,
      REPLACE,
      REPLACE_IF,
      REMOVE,
      REMOVE_IF
   }
}
