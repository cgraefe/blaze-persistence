/*
 * Copyright 2015 Blazebit.
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

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.blazebit.persistence.entity.Document;
import com.blazebit.persistence.entity.EmbeddableTestEntity;
import com.blazebit.persistence.entity.EmbeddableTestEntityEmbeddable;
import com.blazebit.persistence.entity.EmbeddableTestEntityId;
import com.blazebit.persistence.entity.EmbeddableTestEntityIdEmbeddable;
import com.blazebit.persistence.entity.IntIdEntity;
import com.blazebit.persistence.testsuite.base.category.NoDatanucleus;
import com.blazebit.persistence.testsuite.base.category.NoEclipselink;
import com.blazebit.persistence.testsuite.base.category.NoFirebird;
import com.blazebit.persistence.testsuite.base.category.NoH2;
import com.blazebit.persistence.testsuite.base.category.NoMySQL;
import com.blazebit.persistence.testsuite.base.category.NoOpenJPA;
import com.blazebit.persistence.testsuite.base.category.NoOracle;
import com.blazebit.persistence.testsuite.base.category.NoSQLite;
import com.blazebit.persistence.tx.TxVoidWork;

/**
 * This kind of mapping is not required to be supported by a JPA implementation.
 *
 * @author Christian Beikov
 * @author Moritz Becker
 * @since 1.1.0
 */
public class UpdateEmbeddableComplexTest extends AbstractCoreTest {

    Document doc1;
    Document doc2;
    Document doc3;

    @Override
    protected Class<?>[] getEntityClasses() {
        return new Class<?>[] {
            IntIdEntity.class,
            EmbeddableTestEntity.class,
            EmbeddableTestEntityEmbeddable.class,
            EmbeddableTestEntityId.class
        };
    }
    
    // NOTE: Currently only PostgreSQL and DB2 support returning from within a CTE
    @Test
    @Category({ NoH2.class, NoOracle.class, NoSQLite.class, NoFirebird.class, NoMySQL.class, NoDatanucleus.class, NoEclipselink.class, NoOpenJPA.class })
    public void testUpdateWithReturningEmbeddable(){
    	final String newEmbeddableTestEntityIdKey = "newKey";
    	
        transactional(new TxVoidWork() {
            @Override
            public void work() {
    			IntIdEntity intIdEntity1 = new IntIdEntity("1");
    			em.persist(intIdEntity1);
    			
    			EmbeddableTestEntityId embeddable1Id = new EmbeddableTestEntityId(intIdEntity1, "oldKey");
    			embeddable1Id.setLocalizedEntity(new EmbeddableTestEntityIdEmbeddable(""));
    			EmbeddableTestEntity embeddable1 = new EmbeddableTestEntity();
    			embeddable1.setId(embeddable1Id);
    			em.persist(embeddable1);
    			em.flush();
                
    	        String key = cbf.update(em, EmbeddableTestEntity.class, "e")
    	            .set("id.key", newEmbeddableTestEntityIdKey)
    	            .executeWithReturning("id.key", String.class)
    	            .getLastResult();
    	        
    	        assertEquals(newEmbeddableTestEntityIdKey, key);
            }
        });
    }
    
    // NOTE: Currently only PostgreSQL and DB2 support returning from within a CTE
    @Test
    @Category({ NoH2.class, NoOracle.class, NoSQLite.class, NoFirebird.class, NoMySQL.class, NoDatanucleus.class, NoEclipselink.class, NoOpenJPA.class })
    public void testUpdateWithReturningExplicitId(){
    	final String intIdEntity1Key = "1";
    	
        transactional(new TxVoidWork() {
            @Override
            public void work() {
    			IntIdEntity intIdEntity1 = new IntIdEntity("1");
    			em.persist(intIdEntity1);
    			
    			EmbeddableTestEntityId embeddable2Id = new EmbeddableTestEntityId(intIdEntity1, "2");
    			embeddable2Id.setLocalizedEntity(new EmbeddableTestEntityIdEmbeddable(""));
    			EmbeddableTestEntity embeddable2 = new EmbeddableTestEntity();
    			
    			embeddable2.setId(embeddable2Id);
    			em.persist(embeddable2);
    			
    			EmbeddableTestEntityId embeddable1Id = new EmbeddableTestEntityId(intIdEntity1, intIdEntity1Key);
    			embeddable1Id.setLocalizedEntity(new EmbeddableTestEntityIdEmbeddable(""));
    			EmbeddableTestEntity embeddable1 = new EmbeddableTestEntity();
    			embeddable1.setId(embeddable1Id);
    			EmbeddableTestEntityEmbeddable embeddable1Embeddable = new EmbeddableTestEntityEmbeddable();
    			embeddable1Embeddable.setManyToOne(embeddable2);
    			embeddable1.setEmbeddable(embeddable1Embeddable);
    			em.persist(embeddable1);
    			em.flush();

    	        EmbeddableTestEntityId manyToOneId = cbf.update(em, EmbeddableTestEntity.class, "e")
    	            .set("id.key", "newKey")
    	            .where("e.id.key").eq(intIdEntity1Key)
    	            .executeWithReturning("embeddable.manyToOne.id", EmbeddableTestEntityId.class)
    	            .getLastResult();
    	        assertEquals(embeddable2Id, manyToOneId);
            }
        });
    }
}
