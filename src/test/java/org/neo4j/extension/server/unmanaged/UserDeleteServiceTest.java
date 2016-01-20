/**
 * Licensed to Neo Technology under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Neo Technology licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.neo4j.extension.server.unmanaged;

import org.junit.Test;

import java.net.URI;
import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.neo4j.server.rest.repr.OutputFormat;
import org.neo4j.server.rest.repr.formats.JsonFormat;
import org.neo4j.server.security.auth.AuthManager;
import org.neo4j.server.security.auth.Credential;
import org.neo4j.server.security.auth.User;
import org.neo4j.server.security.auth.exception.IllegalUsernameException;
import org.neo4j.test.server.EntityOutputFormat;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Test the rest endpoint.
 * @author Robin Bramley
 */
public class UserDeleteServiceTest
{
    private static final Principal NEO4J_PRINCIPLE = new Principal()
    {
        @Override
        public String getName()
        {
            return "neo4j";
        }
    };
    private static final Principal BAD_PRINCIPLE = new Principal()
    {
        @Override
        public String getName()
        {
            return "bad";
        }
    };
    private static final User NEO4J_USER = new User( "neo4j", Credential.forPassword( "neo4j" ), true );
    private static final User FOO_USER = new User( "foo", Credential.forPassword( "bar" ), true );

    @Test
    public void shouldDeleteUserAndReturnSuccess() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( NEO4J_PRINCIPLE );

        AuthManager authManager = mock( AuthManager.class );
        when( authManager.deleteUser( "foo" ) ).thenReturn( true );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserDeleteService userDeleteService = new UserDeleteService( authManager, outputFormat );

        // When
        Response response = userDeleteService.deleteUser( "foo", req );

        // Then
        assertThat( response.getStatus(), equalTo( 200 ) );
        verify( authManager ).deleteUser( "foo" );
    }

    @Test
    public void shouldReturn404WhenDeletingUserIfNotAuthenticated() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( null );

        AuthManager authManager = mock( AuthManager.class );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserDeleteService userDeleteService = new UserDeleteService( authManager, outputFormat );

        // When
        Response response = userDeleteService.deleteUser( "foo", req );

        // Then
        assertThat( response.getStatus(), equalTo( 404 ) );
    }

    @Test
    public void shouldReturn404WhenDeletingUserIfNotNeo4jUser() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( BAD_PRINCIPLE );

        AuthManager authManager = mock( AuthManager.class );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserDeleteService userDeleteService = new UserDeleteService( authManager, outputFormat );

        // When
        Response response = userDeleteService.deleteUser( "foo", req );

        // Then
        assertThat( response.getStatus(), equalTo( 404 ) );
        verifyZeroInteractions( authManager );
    }

    @Test
    public void shouldReturn404WhenDeletingInvalidUser() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( NEO4J_PRINCIPLE );

        AuthManager authManager = mock( AuthManager.class );
        when( authManager.deleteUser( "foo" ) ).thenReturn( false );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserDeleteService userDeleteService = new UserDeleteService( authManager, outputFormat );

        // When
        Response response = userDeleteService.deleteUser( "foo", req );

        // Then
        assertThat( response.getStatus(), equalTo( 404 ) );
        verify( authManager ).deleteUser( "foo" );
    }

}
