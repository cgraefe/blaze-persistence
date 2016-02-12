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
package com.blazebit.persistence;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import javax.persistence.Tuple;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.blazebit.persistence.entity.Workflow;
import com.blazebit.persistence.testsuite.base.category.NoDB2;

/**
 *
 * @author Christian Beikov
 * @since 1.0
 */
public class MultipleJoinComplexExpressionTest extends AbstractCoreTest {

    @Override
    protected Class<?>[] getEntityClasses() {
        return new Class<?>[]{
            Workflow.class
        };
    }

    @Test
    public void testCaseWhenBooleanExpressionSelect() {
        // TODO: Report that EclipseLink has a bug in case when handling
        CriteriaBuilder<Tuple> cb = cbf.create(em, Tuple.class).from(Workflow.class)
                .select("CASE WHEN localized[:locale].name IS NULL THEN localized[defaultLanguage].name ELSE localized[:locale].name END");
        String expectedQuery = "SELECT CASE WHEN " + joinAliasValue("localized_locale_1", "name") + " IS NULL THEN " + joinAliasValue("localized_workflow_defaultLanguage_1", "name") + " ELSE " + joinAliasValue("localized_locale_1", "name") + " END FROM Workflow workflow"
                + " LEFT JOIN workflow.localized localized_locale_1 " + ON_CLAUSE + " KEY(localized_locale_1) = :locale"
                + " LEFT JOIN workflow.localized localized_workflow_defaultLanguage_1 " + ON_CLAUSE + " KEY(localized_workflow_defaultLanguage_1) = workflow.defaultLanguage";
        assertEquals(expectedQuery, cb.getQueryString());
        cb.setParameter("locale", Locale.GERMAN)
            .getResultList();
    }
    
    @Test
    @Category(NoDB2.class)
    public void testCaseWhenWithFunctionsInSelectAndLiterals() {
        // TODO: Report that EclipseLink has a bug in case when handling
        CriteriaBuilder<Tuple> cb = cbf.create(em, Tuple.class).from(Workflow.class)
                .select("SUBSTRING(COALESCE(CASE WHEN localized[:locale].name IS NULL THEN localized[defaultLanguage].name ELSE localized[:locale].name END,' - '),0,20)");
        // TODO: Apparently DB2 is 1 based on the start index
        String expectedQuery = 
                "SELECT SUBSTRING(COALESCE(CASE WHEN " + joinAliasValue("localized_locale_1", "name") + " IS NULL THEN " + joinAliasValue("localized_workflow_defaultLanguage_1", "name") + " ELSE " + joinAliasValue("localized_locale_1", "name") + " END,' - '),0,20)"
                + " FROM Workflow workflow"
                + " LEFT JOIN workflow.localized localized_locale_1 " + ON_CLAUSE + " KEY(localized_locale_1) = :locale"
                + " LEFT JOIN workflow.localized localized_workflow_defaultLanguage_1 " + ON_CLAUSE + " KEY(localized_workflow_defaultLanguage_1) = workflow.defaultLanguage";
        assertEquals(expectedQuery, cb.getQueryString());
        cb.setParameter("locale", Locale.GERMAN)
            .getResultList();
    }
}
