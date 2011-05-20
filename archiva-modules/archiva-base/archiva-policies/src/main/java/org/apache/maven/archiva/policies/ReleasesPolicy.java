package org.apache.maven.archiva.policies;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import org.springframework.stereotype.Service;

/**
 * {@link PreDownloadPolicy} to apply for released versions.
 *
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.policies.PreDownloadPolicy"
 *                   role-hint="releases"
 */
@Service("preDownloadPolicy#releases")
public class ReleasesPolicy
    extends AbstractUpdatePolicy
    implements PreDownloadPolicy
{
    /**
     * Defaults to {@link AbstractUpdatePolicy#HOURLY}
     */
    public String getDefaultOption()
    {
        return AbstractUpdatePolicy.HOURLY;
    }

    protected boolean isSnapshotPolicy()
    {
        return false;
    }
    
    protected String getUpdateMode()
    {
        return "releases";
    }

    public String getId()
    {
        return "releases";
    }

    public String getName()
    {
        return "Releases";
    }
}
