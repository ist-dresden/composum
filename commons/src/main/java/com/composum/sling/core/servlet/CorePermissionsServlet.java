package com.composum.sling.core.servlet;

import com.composum.sling.core.service.PermissionsService;
import com.composum.sling.core.util.XSS;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The service servlet to retrieve and/or check permissions of a path. The path is necessary only for
 * privilege checks; a 'path' parameter, a suffix or the requested resource is used (in this order).
 * parameters: 'member' and/or 'privilege' (possibly multiple), optional 'path';
 * each parameter can be a ',' separated list combined with OR; multiple parameters are combined with AND
 * response: {"result":true/false,"userId":...,"path":...,...matching permissions}
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Core Permissions Servlet",
                ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=sling/servlet/default",
                ServletResolverConstants.SLING_SERVLET_SELECTORS + "=cpm.permissions",
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
        }
)
public class CorePermissionsServlet extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(CorePermissionsServlet.class);

    @Reference
    private PermissionsService permissionsService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {
        Boolean checkResult = null;
        String userId = null;
        String path = null;
        List<String> memberOf = new ArrayList<>();
        List<String> privilege = new ArrayList<>();
        Session session = request.getResourceResolver().adaptTo(Session.class);
        if (session != null) {
            userId = session.getUserID();
            RequestPathInfo pathInfo = request.getRequestPathInfo();
            List<String> selectors = Arrays.asList(pathInfo.getSelectors());
            String[] memberValues = XSS.filter(request.getParameterValues("member"));
            if (memberValues != null) {
                for (String members : memberValues) {
                    if (checkResult == null || checkResult) {
                        String found = permissionsService.isMemberOfOne(session,
                                StringUtils.split(members, ","));
                        if (checkResult = (found != null)) {
                            memberOf.add(found);
                        }
                    }
                }
            }
            if (checkResult == null || checkResult) {
                String[] privilegeValues = XSS.filter(request.getParameterValues("privilege"));
                if (privilegeValues != null) {
                    path = XSS.filter(request.getParameter("path"));
                    if (StringUtils.isBlank(path)) {
                        path = XSS.filter(pathInfo.getSuffix());
                    }
                    if (StringUtils.isBlank(path)) {
                        Resource resource = request.getResource();
                        if (resource != null) {
                            path = resource.getPath();
                        }
                    }
                    if (StringUtils.isNotBlank(path)) {
                        for (String privileges : privilegeValues) {
                            if (checkResult == null || checkResult) {
                                String found = permissionsService.hasOneOfPrivileges(session, path,
                                        StringUtils.split(privileges, ","));
                                if (checkResult = (found != null)) {
                                    privilege.add(found);
                                }
                            }
                        }
                    }
                }
            }
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json; charset=UTF-8");
        JsonWriter writer = new JsonWriter(response.getWriter());
        writer.beginObject();
        writer.name("result").value(checkResult != null && checkResult);
        if (StringUtils.isNotBlank(userId)) {
            writer.name("userId").value(userId);
        }
        if (StringUtils.isNotBlank(path)) {
            writer.name("path").value(path);
        }
        if (memberOf.size() > 0) {
            writer.name("memberOf").beginArray();
            for (String value : memberOf) {
                writer.value(value);
            }
            writer.endArray();
        }
        if (privilege.size() > 0) {
            writer.name("privilege").beginArray();
            for (String value : privilege) {
                writer.value(value);
            }
            writer.endArray();
        }
        writer.endObject();
    }
}
