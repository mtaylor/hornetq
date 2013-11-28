package org.hornetq.integration.aerogear;

import java.util.HashSet;
import java.util.Set;

public class AeroGearConstants
{
   public static final Set<String> ALLOWABLE_PROPERTIES = new HashSet<>();
   public static final Set<String> REQUIRED_PROPERTIES = new HashSet<>();
   public static final String QUEUE_NAME = "queue";

   static
   {
      ALLOWABLE_PROPERTIES.add(QUEUE_NAME);

      REQUIRED_PROPERTIES.add(QUEUE_NAME);
   }
}
