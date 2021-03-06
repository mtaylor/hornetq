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
package org.hornetq.core.config.ha;

import org.hornetq.core.config.HAPolicyConfiguration;
import org.hornetq.core.config.ScaleDownConfiguration;

public class LiveOnlyPolicyConfiguration implements HAPolicyConfiguration
{
   public LiveOnlyPolicyConfiguration()
   {
   }

   public LiveOnlyPolicyConfiguration(ScaleDownConfiguration scaleDownConfiguration)
   {
      this.scaleDownConfiguration = scaleDownConfiguration;
   }

   @Override
   public TYPE getType()
   {
      return TYPE.LIVE_ONLY;
   }

   public ScaleDownConfiguration getScaleDownConfiguration()
   {
      return scaleDownConfiguration;
   }

   public void setScaleDownConfiguration(ScaleDownConfiguration scaleDownConfiguration)
   {
      this.scaleDownConfiguration = scaleDownConfiguration;
   }

   ScaleDownConfiguration scaleDownConfiguration;
}
