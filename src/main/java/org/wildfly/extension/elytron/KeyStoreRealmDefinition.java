/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.elytron;

import static org.wildfly.extension.elytron.ElytronDefinition.commonDependencies;
import static org.wildfly.extension.elytron.KeyStoreDefinition.KEY_STORE_UTIL;

import java.security.KeyStore;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.RestartParentWriteAttributeHandler;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.security.auth.provider.SecurityRealm;

/**
 * A {@link ResourceDefinition} for a {@link SecurityRealm} backed by a {@link KeyStore}.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class KeyStoreRealmDefinition extends SimpleResourceDefinition {

    static final ServiceUtil<SecurityRealm> REALM_SERVICE_UTIL = ServiceUtil.newInstance(ElytronDescriptionConstants.KEYSTORE_REALM, SecurityRealm.class);

    static final SimpleAttributeDefinition KEYSTORE = new SimpleAttributeDefinitionBuilder(ElytronDescriptionConstants.KEYSTORE, ModelType.STRING, false)
        .setAllowExpression(true)
        .setMinSize(1)
        .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
        .build();

    private static final AbstractAddStepHandler ADD = new RealmAddHandler();
    private static final OperationStepHandler REMOVE = new RealmRemoveHandler(ADD);

    KeyStoreRealmDefinition() {
        super(PathElement.pathElement(ElytronDescriptionConstants.KEYSTORE_REALM),
                ElytronExtension.getResourceDescriptionResolver(ElytronDescriptionConstants.KEYSTORE_REALM),
                ADD, REMOVE,
                OperationEntry.Flag.RESTART_RESOURCE_SERVICES,
                OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(KEYSTORE, null, new WriteAttributeHandler());
    }

    private static class RealmAddHandler extends AbstractAddStepHandler {

        private RealmAddHandler() {
            super(KEYSTORE);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
                throws OperationFailedException {
            ServiceTarget serviceTarget = context.getServiceTarget();
            ServiceName realmName = REALM_SERVICE_UTIL.serviceName(operation);
            KeyStoreRealmService keyStoreRealmService = new KeyStoreRealmService();

            ServiceBuilder<SecurityRealm> serviceBuilder = serviceTarget.addService(realmName, keyStoreRealmService);

            KEY_STORE_UTIL.addInjection(serviceBuilder, keyStoreRealmService.getKeyStoreInjector(), KEYSTORE.resolveModelAttribute(context, model).asString());
            commonDependencies(serviceBuilder)
                .setInitialMode(Mode.ACTIVE)
                .install();
        }

    }

    private static class RealmRemoveHandler extends ServiceRemoveStepHandler {

        public RealmRemoveHandler(AbstractAddStepHandler addOperation) {
            super(addOperation);
        }

        @Override
        protected ServiceName serviceName(String name) {
            return REALM_SERVICE_UTIL.serviceName(name);
        }

    }

    private static class WriteAttributeHandler extends RestartParentWriteAttributeHandler {

        WriteAttributeHandler() {
            super(ElytronDescriptionConstants.KEYSTORE_REALM, KEYSTORE);
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress arg0) {
            return null;
        }
    }

}