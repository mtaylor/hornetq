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
package org.hornetq.cli.commands;

import io.airlift.command.Arguments;
import io.airlift.command.Command;
import org.hornetq.dto.BrokerDTO;
import org.hornetq.factory.BrokerFactory;
import org.hornetq.utils.FactoryFinder;

import java.io.File;
import java.net.URI;

@Command(name = "stop", description = "stops the broker instance")
public class Stop implements Action
{
   @Arguments(description = "Broker Configuration URI, default 'xml:config/hornetq.xml'")
   String configuration = "xml:config/hornetq.xml";

   private static final String JMX_URL = "service:jmx:rmi:///jndi/rmi://localhost:3000/jmxrmi";

   @Override
   public Object execute(ActionContext context) throws Exception
   {
      URI configURI = new URI(configuration);
      FactoryFinder finder = new FactoryFinder("META-INF/services/org/hornetq/broker/");
      BrokerFactory factory = (BrokerFactory)finder.newInstance(configURI.getScheme());
      BrokerDTO broker = factory.createBroker(configURI);

      File file = new File(broker.core.configuration).getParentFile();

      File stopFile = new File(file, "STOP_ME");

      stopFile.createNewFile();

      return null;
   }
}
