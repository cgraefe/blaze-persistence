/*
 * Copyright 2014 - 2023 Blazebit.
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

package com.blazebit.persistence.impl;

import com.blazebit.persistence.DeleteCriteriaBuilder;
import com.blazebit.persistence.parser.expression.ExpressionCopyContext;

import java.util.Map;

/**
 *
 * @param <T> The query result type
 * @author Christian Beikov
 * @since 1.2.0
 */
public class DeleteCollectionCriteriaBuilderImpl<T> extends AbstractDeleteCollectionCriteriaBuilder<T, DeleteCriteriaBuilder<T>, Void> implements DeleteCriteriaBuilder<T> {

    public DeleteCollectionCriteriaBuilderImpl(MainQuery mainQuery, Class<T> deleteOwnerClass, String alias, String collectionName) {
        super(mainQuery, null, true, deleteOwnerClass, alias, null, null, null, null, collectionName);
    }

    @Override
    AbstractCommonQueryBuilder<T, DeleteCriteriaBuilder<T>, AbstractCommonQueryBuilder<?, ?, ?, ?, ?>, AbstractCommonQueryBuilder<?, ?, ?, ?, ?>, BaseFinalSetOperationBuilderImpl<T, ?, ?>> copy(QueryContext queryContext, Map<JoinManager, JoinManager> joinManagerMapping, ExpressionCopyContext copyContext) {
        throw new UnsupportedOperationException("This should only be used on CTEs!");
    }
}
