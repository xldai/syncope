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
package org.apache.syncope.client.console;

import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.XMLRolesReader;
import org.apache.syncope.client.console.pages.Configuration;
import org.apache.syncope.client.console.pages.InfoModalPage;
import org.apache.syncope.client.console.pages.Login;
import org.apache.syncope.client.console.pages.Logout;
import org.apache.syncope.client.console.pages.Reports;
import org.apache.syncope.client.console.pages.Resources;
import org.apache.syncope.client.console.pages.Roles;
import org.apache.syncope.client.console.pages.Schema;
import org.apache.syncope.client.console.pages.Tasks;
import org.apache.syncope.client.console.pages.Todo;
import org.apache.syncope.client.console.pages.UserSelfModalPage;
import org.apache.syncope.client.console.pages.Users;
import org.apache.syncope.client.console.pages.WelcomePage;
import org.apache.syncope.client.console.resources.FilesystemResource;
import org.apache.syncope.client.console.resources.WorkflowDefGETResource;
import org.apache.syncope.client.console.resources.WorkflowDefPUTResource;
import org.apache.syncope.client.console.rest.UserSelfRestClient;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.Session;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authorization.IUnauthorizedComponentInstantiationListener;
import org.apache.wicket.authorization.UnauthorizedInstantiationException;
import org.apache.wicket.authroles.authorization.strategies.role.IRoleCheckingStrategy;
import org.apache.wicket.authroles.authorization.strategies.role.RoleAuthorizationStrategy;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.ContextRelativeResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * SyncopeApplication class.
 */
public class SyncopeApplication
        extends WebApplication
        implements IUnauthorizedComponentInstantiationListener, IRoleCheckingStrategy, Serializable {

    private static final long serialVersionUID = -2920378752291913495L;

    public static final String IMG_PREFIX = "/img/menu/";

    public static final String IMG_NOTSEL = "notsel/";

    private static final String ACTIVITI_MODELER_CONTEXT = "activiti-modeler";

    private static final int EDIT_PROFILE_WIN_HEIGHT = 550;

    private static final int EDIT_PROFILE_WIN_WIDTH = 800;

    @Override
    protected void init() {
        super.init();

        getComponentInstantiationListeners().add(new SpringComponentInjector(this));

        getResourceSettings().setThrowExceptionOnMissingResource(true);

        getSecuritySettings().setAuthorizationStrategy(new RoleAuthorizationStrategy(this));
        getSecuritySettings().setUnauthorizedComponentInstantiationListener(this);

        getMarkupSettings().setStripWicketTags(true);
        getMarkupSettings().setCompressWhitespace(true);

        getRequestCycleListeners().add(new SyncopeRequestCycleListener());

        final String activitiModelerDirectory = WebApplicationContextUtils.getWebApplicationContext(
                WebApplication.get().getServletContext()).getBean("activitiModelerDirectory", String.class);
        mountResource("/" + ACTIVITI_MODELER_CONTEXT, new ResourceReference(ACTIVITI_MODELER_CONTEXT) {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new FilesystemResource(ACTIVITI_MODELER_CONTEXT, activitiModelerDirectory);
            }

        });
        mountResource("/workflowDefGET", new ResourceReference("workflowDefGET") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new WorkflowDefGETResource();
            }
        });
        mountResource("/workflowDefPUT", new ResourceReference("workflowDefPUT") {

            private static final long serialVersionUID = -128426276529456602L;

            @Override
            public IResource getResource() {
                return new WorkflowDefPUTResource();
            }
        });
    }

    public void setupNavigationPanel(final WebPage page, final XMLRolesReader xmlRolesReader, final boolean notsel) {
        final ModalWindow infoModal = new ModalWindow("infoModal");
        page.add(infoModal);
        infoModal.setInitialWidth(350);
        infoModal.setInitialHeight(300);
        infoModal.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        infoModal.setCookieName("infoModal");
        infoModal.setPageCreator(new ModalWindow.PageCreator() {

            private static final long serialVersionUID = -7834632442532690940L;

            @Override
            public Page createPage() {
                return new InfoModalPage();
            }
        });

        final AjaxLink<Page> infoLink = new AjaxLink<Page>("infoLink") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                infoModal.show(target);
            }
        };
        page.add(infoLink);

        BookmarkablePageLink<Page> schemaLink = new BookmarkablePageLink<>("schema", Schema.class);
        MetaDataRoleAuthorizationStrategy.authorize(
                schemaLink, WebPage.ENABLE, xmlRolesReader.getEntitlement("Schema", "list"));
        page.add(schemaLink);
        schemaLink.add(new Image("schemaIcon", new ContextRelativeResource(IMG_PREFIX + (notsel
                ? IMG_NOTSEL
                : StringUtils.EMPTY) + "schema" + Constants.PNG_EXT)));

        BookmarkablePageLink<Page> usersLink = new BookmarkablePageLink<>("users", Users.class);
        MetaDataRoleAuthorizationStrategy.authorize(
                usersLink, WebPage.ENABLE, xmlRolesReader.getEntitlement("Users", "list"));
        page.add(usersLink);
        usersLink.add(new Image("usersIcon", new ContextRelativeResource(IMG_PREFIX + (notsel
                ? IMG_NOTSEL
                : StringUtils.EMPTY) + "users" + Constants.PNG_EXT)));

        BookmarkablePageLink<Page> rolesLink = new BookmarkablePageLink<>("roles", Roles.class);
        MetaDataRoleAuthorizationStrategy.authorize(
                rolesLink, WebPage.ENABLE, xmlRolesReader.getEntitlement("Roles", "list"));
        page.add(rolesLink);
        rolesLink.add(new Image("rolesIcon", new ContextRelativeResource(IMG_PREFIX + (notsel
                ? IMG_NOTSEL
                : StringUtils.EMPTY) + "roles" + Constants.PNG_EXT)));

        BookmarkablePageLink<Page> resourcesLink = new BookmarkablePageLink<>("resources", Resources.class);
        MetaDataRoleAuthorizationStrategy.authorize(
                resourcesLink, WebPage.ENABLE, xmlRolesReader.getEntitlement("Resources", "list"));
        page.add(resourcesLink);
        resourcesLink.add(new Image("resourcesIcon", new ContextRelativeResource(IMG_PREFIX + (notsel
                ? IMG_NOTSEL
                : StringUtils.EMPTY) + "resources" + Constants.PNG_EXT)));

        BookmarkablePageLink<Page> todoLink = new BookmarkablePageLink<>("todo", Todo.class);
        MetaDataRoleAuthorizationStrategy.authorize(
                todoLink, WebPage.ENABLE, xmlRolesReader.getEntitlement("Approval", "list"));
        page.add(todoLink);
        todoLink.add(new Image("todoIcon", new ContextRelativeResource(IMG_PREFIX + (notsel
                ? IMG_NOTSEL
                : StringUtils.EMPTY) + "todo" + Constants.PNG_EXT)));

        BookmarkablePageLink<Page> reportLink = new BookmarkablePageLink<>("reports", Reports.class);
        MetaDataRoleAuthorizationStrategy.authorize(
                reportLink, WebPage.ENABLE, xmlRolesReader.getEntitlement("Reports", "list"));
        page.add(reportLink);
        reportLink.add(new Image("reportsIcon", new ContextRelativeResource(IMG_PREFIX + (notsel
                ? IMG_NOTSEL
                : StringUtils.EMPTY) + "reports" + Constants.PNG_EXT)));

        BookmarkablePageLink<Page> configurationLink = new BookmarkablePageLink<>("configuration",
                Configuration.class);
        MetaDataRoleAuthorizationStrategy.authorize(
                configurationLink, WebPage.ENABLE, xmlRolesReader.getEntitlement("Configuration", "list"));
        page.add(configurationLink);
        configurationLink.add(new Image("configurationIcon", new ContextRelativeResource(IMG_PREFIX + (notsel
                ? IMG_NOTSEL
                : StringUtils.EMPTY) + "configuration" + Constants.PNG_EXT)));

        BookmarkablePageLink<Page> taskLink = new BookmarkablePageLink<>("tasks", Tasks.class);
        MetaDataRoleAuthorizationStrategy.authorize(
                taskLink, WebPage.ENABLE, xmlRolesReader.getEntitlement("Tasks", "list"));
        page.add(taskLink);
        taskLink.add(new Image("tasksIcon", new ContextRelativeResource(IMG_PREFIX + (notsel
                ? IMG_NOTSEL
                : StringUtils.EMPTY) + "tasks" + Constants.PNG_EXT)));

        page.add(new BookmarkablePageLink<Page>("logout", Logout.class));
    }

    public void setupEditProfileModal(final WebPage page, final UserSelfRestClient userSelfRestClient) {
        // Modal window for editing user profile
        final ModalWindow editProfileModalWin = new ModalWindow("editProfileModal");
        editProfileModalWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editProfileModalWin.setInitialHeight(EDIT_PROFILE_WIN_HEIGHT);
        editProfileModalWin.setInitialWidth(EDIT_PROFILE_WIN_WIDTH);
        editProfileModalWin.setCookieName("edit-profile-modal");
        page.add(editProfileModalWin);

        final AjaxLink<Page> editProfileLink = new AjaxLink<Page>("editProfileLink") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final UserTO userTO;
                if (SyncopeSession.get().isAuthenticated()) {
                    try {
                        userTO = userSelfRestClient.read();
                    } catch (Exception e) {
                        throw new WicketRuntimeException(e);
                    }
                } else {
                    userTO = new UserTO();
                }

                editProfileModalWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        return new UserSelfModalPage(page.getPageReference(), editProfileModalWin, userTO);
                    }
                });

                editProfileModalWin.show(target);
            }
        };

        editProfileLink.add(new Label("username", SyncopeSession.get().getUsername()));

        if ("admin".equals(SyncopeSession.get().getUsername())) {
            editProfileLink.setEnabled(false);
        }

        page.add(editProfileLink);
    }

    @Override
    public Session newSession(final Request request, final Response response) {
        return new SyncopeSession(request);
    }

    @Override
    public Class<? extends Page> getHomePage() {
        return SyncopeSession.get().isAuthenticated() ? WelcomePage.class : Login.class;
    }

    @Override
    public void onUnauthorizedInstantiation(final Component component) {
        SyncopeSession.get().invalidate();

        if (component instanceof Page) {
            throw new UnauthorizedInstantiationException(component.getClass());
        }

        throw new RestartResponseAtInterceptPageException(Login.class);
    }

    @Override
    public boolean hasAnyRole(final org.apache.wicket.authroles.authorization.strategies.role.Roles roles) {
        return SyncopeSession.get().hasAnyRole(roles);
    }
}
