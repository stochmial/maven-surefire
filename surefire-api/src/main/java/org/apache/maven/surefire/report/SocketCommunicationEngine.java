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

    public static final String SOCKET = "socket";

    public static final int PAUSE_BETWEEN_RETRIES = 1000;

    private static final String HOSTNAME = HostnameResolver.resolveHostname();

    private final URI uri;

    private final int retries;

    public SocketCommunicationEngine( String uri, int retries )
    {
        this.retries = retries;
        this.uri = createUri( uri );
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
                response = tryRequest( request );
                break;
            }
            catch ( IOException e )
            {
                if ( tries < retries )
                {
                    tries++;
                    System.out.println( "Error connecting to external test source. Retry in 1 second." );
                    sleep( PAUSE_BETWEEN_RETRIES );
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
        SocketCommunicationEngine engine = new SocketCommunicationEngine( "socket://localhost:8989", 0 );
        System.out.println( String.format( "connect to: %s", engine.uri ) );
        String testName = engine.tryRequest( engine.createRequestJson( "GetNext", engine.uri ) );
        System.out.println( String.format( "testName='%s'", testName ) );
    }

}
