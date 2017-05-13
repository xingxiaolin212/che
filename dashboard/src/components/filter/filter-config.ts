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
import {CheNumberRoundFilter} from './number-round/number-round.filter';
import {ChangeUnitFilter} from './change-unit.filter';

export class FilterConfig {

  constructor(register: che.IRegisterService) {

    new CheNumberRoundFilter(register);
    new ChangeUnitFilter(register);

  }

}
