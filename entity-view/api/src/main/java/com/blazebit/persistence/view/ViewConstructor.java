/*
 * Copyright 2014 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blazebit.persistence.view;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A naming mechanism for entity view constructors. The {@linkplain ViewConstructor} annotation can be applied to constructors
 * of an entity view. It is necessary to use them if an entity view has more than one constructor.
 *
 * @author Christian Beikov
 * @since 1.0
 */
@Target({ ElementType.CONSTRUCTOR })
@Retention(RetentionPolicy.RUNTIME)
public @interface ViewConstructor {

    /**
     * The name of the view constructor which should be unique within the entity view
     *
     * @return The name of the view constructor
     */
    String value();
}
