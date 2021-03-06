/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import {Component, EventEmitter, Input, Output} from '@angular/core';
import {GroupStaticProperty} from '../../model/GroupStaticProperty';
import {EventSchema} from '../../schema-editor/model/EventSchema';

@Component({
    selector: 'app-static-group',
    templateUrl: './static-group.component.html',
    styleUrls: ['./static-group.component.css']
})
export class StaticGroupComponent {

    @Input()
    staticProperty: GroupStaticProperty;

    @Input()
    adapterId: string;

    @Input()
    eventSchema: EventSchema;

    @Output() inputEmitter: EventEmitter<Boolean> = new EventEmitter<Boolean>();

    private hasInput: Boolean;

    valueChange(inputValue) {
        let property = this.staticProperty.staticProperties.find(property => property.isValid == false);
        if (property == undefined) {
            this.hasInput = true;
        } else {
            this.hasInput = false;
        }
        this.inputEmitter.emit(this.hasInput)
    }

}