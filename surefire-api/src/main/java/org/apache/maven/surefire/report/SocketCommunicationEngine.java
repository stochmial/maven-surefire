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

import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * An engine to send/receive requests related to single test execution.
 */
public class SocketCommunicationEngine
{
    public static final String JUST_WAIT = "WAIT";

    public static final String SOCKET = "socket";

    public static final int DEFAULT_PAUSE_BETWEEN_RETRIES = 1000;

    public static final int DEFAULT_NUMBER_OF_RETRIES = 3;

    private static final String HOSTNAME = HostnameResolver.resolveHostname();

    private final URI uri;

    private final int retries;

    private final boolean debugMode;

    private final int pauseBetweenRetries;

    public SocketCommunicationEngine( String uri, int retries, int pauseBetweenRetriesInSeconds, boolean debugMode )
    {
        this.retries = retries;
        this.uri = createUri( uri );
        this.debugMode = debugMode;
        this.pauseBetweenRetries = pauseBetweenRetriesInSeconds;
    }

    private URI createUri( String sourceUrl )
    {
        URI uri = null;
        try
        {
            uri = new URI( sourceUrl );

            if ( !SOCKET.equals( uri.getScheme() ) )
            {
                throw new IllegalArgumentException( "No support for external test provider with URI: " + uri );
            }
            return uri;
        }
        catch ( URISyntaxException e )
        {

            throw new IllegalArgumentException( "No support for external test provider with URI: " + uri );
        }
    }

    private void sleep( int time )
    {
        try
        {
            Thread.sleep( time );
        }
        catch ( InterruptedException e1 )
        {
            throw new RuntimeException( e1 );
        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder( "SocketCommunicationEngine: " );
        sb.append( '(' ).append( uri ).append( "):" );
        sb.append( '(' ).append( retries ).append( "):" );
        return sb.toString();
    }

    public String sendRequest( String requestType )
    {
        return sendRequest( requestType, null );
    }

    public String sendRequest( String requestType, Object pojo )
    {
        String response;
        int tries = 0;

        String request = createRequestJson( requestType, pojo );
        while ( true )
        {
            try
            {
                if ( debugMode )
                {
                    System.out.println( String.format( "TEST CLIENT: Sending request '%s', attempt number '%d'.",
                            requestType, tries + 1 ) );
                }
                response = tryRequest( request );
                if ( debugMode )
                {
                    System.out.println( String.format(
                            "TEST CLIENT: Response received for request '%s', attempt number '%d',"
                                    + " response content '%s'.", requestType, tries + 1, response ) );
                }
                break;
            }
            catch ( IOException e )
            {
                if ( tries < retries )
                {
                    tries++;
                    System.out.println( "TEST CLIENT: Error connecting to external test source. Retry in 1 second." );
                    sleep( pauseBetweenRetries );
                }
                else
                {
                    throw new IllegalStateException( "Broken communication with test server", e );
                }
            }
        }
        return response;
    }

    private String createRequestJson( String requestType, Object pojo )
    {

        JSONObject obj = new JSONObject();
        obj.put( "hostname", HOSTNAME );
        obj.put( "request", requestType );
        addPojoToJson( pojo, obj );
        StringWriter out = new StringWriter();
        try
        {
            obj.writeJSONString( out );
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( e );
        }
        String requestStr = out.toString();
        return requestStr + '\n';
    }

    private void addPojoToJson( Object pojo, JSONObject obj )
    {
        if ( pojo != null )
        {
            if ( pojo instanceof CharSequence )
            {
                obj.put( "data", pojo );
            }
            else
            {
                // not needed yet
                throw new IllegalArgumentException( "unsupported object type" );
            }
        }
    }

    private String tryRequest( String requestContent )
            throws IOException
    {
        String response = null;
        Socket socket = new Socket( uri.getHost(), uri.getPort() );
        BufferedWriter out = null;
        BufferedReader in = null;
        try
        {
            out = new BufferedWriter( new OutputStreamWriter( socket.getOutputStream() ) );
            out.write( requestContent );
            out.flush();
            in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
            response = in.readLine();
            socket.shutdownInput();
        }
        finally
        {
            try
            {
                if ( out != null )
                {
                    out.close();
                }

                if ( in != null )
                {
                    in.close();
                }
            }
            finally
            {
                socket.close();
            }
        }

        return response;
    }

    public static void main( String... args ) throws IOException
    {
        SocketCommunicationEngine engine = new SocketCommunicationEngine(
                "socket://localhost:8989", 0, DEFAULT_PAUSE_BETWEEN_RETRIES, true );

        System.out.println( String.format( "connect to: %s", engine.uri ) );

        String testName = engine.sendRequest( "GetNext" );

        while ( ! "".equals( testName ) )
        {
            if ( JUST_WAIT.equals( testName ) )
            {
                System.out.println( "waiting" );
            }
            else
            {
                System.out.println( String.format( "executing testName='%s'", testName ) );
            }

            testName = engine.sendRequest( "GetNext" );
        }

        System.out.println( "No more tests" );
    }

}
