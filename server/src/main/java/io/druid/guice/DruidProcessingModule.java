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

package io.druid.guice;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.ProvisionException;
import com.metamx.common.concurrent.ExecutorServiceConfig;
import com.metamx.common.lifecycle.Lifecycle;
import com.metamx.common.logger.Logger;
import com.metamx.emitter.service.ServiceEmitter;
import com.metamx.emitter.service.ServiceMetricEvent;
import io.druid.client.cache.CacheConfig;
import io.druid.collections.StupidPool;
import io.druid.common.utils.VMUtils;
import io.druid.guice.annotations.BackgroundCaching;
import io.druid.guice.annotations.Global;
import io.druid.guice.annotations.Processing;
import io.druid.offheap.OffheapBufferPool;
import io.druid.query.DruidProcessingConfig;
import io.druid.query.MetricsEmittingExecutorService;
import io.druid.query.PrioritizedExecutorService;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 */
public class DruidProcessingModule implements Module
{
  private static final Logger log = new Logger(DruidProcessingModule.class);

  @Override
  public void configure(Binder binder)
  {
    ConfigProvider.bind(binder, DruidProcessingConfig.class, ImmutableMap.of("base_path", "druid.processing"));
    binder.bind(ExecutorServiceConfig.class).to(DruidProcessingConfig.class);
  }

  @Provides
  @BackgroundCaching
  @LazySingleton
  public ExecutorService getBackgroundExecutorService(
      CacheConfig cacheConfig
  )
  {
    if (cacheConfig.getNumBackgroundThreads() > 0) {
      return Executors.newFixedThreadPool(
          cacheConfig.getNumBackgroundThreads(),
          new ThreadFactoryBuilder()
              .setNameFormat("background-cacher-%d")
              .setDaemon(true)
              .setPriority(Thread.MIN_PRIORITY)
              .build()
      );
    } else {
      return MoreExecutors.sameThreadExecutor();
    }
  }

  @Provides
  @Processing
  @ManageLifecycle
  public ExecutorService getProcessingExecutorService(
      ExecutorServiceConfig config,
      ServiceEmitter emitter,
      Lifecycle lifecycle
  )
  {
    return new MetricsEmittingExecutorService(
        PrioritizedExecutorService.create(
            lifecycle,
            config
        ),
        emitter,
        new ServiceMetricEvent.Builder()
    );
  }

  @Provides
  @LazySingleton
  @Global
  public StupidPool<ByteBuffer> getIntermediateResultsPool(DruidProcessingConfig config)
  {
    try {
      long maxDirectMemory = VMUtils.getMaxDirectMemory();

      final long memoryNeeded = (long) config.intermediateComputeSizeBytes() * (config.getNumThreads() + 1);
      if (maxDirectMemory < memoryNeeded) {
        throw new ProvisionException(
            String.format(
                "Not enough direct memory.  Please adjust -XX:MaxDirectMemorySize, druid.processing.buffer.sizeBytes, or druid.processing.numThreads: "
                + "maxDirectMemory[%,d], memoryNeeded[%,d] = druid.processing.buffer.sizeBytes[%,d] * ( druid.processing.numThreads[%,d] + 1 )",
                maxDirectMemory,
                memoryNeeded,
                config.intermediateComputeSizeBytes(),
                config.getNumThreads()
            )
        );
      }
    } catch(UnsupportedOperationException e) {
      log.info(e.getMessage());
    }

    return new OffheapBufferPool(config.intermediateComputeSizeBytes());
  }


}
