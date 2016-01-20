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

import org.neo4j.server.rest.repr.BadInputException;
import org.neo4j.server.rest.repr.InputFormat;
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
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Test the rest endpoint.
 * @author Robin Bramley
 */
public class UserAddServiceTest
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
    public void shouldCreateUserAndReturnSuccess() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( NEO4J_PRINCIPLE );

        AuthManager authManager = mock( AuthManager.class );
        when( authManager.newUser( "foo", "bar", true ) ).thenReturn( FOO_USER );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserAddService userAddService = new UserAddService( authManager, new JsonFormat(), outputFormat );

        // When
        Response response = userAddService.createUser( "foo", req, "{ \"password\" : \"bar\" }" );

        // Then
        assertThat( response.getStatus(), equalTo( 200 ) );
        verify( authManager ).newUser( "foo", "bar", true );
    }

    @Test
    public void shouldReturn404WhenCreatingUserIfNotAuthenticated() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( null );

        AuthManager authManager = mock( AuthManager.class );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserAddService userAddService = new UserAddService( authManager, new JsonFormat(), outputFormat );

        // When
        Response response = userAddService.createUser( "foo", req, "{ \"password\" : \"bar\" }" );

        // Then
        assertThat( response.getStatus(), equalTo( 404 ) );
    }

    @Test
    public void shouldReturn404WhenCreatingUserIfNotNeo4jUser() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( BAD_PRINCIPLE );

        AuthManager authManager = mock( AuthManager.class );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserAddService userAddService = new UserAddService( authManager, new JsonFormat(), outputFormat );

        // When
        Response response = userAddService.createUser( "foo", req, "{ \"password\" : \"bar\" }" );

        // Then
        assertThat( response.getStatus(), equalTo( 404 ) );
        verifyZeroInteractions( authManager );
    }

    @Test
    public void shouldReturn400IfPayloadIsInvalid() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( NEO4J_PRINCIPLE );

        AuthManager authManager = mock( AuthManager.class );

        InputFormat inputFormat = mock( InputFormat.class );
        when( inputFormat.readMap( anyString() )).thenThrow( new BadInputException( "Barf" ) );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserAddService userAddService = new UserAddService( authManager, inputFormat, outputFormat );

        // When
        Response response = userAddService.createUser( "foo", req, "{ \"password\" : \"bar\" }" );

        // Then
        assertThat( response.getStatus(), equalTo( 400 ) );
        String json = new String( (byte[]) response.getEntity() );
        assertNotNull( json );
        assertThat( json, containsString( "\"code\" : \"Neo.ClientError.Request.InvalidFormat\"" ) );
    }

    @Test
    public void shouldReturn422IfMissingPassword() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( NEO4J_PRINCIPLE );

        AuthManager authManager = mock( AuthManager.class );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserAddService userAddService = new UserAddService( authManager, new JsonFormat(), outputFormat );

        // When
        Response response = userAddService.createUser( "foo", req, "{ \"unknown\" : \"unknown\" }" );

        // Then
        assertThat( response.getStatus(), equalTo( 422 ) );
        String json = new String( (byte[]) response.getEntity() );
        assertNotNull( json );
        assertThat( json, containsString( "\"code\" : \"Neo.ClientError.Request.InvalidFormat\"" ) );
        assertThat( json, containsString( "\"message\" : \"Required parameter 'password' is missing.\"" ) );
    }

    @Test
    public void shouldReturn422IfInvalidPasswordType() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( NEO4J_PRINCIPLE );

        AuthManager authManager = mock( AuthManager.class );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserAddService userAddService = new UserAddService( authManager, new JsonFormat(), outputFormat );

        // When
        Response response = userAddService.createUser( "foo", req, "{ \"password\" : 1 }" );

        // Then
        assertThat( response.getStatus(), equalTo( 422 ) );
        String json = new String( (byte[]) response.getEntity() );
        assertNotNull( json );
        assertThat( json, containsString( "\"code\" : \"Neo.ClientError.Request.InvalidFormat\"" ) );
        assertThat( json, containsString( "\"message\" : \"Expected 'password' to be a string.\"" ) );
    }

    @Test
    public void shouldReturn422IfEmptyPassword() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( NEO4J_PRINCIPLE );

        AuthManager authManager = mock( AuthManager.class );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserAddService userAddService = new UserAddService( authManager, new JsonFormat(), outputFormat );

        // When
        Response response = userAddService.createUser( "foo", req, "{ \"password\" : \"\" }" );

        // Then
        assertThat( response.getStatus(), equalTo( 422 ) );
        String json = new String( (byte[]) response.getEntity() );
        assertNotNull( json );
        assertThat( json, containsString( "\"code\" : \"Neo.ClientError.Request.Invalid\"" ) );
        assertThat( json, containsString( "\"message\" : \"Password cannot be empty.\"" ) );
    }

    @Test
    public void shouldReturn500IfDuplicateUser() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( NEO4J_PRINCIPLE );

        AuthManager authManager = mock( AuthManager.class );
        when( authManager.newUser( "foo", "bar", true ) ).thenThrow( new IllegalUsernameException("The specified user already exists") );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserAddService userAddService = new UserAddService( authManager, new JsonFormat(), outputFormat );

        // When
        Response response = userAddService.createUser( "foo", req, "{ \"password\" : \"bar\" }" );

        // Then
        assertThat( response.getStatus(), equalTo( 500 ) );
        String json = new String( (byte[]) response.getEntity() );
        assertNotNull( json );
        assertThat( json, containsString( "\"code\" : \"Neo.ClientError.Request.Invalid\"" ) );
        assertThat( json, containsString( "\"message\" : \"The specified user already exists\"" ) );
    }
}
