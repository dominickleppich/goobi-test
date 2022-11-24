/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information.
 *             - https://goobi.io
 *             - https://www.intranda.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Linking this library statically or dynamically with other modules is making a combined work based on this library. Thus, the terms and conditions
 * of the GNU General Public License cover the whole combination. As a special exception, the copyright holders of this library give you permission to
 * link this library with independent modules to produce an executable, regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that you also meet, for each linked independent module, the terms and
 * conditions of the license of that module. An independent module is a module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but you are not obliged to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */
package org.goobi.api.rest;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import org.goobi.beans.User;
import org.goobi.managedbeans.LoginBean;

import com.auth0.jwt.interfaces.DecodedJWT;

import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.forms.SessionForm;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.JwtHelper;
import de.sub.goobi.persistence.managers.UserManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Path("/login")
public class Login {
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    @Inject
    private SessionForm sessionForm;

    @POST
    @Path("/openid")
    @Operation(summary = "OpenID connect callback", description = "Verifies an openID claim and starts a session for the user")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Bad request")
    @ApiResponse(responseCode = "500", description = "Internal error")
    public void openIdLogin(@FormParam("error") String error, @FormParam("id_token") String idToken) throws IOException {
        ConfigurationHelper config = ConfigurationHelper.getInstance();
        String clientID = config.getOIDCClientID();
        String nonce = (String) servletRequest.getSession().getAttribute("openIDNonce");
        if (error == null) {
            // no error - we should have a token. Verify it.
            DecodedJWT jwt = JwtHelper.verifyOpenIdToken(idToken);
            if (jwt != null) {
                // now check if the nonce is the same as in the old session
                if (nonce.equals(jwt.getClaim("nonce").asString()) && clientID.equals(jwt.getClaim("aud").asString())) {
                    //all OK, login the user
                    HttpSession session = servletRequest.getSession();
                    LoginBean userBean = Helper.getLoginBeanFromSession(session);

                    // get the user by the configured claim from the JWT
                    String login = jwt.getClaim(config.getOIDCIdClaim()).asString();
                    log.debug("logging in user " + login);
                    User user = UserManager.getUserBySsoId(login);
                    if (user == null) {
                        userBean.setSsoError("Could not find user in Goobi database. Please contact your admin to add your SSO ID to the database.");
                        servletResponse.sendRedirect("/goobi/uii/logout.xhtml");
                        return;
                    }
                    userBean.setSsoError(null);
                    user.lazyLoad();
                    userBean.setMyBenutzer(user);
                    userBean.setRoles(user.getAllUserRoles());
                    userBean.setMyBenutzer(user);
                    //add the user to the sessionform that holds information about all logged in users
                    sessionForm.updateSessionUserName(servletRequest.getSession(), user);
                } else {
                    if (!nonce.equals(jwt.getClaim("nonce").asString())) {
                        log.error("nonce does not match. Not logging user in");
                    }
                    if (!clientID.equals(jwt.getClaim("aud").asString())) {
                        log.error("clientID does not match aud. Not logging user in");
                    }
                }
            } else {
                log.error("could not verify JWT");
            }
        } else {
            log.error(error);
        }
        servletResponse.sendRedirect("/goobi/index.xhtml");
    }

    @GET
    @Path("/header")
    @Operation(summary = "Header login", description = "Checks a configurable header for a username and logs in the user if it is found in the DB")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "500", description = "Internal error")
    public String apacheHeaderLogin() throws IOException {
        log.debug(LoginBean.LOGIN_LOG_PREFIX + "User tries to login via SSO Login.");
        ConfigurationHelper config = ConfigurationHelper.getInstance();
        if (!config.isEnableHeaderLogin()) {
            log.debug(LoginBean.LOGIN_LOG_PREFIX + "SSO Login is deactivated. SSO Login trial of user is aborted.");
            return "";
        }
        log.trace(LoginBean.LOGIN_LOG_PREFIX + "SSO Login is activated. User authentication is starting.");
        //the header we read the ssoID from is configurable
        String ssoHeaderName = config.getSsoHeaderName();
        log.debug(LoginBean.LOGIN_LOG_PREFIX + "Header name " + ssoHeaderName + " is used.");
        String ssoId = null;
        if ("header".equalsIgnoreCase(config.getSsoParameterType())) {
            log.trace(LoginBean.LOGIN_LOG_PREFIX + "'header' option is used for SSO Login.");
            ssoId = servletRequest.getHeader(ssoHeaderName);
        } else {
            log.trace(LoginBean.LOGIN_LOG_PREFIX + "'attribute' option is used for SSO Login.");
            ssoId = (String) servletRequest.getAttribute(ssoHeaderName);
        }
        LoginBean userBean = Helper.getLoginBeanFromSession(servletRequest.getSession());
        User user = UserManager.getUserBySsoId(ssoId);
        if (user == null) {
            log.debug(LoginBean.LOGIN_LOG_PREFIX + "There is no user with ssoId \"" + ssoId + "\".");
            userBean.setSsoError("Could not find user in Goobi database. Please contact your admin to add your SSO ID to the database.");
            servletResponse.sendRedirect("/goobi/uii/logout.xhtml");
            return "";
        }
        log.debug(LoginBean.LOGIN_LOG_PREFIX + "User \"" + user.getLogin() + "\" can be logged in via SSO:");
        userBean.setSsoError(null);
        user.lazyLoad();
        userBean.setMyBenutzer(user);
        userBean.setRoles(user.getAllUserRoles());
        userBean.setMyBenutzer(user);
        log.trace(LoginBean.LOGIN_LOG_PREFIX + "User is added to the session bean now.");
        //add the user to the sessionform that holds information about all logged in users
        sessionForm.updateSessionUserName(servletRequest.getSession(), user);
        log.debug(LoginBean.LOGIN_LOG_PREFIX + "Redirecting user to start page or dashboard.");
        servletResponse.sendRedirect("/goobi/index.xhtml");
        return "";
    }
}
