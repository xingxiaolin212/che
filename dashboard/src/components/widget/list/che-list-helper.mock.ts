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

type Item = {
  id: string;
  name: string;
}

/**
 * This class creates mock data sets.
 *
 * @author Oleksii Kurinnyi
 */
export class CheListHelperMock {
  private idKey: string = 'id';
  private items: Item[];

  mockData(): void {
    const itemsNumber = 15;

    this.items = Array.from(Array(itemsNumber)).map((x: any, i: number) => {
      return <Item>{
        [this.idKey]: `item-${this.idKey}-${i}`,
        name: `item-name-${i}`
      };
    });

  }

  getIdKey(): string {
    return this.idKey;
  }

  getItemsList(): Item[] {
    return this.items;
  }

  createFilterByName(name: string): any[] {
    return [{name: name}];
  }
}
