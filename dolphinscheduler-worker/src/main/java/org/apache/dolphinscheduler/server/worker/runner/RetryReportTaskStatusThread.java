/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.server.worker.runner;

import org.apache.dolphinscheduler.common.thread.Stopper;
import org.apache.dolphinscheduler.common.thread.ThreadUtils;
import org.apache.dolphinscheduler.remote.command.Command;
import org.apache.dolphinscheduler.server.worker.cache.ResponseCache;
import org.apache.dolphinscheduler.server.worker.processor.TaskCallbackService;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Retry Report Task Status Thread
 */
@Component
public class RetryReportTaskStatusThread implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(RetryReportTaskStatusThread.class);

    /**
     * every 5 minutes
     */
    private static long RETRY_REPORT_TASK_STATUS_INTERVAL = 5 * 60 * 1000L;

    @Autowired
    private TaskCallbackService taskCallbackService;

    public void start() {
        logger.info("Retry report task status thread starting");
        Thread thread = new Thread(this, "RetryReportTaskStatusThread");
        thread.setDaemon(true);
        thread.start();
        logger.info("Retry report task status thread started");
    }

    /**
     * retry ack/response
     */
    @Override
    public void run() {
        ResponseCache instance = ResponseCache.get();

        while (Stopper.isRunning()) {

            // sleep 5 minutes
            ThreadUtils.sleep(RETRY_REPORT_TASK_STATUS_INTERVAL);

            try {
                if (!instance.getRunningCache().isEmpty()) {
                    Map<Integer, Command> runningCache = instance.getRunningCache();
                    for (Map.Entry<Integer, Command> entry : runningCache.entrySet()) {
                        Integer taskInstanceId = entry.getKey();
                        Command runningCommand = entry.getValue();
                        taskCallbackService.send(taskInstanceId, runningCommand);
                    }
                }

                if (!instance.getResponseCache().isEmpty()) {
                    Map<Integer, Command> responseCache = instance.getResponseCache();
                    for (Map.Entry<Integer, Command> entry : responseCache.entrySet()) {
                        Integer taskInstanceId = entry.getKey();
                        Command responseCommand = entry.getValue();
                        taskCallbackService.send(taskInstanceId, responseCommand);
                    }
                }
                if (!instance.getRecallCache().isEmpty()) {
                    Map<Integer, Command> recallCache = instance.getRecallCache();
                    for (Map.Entry<Integer, Command> entry : recallCache.entrySet()) {
                        Integer taskInstanceId = entry.getKey();
                        Command responseCommand = entry.getValue();
                        taskCallbackService.send(taskInstanceId, responseCommand);
                    }
                }
            } catch (Exception e) {
                logger.warn("Retry report task status error", e);
            }
        }
    }
}
