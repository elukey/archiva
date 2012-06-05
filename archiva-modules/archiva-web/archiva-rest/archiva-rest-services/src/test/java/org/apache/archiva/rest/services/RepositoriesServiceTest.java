package org.apache.archiva.rest.services;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.common.utils.FileUtil;
import org.apache.archiva.rest.api.model.Artifact;
import org.apache.archiva.rest.api.model.BrowseResult;
import org.apache.archiva.rest.api.model.BrowseResultEntry;
import org.apache.archiva.rest.api.model.VersionsList;
import org.apache.archiva.rest.api.services.BrowseService;
import org.apache.archiva.rest.api.services.ManagedRepositoriesService;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.apache.cxf.jaxrs.client.ServerWebApplicationException;
import org.fest.assertions.Assertions;
import org.junit.Test;

import java.io.File;
import java.util.List;

/**
 * @author Olivier Lamy
 */
public class RepositoriesServiceTest
    extends AbstractArchivaRestTest
{

    @Test( expected = ServerWebApplicationException.class )
    public void scanRepoKarmaFailed()
        throws Exception
    {
        RepositoriesService service = getRepositoriesService();
        try
        {
            service.scanRepository( "id", true );
        }
        catch ( ServerWebApplicationException e )
        {
            assertEquals( 403, e.getStatus() );
            throw e;
        }
    }

    @Test
    public void scanRepo()
        throws Exception
    {
        RepositoriesService service = getRepositoriesService( authorizationHeader );

        ManagedRepositoriesService managedRepositoriesService = getManagedRepositoriesService( authorizationHeader );

        String repoId = managedRepositoriesService.getManagedRepositories().get( 0 ).getId();

        int timeout = 20000;
        while ( timeout > 0 && service.alreadyScanning( repoId ) )
        {
            Thread.sleep( 500 );
            timeout -= 500;
        }

        assertTrue( service.scanRepository( repoId, true ) );
    }

    @Test( expected = ServerWebApplicationException.class )
    public void deleteArtifactKarmaFailed()
        throws Exception
    {
        try
        {
            Artifact artifact = new Artifact();
            artifact.setGroupId( "commons-logging" );
            artifact.setArtifactId( "commons-logging" );
            artifact.setVersion( "1.0.1" );
            artifact.setPackaging( "jar" );
            artifact.setContext( SOURCE_REPO_ID );

            RepositoriesService repositoriesService = getRepositoriesService( null );

            repositoriesService.deleteArtifact( artifact );
        }
        catch ( ServerWebApplicationException e )
        {
            assertEquals( 403, e.getStatus() );
            throw e;

        }
    }

    @Test( expected = ServerWebApplicationException.class )
    public void deleteWithRepoNull()
        throws Exception
    {
        try
        {

            RepositoriesService repositoriesService = getRepositoriesService( authorizationHeader );

            Artifact artifact = new Artifact();
            artifact.setGroupId( "commons-logging" );
            artifact.setArtifactId( "commons-logging" );
            artifact.setVersion( "1.0.1" );
            artifact.setPackaging( "jar" );

            repositoriesService.deleteArtifact( artifact );
        }
        catch ( ServerWebApplicationException e )
        {
            assertEquals( "not http 400 status", 400, e.getStatus() );
            throw e;
        }
    }


    @Test
    public void deleteArtifact()
        throws Exception
    {
        initSourceTargetRepo();

        BrowseService browseService = getBrowseService( authorizationHeader, false );

        List<Artifact> artifacts =
            browseService.getArtifactDownloadInfos( "org.apache.karaf.features", "org.apache.karaf.features.core",
                                                    "2.2.2", SOURCE_REPO_ID );

        log.info( "artifacts: {}", artifacts );

        Assertions.assertThat( artifacts ).isNotNull().isNotEmpty().hasSize( 2 );

        VersionsList versionsList =
            browseService.getVersionsList( "org.apache.karaf.features", "org.apache.karaf.features.core",
                                           SOURCE_REPO_ID );
        Assertions.assertThat( versionsList.getVersions() ).isNotNull().isNotEmpty().hasSize( 2 );

        log.info( "artifacts.size: {}", artifacts.size() );

        try
        {
            File artifactFile = new File(
                "target/test-origin-repo/org/apache/karaf/features/org.apache.karaf.features.core/2.2.2/org.apache.karaf.features.core-2.2.2.jar" );

            assertTrue( "artifact not exists:" + artifactFile.getPath(), artifactFile.exists() );

            Artifact artifact = new Artifact();
            artifact.setGroupId( "org.apache.karaf.features" );
            artifact.setArtifactId( "org.apache.karaf.features.core" );
            artifact.setVersion( "2.2.2" );
            artifact.setPackaging( "jar" );
            artifact.setContext( SOURCE_REPO_ID );

            RepositoriesService repositoriesService = getRepositoriesService( authorizationHeader );

            repositoriesService.deleteArtifact( artifact );

            assertFalse( "artifact not deleted exists:" + artifactFile.getPath(), artifactFile.exists() );

            artifacts =
                browseService.getArtifactDownloadInfos( "org.apache.karaf.features", "org.apache.karaf.features.core",
                                                        "2.2.2", SOURCE_REPO_ID );

            Assertions.assertThat( artifacts ).isNotNull().isEmpty();

            versionsList = browseService.getVersionsList( "org.apache.karaf.features", "org.apache.karaf.features.core",
                                                          SOURCE_REPO_ID );

            Assertions.assertThat( versionsList.getVersions() ).isNotNull().isNotEmpty().hasSize( 1 );

        }
        finally
        {
            cleanRepos();
        }
    }

    @Test
    public void deleteArtifactWithClassifier()
        throws Exception
    {
        initSourceTargetRepo();

        BrowseService browseService = getBrowseService( authorizationHeader, false );

        List<Artifact> artifacts =
            browseService.getArtifactDownloadInfos( "commons-logging", "commons-logging", "1.0.1", SOURCE_REPO_ID );

        Assertions.assertThat( artifacts ).isNotNull().isNotEmpty().hasSize( 3 );

        VersionsList versionsList =
            browseService.getVersionsList( "commons-logging", "commons-logging", SOURCE_REPO_ID );
        Assertions.assertThat( versionsList.getVersions() ).isNotNull().isNotEmpty().hasSize( 6 );

        log.info( "artifacts.size: {}", artifacts.size() );

        try
        {
            File artifactFile = new File(
                "target/test-origin-repo/commons-logging/commons-logging/1.0.1/commons-logging-1.0.1-javadoc.jar" );

            File artifactFilemd5 = new File(
                "target/test-origin-repo/commons-logging/commons-logging/1.0.1/commons-logging-1.0.1-javadoc.jar.md5" );

            File artifactFilesha1 = new File(
                "target/test-origin-repo/commons-logging/commons-logging/1.0.1/commons-logging-1.0.1-javadoc.jar.sha1" );

            assertTrue( "artifact not exists:" + artifactFile.getPath(), artifactFile.exists() );

            assertTrue( "md5 not exists:" + artifactFilemd5.getPath(), artifactFilemd5.exists() );
            assertTrue( "sha1 not exists:" + artifactFilesha1.getPath(), artifactFilesha1.exists() );

            Artifact artifact = new Artifact();
            artifact.setGroupId( "commons-logging" );
            artifact.setArtifactId( "commons-logging" );
            artifact.setVersion( "1.0.1" );
            artifact.setClassifier( "javadoc" );
            artifact.setPackaging( "jar" );
            artifact.setContext( SOURCE_REPO_ID );

            RepositoriesService repositoriesService = getRepositoriesService( authorizationHeader );

            repositoriesService.deleteArtifact( artifact );

            assertFalse( "artifact not deleted exists:" + artifactFile.getPath(), artifactFile.exists() );
            assertFalse( "md5 still exists:" + artifactFilemd5.getPath(), artifactFilemd5.exists() );
            assertFalse( "sha1 still exists:" + artifactFilesha1.getPath(), artifactFilesha1.exists() );

            artifacts =
                browseService.getArtifactDownloadInfos( "commons-logging", "commons-logging", "1.0.1", SOURCE_REPO_ID );

            log.info( "artifact: {}", artifacts );

            Assertions.assertThat( artifacts ).isNotNull().isNotEmpty().hasSize( 2 );

            versionsList = browseService.getVersionsList( "commons-logging", "commons-logging", SOURCE_REPO_ID );

            log.info( "versionsList: {}", versionsList );

            Assertions.assertThat( versionsList.getVersions() ).isNotNull().isNotEmpty().hasSize( 6 );

        }
        finally
        {
            cleanRepos();
        }
    }


    @Test
    public void deleteGroupId()
        throws Exception
    {
        initSourceTargetRepo();
        try
        {
            BrowseService browseService = getBrowseService( authorizationHeader, false );

            BrowseResult browseResult = browseService.browseGroupId( "org.apache.karaf.features", SOURCE_REPO_ID );

            assertNotNull( browseResult );

            Assertions.assertThat( browseResult.getBrowseResultEntries() ).isNotNull().isNotEmpty().contains(
                new BrowseResultEntry( "org.apache.karaf.features.org.apache.karaf.features.command", true ),
                new BrowseResultEntry( "org.apache.karaf.features.org.apache.karaf.features.core", true ) );

            log.info( "browseResult: {}", browseResult );
        }
        finally
        {
            cleanRepos();
        }
    }

    @Test
    public void authorizedToDeleteArtifacts()
        throws Exception
    {
        ManagedRepository managedRepository = getTestManagedRepository( "SOURCE_REPO_ID", "SOURCE_REPO_ID" );
        try
        {
            getManagedRepositoriesService( authorizationHeader ).addManagedRepository( managedRepository );
            RepositoriesService repositoriesService = getRepositoriesService( authorizationHeader );
            assertTrue( repositoriesService.isAuthorizedToDeleteArtifacts( managedRepository.getId() ) );
        }
        finally
        {
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( managedRepository.getId(),
                                                                                          true );
        }
    }

    @Test
    public void notAuthorizedToDeleteArtifacts()
        throws Exception
    {
        ManagedRepository managedRepository = getTestManagedRepository( "SOURCE_REPO_ID", "SOURCE_REPO_ID" );
        try
        {
            getManagedRepositoriesService( authorizationHeader ).addManagedRepository( managedRepository );
            RepositoriesService repositoriesService = getRepositoriesService( guestAuthzHeader );
            assertFalse( repositoriesService.isAuthorizedToDeleteArtifacts( managedRepository.getId() ) );
        }
        finally
        {
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( managedRepository.getId(),
                                                                                          true );
        }
    }

    protected ManagedRepository getTestManagedRepository( String id, String path )
    {
        String location = new File( FileUtil.getBasedir(), "target/" + path ).getAbsolutePath();
        return new ManagedRepository( id, id, location, "default", true, true, true, "2 * * * * ?", null, false, 80, 80,
                                      true, false );
    }

    protected ManagedRepository getTestManagedRepository()
    {
        return getTestManagedRepository( "TEST", "test-repo" );
    }

}
