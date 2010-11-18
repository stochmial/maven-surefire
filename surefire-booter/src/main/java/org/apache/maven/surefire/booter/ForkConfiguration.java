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

/**
 * Configuration for forking tests.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 */
public class ForkConfiguration
{
    public static final String FORK_NEVER = "never";

    private final String forkMode;

    private final boolean useSystemClassLoader;

    private final boolean useManifestOnlyJar;

    public ForkConfiguration( boolean useSystemClassLoader, boolean useManifestOnlyJar, String forkMode )
    {
        this.useSystemClassLoader = useSystemClassLoader;
        this.useManifestOnlyJar = useManifestOnlyJar;
        this.forkMode = forkMode;
    }

    public boolean isForking()
    {
        return !FORK_NEVER.equals( forkMode );
    }

    public boolean isUseSystemClassLoader()
    {
        return useSystemClassLoader;
    }

    public boolean isManifestOnlyJarRequestedAndUsable()
    {
        return isUseSystemClassLoader() && useManifestOnlyJar;
    }

}
