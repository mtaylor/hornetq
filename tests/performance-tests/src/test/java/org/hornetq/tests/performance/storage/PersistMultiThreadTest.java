/*
 * Copyright 2005-2014 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.hornetq.tests.performance.storage;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.hornetq.api.core.SimpleString;
import org.hornetq.core.paging.PagingManager;
import org.hornetq.core.paging.PagingStore;
import org.hornetq.core.paging.cursor.PageCursorProvider;
import org.hornetq.core.paging.impl.Page;
import org.hornetq.core.persistence.OperationContext;
import org.hornetq.core.persistence.StorageManager;
import org.hornetq.core.replication.ReplicationManager;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.JournalType;
import org.hornetq.core.server.RouteContextList;
import org.hornetq.core.server.ServerMessage;
import org.hornetq.core.server.impl.ServerMessageImpl;
import org.hornetq.core.settings.impl.AddressFullMessagePolicy;
import org.hornetq.core.settings.impl.AddressSettings;
import org.hornetq.core.transaction.Transaction;
import org.hornetq.tests.util.ServiceTestBase;
import org.junit.Assert;
import org.junit.Test;

public class PersistMultiThreadTest extends ServiceTestBase {

   FakePagingStore fakePagingStore = new FakePagingStore();

   @Test
   public void testMultipleWrites() throws Exception {
      HornetQServer server = createServer(true);
      server.getConfiguration().setJournalFileSize(10 * 1024 * 1024);
      server.getConfiguration().setJournalMinFiles(2);
      server.getConfiguration().setJournalType(JournalType.ASYNCIO);
      server.getConfiguration().setJournalDirectory("/home/mtaylor/tmp/journal/" + UUID.randomUUID().toString());
      server.getConfiguration().setCreateJournalDir(true);

      server.start();

      StorageManager storage = server.getStorageManager();

      long msgID = storage.generateUniqueID();
      System.out.println("msgID=" + msgID);

      int NUMBER_OF_THREADS = 50;
      int NUMBER_OF_MESSAGES = 5000;

      MyThread[] threads = new MyThread[NUMBER_OF_THREADS];

      final CountDownLatch alignFlag = new CountDownLatch(NUMBER_OF_THREADS);
      final CountDownLatch startFlag = new CountDownLatch(1);
      final CountDownLatch finishFlag = new CountDownLatch(NUMBER_OF_THREADS);

      for (int i = 0; i < threads.length; i++) {
         threads[i] = new MyThread("writer::" + i, storage, NUMBER_OF_MESSAGES, alignFlag, startFlag, finishFlag);
      }

      for (MyThread t : threads) {
         t.start();
      }

      alignFlag.await();

      long startTime = System.currentTimeMillis();
      startFlag.countDown();

      // I'm using a countDown to avoid measuring time spent on thread context from join.
      // i.e. i want to measure as soon as the loops are done
      finishFlag.await();
      long endtime = System.currentTimeMillis();


      for (MyThread t : threads) {
         t.join();
         Assert.assertEquals(0, t.errors.get());
      }

      System.out.println("Time:: " + (endtime - startTime));

   }

   class MyThread extends Thread
   {

      final StorageManager storage;
      final int numberOfMessages;
      final AtomicInteger errors = new AtomicInteger(0);

      final CountDownLatch align;
      final CountDownLatch start;
      final CountDownLatch finish;

      MyThread(String name, StorageManager storage, int numberOfMessages, CountDownLatch align, CountDownLatch start, CountDownLatch finish)
      {
         super(name);
         this.storage = storage;
         this.numberOfMessages = numberOfMessages;
         this.align = align;
         this.start = start;
         this.finish = finish;
      }

      public void run() {
         try {
            align.countDown();
            start.await();

            OperationContext ctx = storage.getContext();

            for (int i = 0; i < numberOfMessages; i++) {


               long txID = storage.generateUniqueID();

               long messageID[] = new long[10];

               for (int msgI = 0; msgI < 10; msgI++) {
                  long id = storage.generateUniqueID();

                  messageID[msgI] = id;

                  ServerMessage message = new ServerMessageImpl(id, 10 * 1024);
                  message.setPagingStore(fakePagingStore);

                  message.getBodyBuffer().writeBytes(new byte[104]);
                  message.putStringProperty("hello", "hello1");

                  storage.storeMessageTransactional(txID, message);
                  storage.storeReferenceTransactional(txID, 1, id);

                  message.decrementRefCount();
               }

               storage.commit(txID);
               ctx.waitCompletion();

               for (long id : messageID) {
                  storage.storeAcknowledge(1, id);
               }

               ctx.waitCompletion();

               for (long id : messageID) {
                  storage.storeAcknowledge(1, id);
               }

               ctx.waitCompletion();

               for (long id : messageID) {
                  storage.deleteMessage(id);
               }

               ctx.waitCompletion();

            }
         }
         catch (Exception e) {
            e.printStackTrace();
            errors.incrementAndGet();
         }
         finally {
            finish.countDown();
         }

      }

   }

   class FakePagingStore implements PagingStore {

      @Override
      public SimpleString getAddress() {
         return null;
      }

      @Override
      public int getNumberOfPages() {
         return 0;
      }

      @Override
      public int getCurrentWritingPage() {
         return 0;
      }

      @Override
      public SimpleString getStoreName() {
         return null;
      }

      @Override
      public String getFolder() {
         return null;
      }

      @Override
      public AddressFullMessagePolicy getAddressFullMessagePolicy() {
         return null;
      }

      @Override
      public long getFirstPage() {
         return 0;
      }

      @Override
      public long getPageSizeBytes() {
         return 0;
      }

      @Override
      public long getAddressSize() {
         return 0;
      }

      @Override
      public long getMaxSize() {
         return 0;
      }

      @Override
      public void applySetting(AddressSettings addressSettings) {

      }

      @Override
      public boolean isPaging() {
         return false;
      }

      @Override
      public void sync() throws Exception {

      }

      @Override
      public void ioSync() throws Exception {

      }

      @Override
      public boolean page(ServerMessage message,
                          Transaction tx,
                          RouteContextList listCtx,
                          ReentrantReadWriteLock.ReadLock readLock) throws Exception {
         return false;
      }

      @Override
      public Page createPage(int page) throws Exception {
         return null;
      }

      @Override
      public boolean checkPageFileExists(int page) throws Exception {
         return false;
      }

      @Override
      public PagingManager getPagingManager() {
         return null;
      }

      @Override
      public PageCursorProvider getCursorProvider() {
         return null;
      }

      @Override
      public void processReload() throws Exception {

      }

      @Override
      public Page depage() throws Exception {
         return null;
      }

      @Override
      public void forceAnotherPage() throws Exception {

      }

      @Override
      public Page getCurrentPage() {
         return null;
      }

      @Override
      public boolean startPaging() throws Exception {
         return false;
      }

      @Override
      public void stopPaging() throws Exception {

      }

      @Override
      public void addSize(int size) {

      }

      @Override
      public boolean checkMemory(Runnable runnable) {
         return false;
      }

      @Override
      public boolean lock(long timeout) {
         return false;
      }

      @Override
      public void unlock() {

      }

      @Override
      public void flushExecutors() {

      }

      @Override
      public Collection<Integer> getCurrentIds() throws Exception {
         return null;
      }

      @Override
      public void sendPages(ReplicationManager replicator, Collection<Integer> pageIds) throws Exception {

      }

      @Override
      public void disableCleanup() {

      }

      @Override
      public void enableCleanup() {

      }

      @Override
      public void start() throws Exception {

      }

      @Override
      public void stop() throws Exception {

      }

      @Override
      public boolean isStarted() {
         return false;
      }
   }
}
