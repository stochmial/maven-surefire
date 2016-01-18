package org.apache.maven.surefire.report;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


/**
 * Decorator that sends test-set results to provided url as json. It allows to receive test results in runtime
 * so external tools, like pipelines, can rerun the tests that didn't finish properly i.e. because of infrastructure
 * problems.
 */
public class RunListenerTestSetResultSocketDecorator implements RunListener
{
    private final SocketCommunicationEngine socketCommunicationEngine;
    private final RunListener runListener;

    public RunListenerTestSetResultSocketDecorator( SocketCommunicationEngine socketCommunicationEngine,
                                                    RunListener runListener )
    {
        this.socketCommunicationEngine = socketCommunicationEngine;
        this.runListener = runListener;
    }

    public void testSetStarting( ReportEntry report )
    {
        runListener.testSetStarting( report );
    }

    public void testSetCompleted( ReportEntry report )
    {
        runListener.testSetCompleted( report );
        socketCommunicationEngine.sendRequest( "TestSetResults", report );
    }

    public void testStarting( ReportEntry report )
    {

        runListener.testStarting( report );
    }

    public void testSucceeded( ReportEntry report )
    {
        runListener.testSucceeded( report );
    }

    public void testAssumptionFailure( ReportEntry report )
    {
        runListener.testAssumptionFailure( report );
    }

    public void testError( ReportEntry report )
    {
        runListener.testError( report );
    }

    public void testFailed( ReportEntry report )
    {
        runListener.testFailed( report );
    }

    public void testSkipped( ReportEntry report )
    {
        runListener.testSkipped( report );
    }
}
