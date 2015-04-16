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

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.server.rest.repr.AuthorizationRepresentation;
import org.neo4j.server.rest.repr.BadInputException;
import org.neo4j.server.rest.repr.ExceptionRepresentation;
import org.neo4j.server.rest.repr.InputFormat;
import org.neo4j.server.rest.repr.OutputFormat;
import org.neo4j.server.rest.transactional.error.Neo4jError;
import org.neo4j.server.security.auth.AuthManager;
import org.neo4j.server.security.auth.User;
import org.neo4j.server.security.auth.exception.IllegalUsernameException;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.neo4j.server.rest.web.CustomStatusType.UNPROCESSABLE;

/**
 * Enable users to be added.
 *
 * @author Robin Bramley
 */
@Path( "/useradd" )
public class UserAddService
{
    private final AuthManager authManager;
    private final InputFormat input;
    private final OutputFormat output;
    public static final String NEO4J_USER = "neo4j";
    public static final String PASSWORD = "password";

    public UserAddService( @Context AuthManager authManager,
        @Context InputFormat input, @Context OutputFormat output )
    {
        this.authManager = authManager;
        this.input = input;
        this.output = output;
    }

    @POST
    @Path("/{username}")
    public Response createUser( @PathParam("username") String username, @Context HttpServletRequest req, String payload )
    {
        Principal principal = req.getUserPrincipal();
        if ( principal == null || !principal.getName().equals( NEO4J_USER ) )
        {
            return output.notFound();
        }

        final Map<String, Object> deserialized;
        try
        {
            deserialized = input.readMap( payload );
        } catch ( BadInputException e )
        {
            return output.response( BAD_REQUEST, new ExceptionRepresentation(
            new Neo4jError( Status.Request.InvalidFormat, e.getMessage() ) ) );
        }

        Object o = deserialized.get( PASSWORD );
        if ( o == null )
        {
            return output.response( UNPROCESSABLE, new ExceptionRepresentation(
            new Neo4jError( Status.Request.InvalidFormat, String.format( "Required parameter '%s' is missing.", PASSWORD ) ) ) );
        }
        if ( !( o instanceof String ) )
        {
            return output.response( UNPROCESSABLE, new ExceptionRepresentation(
            new Neo4jError( Status.Request.InvalidFormat, String.format( "Expected '%s' to be a string.", PASSWORD ) ) ) );
        }
        String newPassword = (String) o;
        if ( newPassword.length() == 0 )
        {
            return output.response( UNPROCESSABLE, new ExceptionRepresentation(
            new Neo4jError( Status.Request.Invalid, "Password cannot be empty." ) ) );
        }

        final User newUser;
        try
        {
            newUser = authManager.newUser( username, newPassword, true );
        } catch ( IOException | IllegalUsernameException e )
        {
            return output.serverErrorWithoutLegacyStacktrace( e );
        }

        if (newUser == null)
        {
            return output.notFound();
        }

        return output.ok();
    }
}
