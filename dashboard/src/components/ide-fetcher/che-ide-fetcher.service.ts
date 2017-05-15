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
import {CheBranding} from '../branding/che-branding.factory';

const IDE_FETCHER_CALLBACK_ID = 'cheIdeFetcherCallback';

/**
 * Provides a way to download IDE .js and then cache it before trying to load the IDE
 * @author Florent Benoit
 */
export class CheIdeFetcher {
  private $http: ng.IHttpService;
  private $window: ng.IWindowService;
  private cheBranding: CheBranding;
  private userAgent: string;

  /**
   * Default constructor that is using resource injection
   * @ngInject for Dependency injection
   */
  constructor($http: ng.IHttpService, $window: ng.IWindowService, cheBranding: CheBranding) {
    this.$http = $http;
    this.$window = $window;
    this.cheBranding = cheBranding;

    this.userAgent = this.getUserAgent();

    const callback = () => {
      this.findMappingFile();
      this.cheBranding.unregisterCallback(IDE_FETCHER_CALLBACK_ID);
    };
    this.cheBranding.registerCallback(IDE_FETCHER_CALLBACK_ID, callback.bind(this));
  }


  getUserAgent(): string {
    const userAgent = this.$window.navigator.userAgent.toLowerCase();
    const docMode = (<any>this.$window.document).documentMode;

    if (userAgent.indexOf('webkit') !== -1) {
      return 'safari';
    } else if (userAgent.indexOf('msie') !== -1) {
      if (docMode >= 10 && docMode < 11) {
        return 'ie10';
      } else if (docMode >= 9 && docMode < 11) {
        return 'ie9';
      } else if (docMode >= 8 && docMode < 11) {
        return 'ie8';
      }
    } else if (userAgent.indexOf('gecko') !== -1) {
      return 'gecko1_8';
    }

    return 'unknown';
  }


  findMappingFile(): void {
    // get the content of the compilation mapping file
    const randVal = Math.floor((Math.random() * 1000000) + 1);
    const resourcesPath = this.cheBranding.getIdeResourcesPath();
    if (!resourcesPath) {
      console.log('Unable to get IDE resources path');
      return;
    }

    const fileMappinUrl = `${resourcesPath}compilation-mappings.txt?uid=${randVal}`;

    this.$http.get(fileMappinUrl).then((response: {data: any}) => {
      let urlToLoad = this.getIdeUrlMappingFile(response.data);
      // load the url
      if (urlToLoad != null) {
        console.log('Preloading IDE javascript', urlToLoad);
        this.$http.get(urlToLoad, { cache: true});
      } else {
        console.error('Unable to find the IDE javascript file to cache');
      }
    }, (error: any) => {
      console.log('unable to find compilation mapping file', error);
    });
  }

  getIdeUrlMappingFile(data: string): string {
    let javascriptFilename: string;
    const mappings = data.split(new RegExp('^\\n', 'gm'));
    const isPasses = mappings.some((mapping: string) => {
      const subMappings = mapping.split('\n');
      const userAgent = subMappings.find((subMapping: string) => {
        return subMapping.startsWith('user.agent ');
      }).split(' ')[1];
      javascriptFilename = subMappings.find((subMapping: string) => {
        return subMapping.endsWith('.cache.js');
      });
      return javascriptFilename && userAgent && this.userAgent === userAgent;
    });
    if (isPasses && javascriptFilename) {
      return this.cheBranding.getIdeResourcesPath() + javascriptFilename;
    }
    return null;
  }

}
