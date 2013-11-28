package org.hornetq.integration.aerogear;

import org.hornetq.api.core.SimpleString;
import org.hornetq.core.filter.Filter;
import org.hornetq.core.postoffice.Binding;
import org.hornetq.core.postoffice.PostOffice;
import org.hornetq.core.server.ConnectorService;
import org.hornetq.core.server.Consumer;
import org.hornetq.core.server.HandleStatus;
import org.hornetq.core.server.MessageReference;
import org.hornetq.core.server.Queue;
import org.hornetq.utils.ConfigurationHelper;

import java.util.List;
import java.util.Map;

public class AeroGearConnectorService implements ConnectorService, Consumer
{
   private final String connectorName;
   private final PostOffice postOffice;
   private final String queueName;
   private Queue queue;

   public AeroGearConnectorService(String connectorName, Map<String, Object> configuration, PostOffice postOffice)
   {
      this.connectorName = connectorName;
      this.postOffice = postOffice;
      this.queueName = ConfigurationHelper.getStringProperty(AeroGearConstants.QUEUE_NAME, null, configuration);
   }

   @Override
   public String getName()
   {
      return connectorName;
   }

   @Override
   public void start() throws Exception
   {
      Binding b = postOffice.getBinding(new SimpleString(queueName));
      if (b == null)
      {
         throw new Exception(connectorName + ": queue " + queueName + " not found");
      }

      queue = (Queue)b.getBindable();

      queue.addConsumer(this);
   }

   @Override
   public void stop() throws Exception
   {
      queue.removeConsumer(this);
   }

   @Override
   public boolean isStarted()
   {
      return false;
   }

   @Override
   public HandleStatus handle(MessageReference reference) throws Exception
   {
      return HandleStatus.HANDLED;
   }

   @Override
   public void proceedDeliver(MessageReference reference) throws Exception
   {
   }

   @Override
   public Filter getFilter()
   {
      return null;
   }

   @Override
   public void getDeliveringMessages(List<MessageReference> refList)
   {
   }

   @Override
   public String debug()
   {
      return null;
   }

   @Override
   public String toManagementString()
   {
      return null;
   }

   @Override
   public void disconnect()
   {
   }
}
