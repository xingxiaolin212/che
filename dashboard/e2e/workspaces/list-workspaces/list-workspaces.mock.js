/*
 * Copyright (c) 2015-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 */
'use strict';

const buildWorkspacesData = function() {
    const workspacesNumber = 15,
          namespace = 'che',
          status = 'STOPPED',
          nowTimestamp = Math.floor(Date.now() / 1000),
          attributes = {
            created: nowTimestamp,
            stackId: 'stackId',
            updated: nowTimestamp
          },
          defaultEnvironment = 'default',
          environments = {
            [defaultEnvironment]: {
              machines: {
                'dev-machine': {
                  attributes: {
                    memoryLimitBytes: '2147483648'
                  },
                  servers: {},
                  agents: ['org.eclipse.che.terminal', 'org.eclipse.che.ws-agent', 'org.eclipse.che.ssh', 'org.eclipse.che.exec'],
                  recipe: {
                    location: 'eclipse/ubuntu_jdk8',
                    type: 'dockerimage'
                  }
                }
              }
            }
          };

    return Array.from(Array(workspacesNumber)).map((x, i) => {
      const id = `workspaceId${i}`,
            name = `workspaceName${i}`;
      return {id, name, namespace, status, defaultEnvironment, environments, attributes};
    });
};

const listWorkspacesMock = function(workspacesData) {

  angular.module('userDashboardMock', ['userDashboard', 'ngMockE2E'])
    .run(['$httpBackend', 'cheAPIBuilder', 'cheHttpBackendProvider', ($httpBackend, cheAPIBuilder, cheHttpBackendProvider) => {

      let cheBackend = cheHttpBackendProvider.buildBackend($httpBackend, cheAPIBuilder);

      const workspaces = workspacesData.map((data) => {
        let workspaceBuilder = cheAPIBuilder.getWorkspaceBuilder();
        workspaceBuilder = workspaceBuilder.withId(data.id).withNamespace(data.namespace).withName(data.name);
        if (data.status) {
          workspaceBuilder = workspaceBuilder.withStatus(data.status);
        }
        if (data.defaultEnvironment) {
          workspaceBuilder = workspaceBuilder.withDefaultEnvironment(data.defaultEnvironment);
        }
        if (data.environments) {
          workspaceBuilder = workspaceBuilder.withEnvironments(data.environments);
        }
        if (data.attributes) {
          workspaceBuilder = workspaceBuilder.withAttributes(data.attributes);
        }
        return workspaceBuilder.build();
      });

      cheBackend.addWorkspaces(workspaces);
      cheBackend.setup();

    }]);

};

exports.listWorkspacesMock = listWorkspacesMock;
exports.buildWorkspacesData = buildWorkspacesData;
