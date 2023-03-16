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

package com.blazebit.persistence.testsuite;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.testsuite.base.jpa.category.NoMSSQL;
import com.blazebit.persistence.testsuite.base.jpa.category.NoMySQLOld;
import com.blazebit.persistence.testsuite.base.jpa.category.NoOracle;
import com.blazebit.persistence.testsuite.entity.*;
import com.blazebit.persistence.testsuite.tx.TxVoidWork;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.hql.spi.QueryTranslator;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Christian Graefe
 * @since 1.6.9
 */
@Category({NoMySQLOld.class})
public class CTEJoinTest extends AbstractCoreTest
{

    @Override
    protected Class<?>[] getEntityClasses() {
        return new Class<?>[] {
                Document.class,
                Person.class,
                IntIdEntity.class,
                Version.class,
                PersonTestCTE.class
        };
    }

    @Override
    public void setUpOnce()
    {
        cleanDatabase();
        transactional(new TxVoidWork()
        {
            @Override
            public void work(EntityManager em)
            {
                Person p = new Person("Pers1");
                p.setAge(1L);
                em.persist(p);
                Person p2 = new Person("Pers1");
                p2.setAge(2L);
                em.persist(p2);
                Person p3 = new Person("Pers3");
                p3.setAge(3L);
                em.persist(p3);
                Person p4 = new Person("Pers3");
                p4.setAge(4L);
                em.persist(p4);
            }
        });
    }

    @Test
    public void testInnerJoinOnCTE() {
        CriteriaBuilder<Person> criteria = cbf.create(em, Person.class)
                .from(Person.class, "per")
                .innerJoinOnSubquery(PersonTestCTE.class, "ctePerson")
                    .from(Person.class,  "per0")
                        .bind("id").select("per0.id")
                        .bind("name").select("per0.name")
                        .bind("rowN").select("ROW_NUMBER() OVER (PARTITION BY per0.name ORDER BY per0.age DESC)")
                    .end()
                    .on("ctePerson.rowN").eqExpression("1")
                    .on("ctePerson.name").eqExpression("per.name")
                .end();

        List<Person> resultList = criteria.getResultList();
        assertNotNull(resultList);
        assertEquals(4, resultList.size());
    }

    @Test
    public void testLeftJoinOnCTE() {
        CriteriaBuilder<Person> criteria = cbf.create(em, Person.class)
                .from(Person.class, "per")
                .leftJoinOnSubquery(PersonTestCTE.class, "ctePerson")
                    .from(Person.class,  "per0")
                        .bind("id").select("per0.id")
                        .bind("name").select("per0.name")
                        .bind("rowN").select("ROW_NUMBER() OVER (PARTITION BY per0.name ORDER BY per0.age DESC)")
                    .end()
                    .on("ctePerson.rowN").eqExpression("1")
                    .on("ctePerson.name").eqExpression("per.name")
                .end();

        List<Person> resultList = criteria.getResultList();
        assertNotNull(resultList);
        assertEquals(4, resultList.size());
    }

}
