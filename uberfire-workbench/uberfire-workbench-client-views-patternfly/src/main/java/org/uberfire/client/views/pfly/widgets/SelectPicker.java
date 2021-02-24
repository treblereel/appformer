/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.uberfire.client.views.pfly.widgets;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;
import org.gwtbootstrap3.client.shared.js.JQuery;

@JsType(
        isNative = true,
        namespace = "<global>",
        name = "jQuery"
)
class SelectPicker extends JQuery {

    @JsOverlay
    public static SelectPicker jQuery(com.google.gwt.dom.client.Element e) {
        return (SelectPicker) JQuery.jQuery(e);
    }

    native <T> T selectpicker(String val);

    native <T> T selectpicker(String val, Object value);

    native <T> T selectpicker(Object value);

    native void prop(String disabled, boolean b);

    @FunctionalInterface
    @JsFunction
    interface FnNoArgs {
        void onInvoke();
    }

}
