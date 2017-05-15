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
import {CheService} from '../api/che-service.factory';

const DEFAULT_BRANDING = {
  title: 'Eclipse Che',
  name: 'Eclipse Che',
  logoFile: 'che-logo.svg',
  logoTextFile: 'che-logo-text.svg',
  favicon: 'favicon.ico',
  loader: 'loader.svg',
  ideResources: '/_app/',
  helpPath: 'https://www.eclipse.org/che/',
  helpTitle: 'Community',
  supportEmail: 'wish@codenvy.com',
  oauthDocs: 'Configure OAuth in the che.properties file.',
  cli : {
    configName : 'che.env',
    name : 'CHE'
  },
  docs : {
    stack: '/docs/getting-started/runtime-stacks/index.html',
    workspace: '/docs/getting-started/intro/index.html'
  }
};


/**
 * This class is handling the branding data in Che
 * @author Florent Benoit
 */
export class CheBranding {
  promise: ng.IPromise;
/*  private $q: ng.IQService;*/
  private $rootScope: che.IRootScopeService;
  private $http: ng.IHttpService;
  private cheService: CheService;
  private deferred: ng.IDeferred;
  private docs: { stack: string; workspace: string };
  private callbacks: Map<string, Function> = new Map();

    /**
     * Default constructor that is using resource
     * @ngInject for Dependency injection
     */
    constructor($http: ng.IHttpService, $rootScope: che.IRootScopeService, $q: ng.IQService, cheService: CheService) {
        this.$http = $http;
        this.$rootScope = $rootScope;
        this.deferred = $q.defer();
        this.promise = this.deferred.promise;
        this.cheService = cheService;
        this.updateData();
        this.getVersion();
    }

    getVersion() {
      this.cheService.fetchServicesInfo().then(() => {
        let info = this.cheService.getServicesInfo();
        this.$rootScope.productVersion = (info && info.implementationVersion) ? info.implementationVersion : '';
      });
    }

    updateData() {
        const assetPrefix = 'assets/branding/';
        this.$http.get(assetPrefix + 'product.json').then((branding: {data: any}) => {
            const brandingData = branding.data;
            this.$rootScope.branding = {
                title: brandingData.title,
                name: brandingData.name,
                logoURL: assetPrefix + brandingData.logoFile,
                logoText: assetPrefix + brandingData.logoTextFile,
                favicon : assetPrefix + brandingData.favicon,
                loaderURL: assetPrefix + brandingData.loader,
                ideResourcesPath : brandingData.ideResources,
                helpPath : brandingData.helpPath,
                helpTitle : brandingData.helpTitle,
                supportEmail: brandingData.supportEmail,
                oauthDocs: brandingData.oauthDocs,
                docs: {
                  stack: brandingData.docs && brandingData.docs.stack ? brandingData.docs.stack : this.docs.stack,
                  workspace: brandingData.docs && brandingData.docs.workspace ? brandingData.docs.workspace :  this.docs.workspace
                }
            };

            this.productName = this.$rootScope.branding.title;
            this.name = this.$rootScope.branding.name;
            this.productFavicon = this.$rootScope.branding.productFavicon;
            this.productLogo = this.$rootScope.branding.logoURL;
            this.productLogoText = this.$rootScope.branding.logoText;
            this.ideResourcesPath = this.$rootScope.branding.ideResourcesPath;
            this.helpPath = this.$rootScope.branding.helpPath;
            this.helpTitle = this.$rootScope.branding.helpTitle;
            this.supportEmail = this.$rootScope.branding.supportEmail;
            this.oauthDocs = this.$rootScope.branding.oauthDocs;
            if (!this.$rootScope.branding.cli) {
              this.cli = {
                configName : this.name + 'env file',
                name : 'PRODUCT_'
              };
            } else {
              this.cli = this.$rootScope.branding.cli;
            }
            this.docs = this.$rootScope.branding.docs;
            this.deferred.resolve(this.$rootScope.branding);
        }).finally(() => {


          this.callbacks.forEach((callback: Function) => {
            callback(this.$rootScope.branding);
          });
        });

    }

  /**
   * Registers a callback function.
   * @param callbackId {string}
   * @param callback {Function}
   */
  registerCallback(callbackId: string, callback: Function): void {
    this.callbacks.set(callbackId, callback);
  }

  /**
   * Unregisters the callback function by Id.
   * @param callbackId {string}
   */
  unregisterCallback(callbackId: string): void {
    if (!this.callbacks.has(callbackId)) {
      return;
    }
    this.callbacks.delete(callbackId);
  }

    getName() {
      return this.name;
    }

    getProductName() {
        return this.productName;
    }

    getProductLogo() {
        return this.productLogo;
    }

    getProductFavicon() {
        return this.productFavicon;
    }

    getIdeResourcesPath() {
        return this.ideResourcesPath;
    }

    getProductHelpPath() {
        return this.helpPath;
    }

    getProductHelpTitle() {
        return this.helpTitle;
    }

    getProductSupportEmail() {
        return this.supportEmail;
    }

    getCLI() {
        return this.cli;
    }

  /**
   * Returns object with docs URLs.
   * @returns {{stack: string, workspace: string}}
   */
  getDocs(): { stack: string; workspace: string } {
    return this.docs;
  }

}

