/*
 * Copyright 2010, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.zanata.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;

import org.hibernate.exception.ConstraintViolationException;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Begin;
import javax.inject.Inject;
import org.apache.deltaspike.core.api.exclude.Exclude;
import org.apache.deltaspike.core.api.projectstage.ProjectStage;
import javax.inject.Named;
import org.jboss.seam.core.Conversation;
import org.jboss.seam.international.StatusMessages;
import org.zanata.dao.AccountDAO;
import org.zanata.dao.PersonDAO;
import org.zanata.i18n.Messages;
import org.zanata.seam.security.IdentityManager;
import org.zanata.service.EmailService;
import org.zanata.service.UserAccountService;
import org.zanata.ui.AbstractListFilter;

import lombok.Getter;
import org.zanata.ui.faces.FacesMessages;

import static javax.faces.application.FacesMessage.SEVERITY_ERROR;
import static org.jboss.seam.ScopeType.PAGE;
import static org.jboss.seam.annotations.Install.APPLICATION;

/**
 * Extension of Seam management's UserAction class' behaviour.
 *
 * @see {@link org.jboss.seam.security.management.action.UserAction}
 * @author Carlos Munoz <a
 *         href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
@Named("org.jboss.seam.security.management.userAction")
@javax.faces.bean.ViewScoped
public class UserAction implements Serializable {
    private static final long serialVersionUID = 1L;

    @Inject
    private IdentityManager identityManager;

    @Inject
    private EntityManager entityManager;

    @Inject
    private FacesMessages facesMessages;

    @Inject
    private Messages msgs;

    @Inject
    private UserAccountService userAccountServiceImpl;

    @Inject
    private EmailService emailServiceImpl;

    @Inject
    private PersonDAO personDAO;

    private String originalUsername;

    @Getter
    private AbstractListFilter<String> userFilter =
            new AbstractListFilter<String>() {
                AccountDAO accountDAO =
                        (AccountDAO) Component.getInstance(AccountDAO.class);

                @Override
                protected List<String> fetchRecords(int start, int max,
                        String filter) {
                    return accountDAO.getUserNames(filter, start, max);
                }

                @Override
                protected long fetchTotalRecords(String filter) {
                    return accountDAO.getUserCount(filter);
                }
            };
    private List<String> roles;
    private String username;
    private String password;
    private String confirm;
    private boolean enabled;

    public void deleteUser(String userName) {
        try {
            identityManager.deleteUser(userName);
            // NB: Need to call flush here to be able to catch the persistence
            // exception, otherwise it would be caught by Seam.
            entityManager.flush();
            userFilter.reset();
        } catch (PersistenceException e) {
            if (e.getCause() instanceof ConstraintViolationException) {
                facesMessages
                        .addFromResourceBundle(SEVERITY_ERROR,
                                "jsf.UserManager.delete.constraintViolation.error");
            }
        }
    }

    public String getEmail(String username) {
        return personDAO.findEmail(username);
    }

    public String getName(String username) {
        return personDAO.findByUsername(username).getName();
    }

    public void loadUser() {
        roles = identityManager.getGrantedRoles(username);
        enabled = identityManager.isUserEnabled(username);
        originalUsername = username;
    }

    public String save() {
        boolean usernameChanged = false;
        String newUsername = getUsername();

        // Allow user name changes when editing
        if (!originalUsername.equals(newUsername)) {
            if (isNewUsernameValid(newUsername)) {
                userAccountServiceImpl.editUsername(originalUsername,
                        newUsername);
                usernameChanged = true;
            } else {
                facesMessages.addToControl("username",
                        msgs.format("jsf.UsernameNotAvailable",
                                getUsername()));
                setUsername(originalUsername); // reset the username field
                return "failure";
            }
        }

        String saveResult;

        saveResult = saveExistingUser();

        if (usernameChanged) {
            String email = getEmail(newUsername);
            String message = emailServiceImpl.sendUsernameChangedEmail(
                    email, newUsername);
            facesMessages.addGlobal(message);
        }
        return saveResult;
    }

    private String saveExistingUser() {
        // Check if a new password has been entered
        if (password != null && !"".equals(password)) {
            if (!password.equals(confirm)) {
                facesMessages.addToControl("password", "Passwords do not match");
                return "failure";
            } else {
                identityManager.changePassword(username, password);
            }
        }

        List<String> grantedRoles = identityManager.getGrantedRoles(username);

        if (grantedRoles != null) {
            for (String role : grantedRoles) {
                if (!roles.contains(role)) identityManager.revokeRole(username, role);
            }
        }

        for (String role : roles) {
            if (grantedRoles == null || !grantedRoles.contains(role)) {
                identityManager.grantRole(username, role);
            }
        }

        if (enabled) {
            identityManager.enableUser(username);
        } else {
            identityManager.disableUser(username);
        }

        return "success";
    }

    /**
     * Validate that a user name is not already in the system, by another
     * account
     */
    private boolean isNewUsernameValid(String username) {
        try {
            entityManager
                    .createQuery("from HAccount a where a.username = :username")
                    .setParameter("username", username).getSingleResult();
            return false;
        } catch (NoResultException e) {
            // pass
            return true;
        }
    }

    public String cancel() {
        return "success";
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirm() {
        return confirm;
    }

    public void setConfirm(String confirm) {
        this.confirm = confirm;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
