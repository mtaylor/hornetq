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
package org.hornetq.core.config;

import java.io.Serializable;
import java.util.Map;

import org.hornetq.core.server.ConnectorServiceFactory;
import org.hornetq.utils.ClassloadingUtil;

/**
 * A ConnectorServiceConfiguration
 * @author <a href="tm.igarashi@gmail.com">Tomohisa Igarashi</a>
 * @author <a href="mailto:mtaylor@redhat.com">Martyn Taylor</a>
 */
public class ConnectorServiceConfiguration implements Serializable
{
   private static final long serialVersionUID = -641207073030767325L;

   private final String name;

   private final  String factoryClassName;

   private final  Map<String, Object> params;

   private transient ConnectorServiceFactory connectorServiceFactory;

   public ConnectorServiceConfiguration(final String clazz, final Map<String, Object> params, final String name)
   {
      this.name = name;
      factoryClassName = clazz;
      this.params = params;
   }

   public ConnectorServiceConfiguration(final ConnectorServiceFactory connectorServiceFactory,
                                        final Map<String, Object> params,
                                        final String name)
   {
      this.name = name;
      this.connectorServiceFactory = connectorServiceFactory;
      this.factoryClassName = connectorServiceFactory.getClass().getCanonicalName();
      this.params = params;
   }

   public String getConnectorName()
   {
      return name;
   }

   public String getFactoryClassName()
   {
      return factoryClassName;
   }

   public Map<String, Object> getParams()
   {
      return params;
   }

   @Override
   public boolean equals(Object o)
   {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;

      ConnectorServiceConfiguration that = (ConnectorServiceConfiguration) o;

      if (getFactoryClassName() != null ? !getFactoryClassName().equals(that.getFactoryClassName()) : that.getFactoryClassName() != null)
         return false;
      if (getConnectorName() != null ? !getConnectorName().equals(that.getConnectorName()) : that.getConnectorName() != null)
         return false;
      if (getParams() != null ? !getParams().equals(that.getParams()) : that.getParams() != null)
         return false;

      return true;
   }

   @Override
   public int hashCode()
   {
      int result = getConnectorName() != null ? getConnectorName().hashCode() : 0;
      result = 31 * result + (getFactoryClassName() != null ? getFactoryClassName().hashCode() : 0);
      result = 31 * result + (getParams() != null ? getParams().hashCode() : 0);
      return result;
   }

   public ConnectorServiceFactory getConnectorServiceFactory()
   {
      if (connectorServiceFactory == null)
      {
         connectorServiceFactory = (ConnectorServiceFactory) ClassloadingUtil.newInstanceFromClassLoader(getFactoryClassName());
      }
      return connectorServiceFactory;
   }
}
