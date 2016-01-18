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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Utility class to resolve hostname as reliably as possible.
 */
public class HostnameResolver
{

    static String resolveHostname()
    {
        try
        {
            return InetAddress.getLocalHost().getHostName();
        }
        catch ( UnknownHostException uhe )
        {
            try
            {
                return readHostnameWithOsCommands();
            }
            catch ( IOException ioe )
            {
                uhe.printStackTrace();
                ioe.printStackTrace();
                throw new IllegalStateException( "Cannot get hostname neither using  "
                        + "InetAddress.getLocalHost().getHostName() nor executing os 'hostname' command. "
                        + "Check error output for details." );
            }
        }
    }

    private static String readHostnameWithOsCommands() throws IOException
    {
        final String os = System.getProperty( "os.name" ).toLowerCase();

        if ( os.contains( "win" ) )
        {
            return execCmd( "hostname" );
        }
        if ( os.contains( "nix" ) || os.contains( "nux" ) )
        {
            return execCmd( "hostname" );
        }
        throw new IOException( String.format( "Unsupported OS '%s'", os ) );
    }


    private static String execCmd( String cmd ) throws IOException
    {
        Scanner s = new Scanner( Runtime.getRuntime().exec( cmd ).getInputStream() ).useDelimiter( "\\A" );
        if ( s.hasNext() )
        {
            return s.next();
        }
        throw new IOException( "Empty result for command: " + cmd );
    }

}
