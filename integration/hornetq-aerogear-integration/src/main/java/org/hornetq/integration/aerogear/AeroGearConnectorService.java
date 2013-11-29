package org.hornetq.integration.aerogear;

import org.hornetq.api.core.SimpleString;
import org.hornetq.core.filter.Filter;
import org.hornetq.core.filter.impl.FilterImpl;
import org.hornetq.core.postoffice.Binding;
import org.hornetq.core.postoffice.PostOffice;
import org.hornetq.core.server.ConnectorService;
import org.hornetq.core.server.Consumer;
import org.hornetq.core.server.HandleStatus;
import org.hornetq.core.server.HornetQServerLogger;
import org.hornetq.core.server.MessageReference;
import org.hornetq.core.server.Queue;
import org.hornetq.core.server.ServerMessage;
import org.hornetq.utils.ConfigurationHelper;
import org.jboss.aerogear.unifiedpush.JavaSender;
import org.jboss.aerogear.unifiedpush.SenderClient;
import org.jboss.aerogear.unifiedpush.message.MessageResponseCallback;
import org.jboss.aerogear.unifiedpush.message.UnifiedMessage;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class AeroGearConnectorService implements ConnectorService, Consumer
{
   private final String connectorName;
   private final PostOffice postOffice;
   private final String queueName;
   private final String endpoint;
   private final String applicationId;
   private final String applicationMasterSecret;
   private final int ttl;
   private final String badge;
   private final String sound;
   private final String filterString;
   private Queue queue;
   private boolean delivering = false;
   private final Object DeliveryGuard = new Object();
   private Filter filter;

   public AeroGearConnectorService(String connectorName, Map<String, Object> configuration, PostOffice postOffice)
   {
      this.connectorName = connectorName;
      this.postOffice = postOffice;
      this.queueName = ConfigurationHelper.getStringProperty(AeroGearConstants.QUEUE_NAME, null, configuration);
      this.endpoint = ConfigurationHelper.getStringProperty(AeroGearConstants.ENDPOINT_NAME, null, configuration);
      this.applicationId = ConfigurationHelper.getStringProperty(AeroGearConstants.APPLICATION_ID_NAME, null, configuration);
      this.applicationMasterSecret = ConfigurationHelper.getStringProperty(AeroGearConstants.APPLICATION_MASTER_SECRET_NAME, null, configuration);
      this.ttl = ConfigurationHelper.getIntProperty(AeroGearConstants.TTL_NAME, AeroGearConstants.DEFAULT_TTL, configuration);
      this.badge = ConfigurationHelper.getStringProperty(AeroGearConstants.BADGE_NAME, null, configuration);
      this.sound = ConfigurationHelper.getStringProperty(AeroGearConstants.SOUND_NAME, AeroGearConstants.DEFAULT_SOUND, configuration);
      this.filterString = ConfigurationHelper.getStringProperty(AeroGearConstants.FILTER_NAME, null, configuration);
   }

   @Override
   public String getName()
   {
      return connectorName;
   }

   @Override
   public void start() throws Exception
   {
      if(filterString != null)
      {
         filter = FilterImpl.createFilter(filterString);
      }
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
   public HandleStatus handle(final MessageReference reference) throws Exception
   {
      ServerMessage message = reference.getMessage();

      if (filter != null && !filter.match(message))
      {
         if (HornetQServerLogger.LOGGER.isTraceEnabled())
         {
            HornetQServerLogger.LOGGER.trace("Reference " + reference + " is a noMatch on consumer " + this);
         }
         return HandleStatus.NO_MATCH;
      }

      synchronized (DeliveryGuard)
      {
         if(delivering)
         {
            return HandleStatus.BUSY;
         }
         delivering = true;
      }

      String alert = message.getBodyBuffer().readString();

      JavaSender sender = new SenderClient(endpoint);

      UnifiedMessage.Builder builder = new UnifiedMessage.Builder();

      builder.pushApplicationId(applicationId)
            .masterSecret(applicationMasterSecret)
            .alert(alert);

      String sound = message.containsProperty(AeroGearConstants.AEROGEAR_SOUND)?message.getStringProperty(AeroGearConstants.AEROGEAR_SOUND):this.sound;

      if(sound != null)
      {
         builder.sound(sound);
      }

      String badge = message.containsProperty(AeroGearConstants.AEROGEAR_BADGE)?message.getStringProperty(AeroGearConstants.AEROGEAR_BADGE):this.badge;

      if(badge != null)
      {
         builder.badge(badge);
      }

      Integer ttl = message.containsProperty(AeroGearConstants.AEROGEAR_TTL)?message.getIntProperty(AeroGearConstants.AEROGEAR_TTL):this.ttl;

      if(ttl != null)
      {
         builder.timeToLive(ttl);
      }

      Set<SimpleString> propertyNames = message.getPropertyNames();

      for (SimpleString propertyName : propertyNames)
      {
         if(propertyName.toString().startsWith("AEROGEAR_"))
         {
            Object property = message.getTypedProperties().getProperty(propertyName);
            builder.attribute(propertyName.toString(), property.toString());
         }
      }

      UnifiedMessage unifiedMessage = builder.build();

      sender.send(unifiedMessage, new MessageResponseCallback()
      {
         public void onComplete(int statusCode)
         {
            if(statusCode != 200)
            {
               try
               {
                  queue.cancel(reference, System.currentTimeMillis());
               }
               catch (Exception e)
               {
                  HornetQAeroGearLogger.LOGGER.unableToCancelDelivery(reference.getMessage().getMessageID(), e);
               }
               //todo add some reconnect code
               queue.removeConsumer(AeroGearConnectorService.this);
            }
            else
            {
               try
               {
                  reference.acknowledge();
               }
               catch (Exception e)
               {
                  HornetQAeroGearLogger.LOGGER.errorAcking(reference.getMessage().getMessageID(), e);
               }
            }

            synchronized (DeliveryGuard)
            {
               delivering = false;
               queue.deliverAsync();
            }
         }

         public void onError(Throwable throwable)
         {
            try
            {
               queue.cancel(reference, System.currentTimeMillis());
            }
            catch (Exception e)
            {
               HornetQAeroGearLogger.LOGGER.unableToCancelDelivery(reference.getMessage().getMessageID(), e);
            }
            //todo add some reconnect code
            queue.removeConsumer(AeroGearConnectorService.this);
         }
      });
      return HandleStatus.HANDLED;
   }

   @Override
   public void proceedDeliver(MessageReference reference) throws Exception
   {
      //noop
   }

   @Override
   public Filter getFilter()
   {
      return filter;
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
