package org.hornetq.tests.integration.aerogear;


import org.hornetq.api.core.Message;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.SendAcknowledgementHandler;
import org.hornetq.api.core.client.ServerLocator;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.ConnectorServiceConfiguration;
import org.hornetq.core.config.CoreQueueConfiguration;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.integration.aerogear.AeroGearConnectorServiceFactory;
import org.hornetq.integration.aerogear.AeroGearConstants;
import org.hornetq.tests.util.ServiceTestBase;
import org.hornetq.tests.util.UnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AeroGearBasicServerTest extends ServiceTestBase
{

   private HornetQServer server;
   private ServerLocator locator;

   @Override
   @Before
   public void setUp() throws Exception
   {
      super.setUp();
      Configuration configuration = createDefaultConfig();
      HashMap<String, Object> params = new HashMap<String, Object>();
      params.put(AeroGearConstants.QUEUE_NAME, "testQueue");
      params.put(AeroGearConstants.ENDPOINT_NAME, "http://aerogear-mtaylor.rhcloud.com");
      params.put(AeroGearConstants.APPLICATION_ID_NAME, "9d646a12-e601-4452-9e05-efb0fccdfd08") ;
      params.put(AeroGearConstants.APPLICATION_MASTER_SECRET_NAME, "ed75f17e-cf3c-4c9b-a503-865d91d60d40");
      configuration.getConnectorServiceConfigurations().add(
            new ConnectorServiceConfiguration(AeroGearConnectorServiceFactory.class.getName(), params, "TestAeroGearService"));

      configuration.getQueueConfigurations().add(new CoreQueueConfiguration("testQueue", "testQueue", null, true));
      server = createServer(configuration);
      server.start();
   }

   @Override
   @After
   public void tearDown() throws Exception
   {
      if(locator != null)
      {
         locator.close();
      }
      if(server != null)
      {
         server.stop();
      }
      super.tearDown();
   }

   @Test
   public void aerogearSimpleReceiveTest() throws Exception
   {
      TransportConfiguration tpconf = new TransportConfiguration(UnitTestCase.INVM_CONNECTOR_FACTORY);
      locator = HornetQClient.createServerLocatorWithoutHA(tpconf);
      ClientSessionFactory sf = createSessionFactory(locator);
      ClientSession session = sf.createSession(false, true, true);
      ClientProducer producer = session.createProducer("testQueue");
      final CountDownLatch latch = new CountDownLatch(2);
      ClientMessage m = session.createMessage(true);
      m.getBodyBuffer().writeString("hello from HornetQ!");

      producer.send(m, new SendAcknowledgementHandler()
      {
         @Override
         public void sendAcknowledged(Message message)
         {
            latch.countDown();
         }
      });
      m = session.createMessage(true);
      m.getBodyBuffer().writeString("another hello from HornetQ!");

      producer.send(m, new SendAcknowledgementHandler()
      {
         @Override
         public void sendAcknowledged(Message message)
         {
            latch.countDown();
         }
      });
      assertTrue(latch.await(5, TimeUnit.SECONDS));
      ClientMessage message = session.createConsumer("testQueue").receiveImmediate();
      assertNull(message);
   }
}
