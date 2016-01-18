package org.apache.maven.surefire.booter;

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

import org.apache.maven.surefire.report.SocketCommunicationEngine;
import org.apache.maven.surefire.util.ReflectionUtils;
import org.apache.maven.surefire.util.TestsToRun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * TestToRun implementation that lazily request tests from external service via socket
 *
 * @author Marek Piechut
 */
public class LazySocketTestToRun
        extends TestsToRun
{
    public static final String JUST_WAIT = "WAIT";

    public static final int WAIT_STEP_SECONDS = 30;

    private final List<Class> workQueue = new ArrayList<Class>();

    private boolean finished = false;

    private final SocketCommunicationEngine socketCommunicationEngine;


    public LazySocketTestToRun( SocketCommunicationEngine socketCommunicationEngine )
    {
        super( Collections.<Class>emptyList() );
        this.socketCommunicationEngine = socketCommunicationEngine;
    }

    private boolean hasNextTextClass( int pos )
    {
        synchronized ( workQueue )
        {
            if ( !finished )
            {
                String nextTestName = socketCommunicationEngine.sendRequest( "GetNext" );
                if ( nothingMoreToProcess( nextTestName ) )
                {
                    finished = true;
                }
                else if ( justWait( nextTestName ) )
                {
                    sleep( WAIT_STEP_SECONDS );
                }
                else
                {
                    Class testClass = loadTestClass( nextTestName );
                    workQueue.add( testClass );
                }
            }
            return workQueue.size() > pos;
        }
    }

    private boolean justWait( String testName )
    {
        return JUST_WAIT.equals( testName );
    }

    private void sleep( int sec )
    {
        try
        {
            Thread.sleep( sec * 1000 );
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
    }

    private boolean nothingMoreToProcess( String nextTestName )
    {
        return nextTestName == null || nextTestName.trim().length() == 0
                || nextTestName.trim().equalsIgnoreCase( "null" );
    }


    private Class getItem( int pos )
    {
        synchronized ( workQueue )
        {
            return workQueue.get( pos );
        }
    }

    private Class loadTestClass( String name )
    {
        return ReflectionUtils.loadClass( Thread.currentThread().getContextClassLoader(), name );
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder( "LazySocketTestsToRun: " );
        sb.append( '(' ).append( socketCommunicationEngine ).append( "):" );
        synchronized ( workQueue )
        {
            sb.append( workQueue );
        }

        return sb.toString();
    }

    @Override
    public Iterator<Class> iterator()
    {
        return new NextExternalTestIterator();
    }

    @Override
    public boolean allowEagerReading()
    {
        return false;
    }

    private class NextExternalTestIterator
            implements Iterator<Class>
    {
        private int pos = -1;

        public boolean hasNext()
        {
            return hasNextTextClass( pos + 1 );
        }

        public Class next()
        {
            return getItem( ++pos );
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

}
