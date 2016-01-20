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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.server.rest.repr.OutputFormat;
import org.neo4j.server.rest.transactional.error.Neo4jError;
import org.neo4j.server.security.auth.AuthManager;
import org.neo4j.server.security.auth.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.neo4j.server.rest.web.CustomStatusType.UNPROCESSABLE;

/**
 * Enable users to be deleted.
 *
 * @author Robin Bramley
 */
@Path( "/userdel" )
public class UserDeleteService
{
    private static final Logger logger = LoggerFactory.getLogger(org.neo4j.extension.server.unmanaged.UserDeleteService.class);

    private final AuthManager authManager;
    private final OutputFormat output;
    public static final String NEO4J_USER = "neo4j";

    public UserDeleteService( @Context AuthManager authManager,
        @Context OutputFormat output )
    {
        this.authManager = authManager;
        this.output = output;
    }

    @GET
    @Path("/{username}")
    public Response deleteUser( final @PathParam("username") String username, @Context HttpServletRequest req )
    {
        Principal principal = req.getUserPrincipal();
        if ( principal == null || !principal.getName().equals( NEO4J_USER ) )
        {
            return output.notFound();
        }

        final boolean deleted;
        try
        {
            deleted = authManager.deleteUser( username );
            logger.info("User {} was deleted by {}: {}", username, principal.getName(), deleted);
        } catch ( IOException e )
        {
            return output.serverErrorWithoutLegacyStacktrace( e );
        }

        if (deleted)
        {
            return output.ok();
        } else {
            return output.notFound();
        }
    }
}
