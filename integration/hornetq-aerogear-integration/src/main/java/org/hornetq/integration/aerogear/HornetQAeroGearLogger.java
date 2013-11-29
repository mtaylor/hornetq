package org.hornetq.integration.aerogear;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Logger Code 23
 *
 * each message id must be 6 digits long starting with 18, the 3rd digit donates the level so
 *
 * INF0  1
 * WARN  2
 * DEBUG 3
 * ERROR 4
 * TRACE 5
 * FATAL 6
 *
 * so an INFO message would be 181000 to 181999
 */
@MessageLogger(projectCode = "HQ")
public interface HornetQAeroGearLogger extends BasicLogger
{
   /**
    * The aerogear logger.
    */
   HornetQAeroGearLogger LOGGER = Logger.getMessageLogger(HornetQAeroGearLogger.class, HornetQAeroGearLogger.class.getPackage().getName());

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 232001, value = "failed to ack message {0}", format = Message.Format.MESSAGE_FORMAT)
   void errorAcking(long id, @Cause Exception cause);

   @LogMessage(level = Logger.Level.WARN)
   @Message(id = 232002, value = "failed to cancel message {0}", format = Message.Format.MESSAGE_FORMAT)
   void unableToCancelDelivery(long messageID,@Cause Exception cause);
}
