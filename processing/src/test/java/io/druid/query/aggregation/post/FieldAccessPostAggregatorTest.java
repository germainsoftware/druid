/*
 * Druid - a distributed column store.
 * Copyright 2012 - 2015 Metamarkets Group Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.druid.query.aggregation.post;

import io.druid.query.aggregation.CountAggregator;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class FieldAccessPostAggregatorTest
{
  @Test
  public void testCompute()
  {
    FieldAccessPostAggregator fieldAccessPostAggregator;

    fieldAccessPostAggregator = new FieldAccessPostAggregator("To be, or not to be, that is the question:", "rows");
    CountAggregator agg = new CountAggregator("rows");
    Map<String, Object> metricValues = new HashMap<String, Object>();
    metricValues.put(agg.getName(), agg.get());
    Assert.assertEquals(new Long(0L), fieldAccessPostAggregator.compute(metricValues));

    agg.aggregate();
    agg.aggregate();
    agg.aggregate();
    metricValues.put(agg.getName(), agg.get());
    Assert.assertEquals(new Long(3L), fieldAccessPostAggregator.compute(metricValues));
  }
}
