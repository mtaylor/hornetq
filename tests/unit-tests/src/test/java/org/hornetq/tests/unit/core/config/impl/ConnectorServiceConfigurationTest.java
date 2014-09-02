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

package org.hornetq.tests.unit.core.config.impl;

import org.hornetq.core.config.ConnectorServiceConfiguration;
import org.hornetq.core.server.ConnectorServiceFactory;
import org.hornetq.tests.unit.core.config.impl.fake.FakeConnectorServiceFactory;
import org.hornetq.tests.util.UnitTestCase;
import org.junit.Test;

/**
 * @author <a href="mailto:mtaylor@redhat.com">Martyn Taylor</a>
 */

public class ConnectorServiceConfigurationTest extends UnitTestCase
{
   @Test
   public void testProvidedInstancesAreCachedAndReturned()
   {
      ConnectorServiceFactory connectorServiceFactory = new FakeConnectorServiceFactory();
      ConnectorServiceConfiguration config = new ConnectorServiceConfiguration(connectorServiceFactory, null, null);
      assertTrue(connectorServiceFactory == config.getConnectorServiceFactory());
   }

   @Test
   public void testNewConnectorServiceFactoryIsCreatedWhenNoneCurrentlyExists()
   {
      String className = FakeConnectorServiceFactory.class.getCanonicalName();
      ConnectorServiceConfiguration config = new ConnectorServiceConfiguration(className, null, null);
      assertNotNull(config.getConnectorServiceFactory());
      assertTrue(config.getConnectorServiceFactory() instanceof FakeConnectorServiceFactory);
   }

   @Test
   public void testConnectorServiceFactoryClassNameIsProperlySet()
   {
      ConnectorServiceFactory connectorServiceFactory = new FakeConnectorServiceFactory();
      ConnectorServiceConfiguration config = new ConnectorServiceConfiguration(connectorServiceFactory, null, null);
      assertEquals(config.getFactoryClassName(), FakeConnectorServiceFactory.class.getCanonicalName());
   }
}
