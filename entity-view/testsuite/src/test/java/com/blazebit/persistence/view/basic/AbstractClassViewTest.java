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

package com.blazebit.persistence.view.basic;

import com.blazebit.persistence.view.AbstractEntityViewTest;
import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.view.entity.Document;
import com.blazebit.persistence.view.entity.Person;
import com.blazebit.persistence.view.EntityViewManager;
import com.blazebit.persistence.view.basic.model.DocumentViewAbstractClass;
import com.blazebit.persistence.view.basic.model.DocumentViewInterface;
import com.blazebit.persistence.view.basic.model.PersonView1;
import com.blazebit.persistence.view.impl.EntityViewConfigurationImpl;
import java.util.List;
import javax.persistence.EntityTransaction;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author cpbec
 */
public class AbstractClassViewTest extends AbstractEntityViewTest {
    
    protected static EntityViewManager evm;
    
    @BeforeClass
    public static void initEvm() {
        EntityViewConfigurationImpl cfg = new EntityViewConfigurationImpl();
        cfg.addEntityView(DocumentViewInterface.class);
        cfg.addEntityView(DocumentViewAbstractClass.class);
        cfg.addEntityView(PersonView1.class);
        evm = cfg.createEntityViewManager();
    }
    
    private Document doc1;
    private Document doc2;
    
    @Before
    public void setUp() {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            doc1 = new Document("doc1");
            doc2 = new Document("doc2");
            
            Person o1 = new Person("pers1");
            Person o2 = new Person("pers2");
            o1.getLocalized().put(1, "localized1");
            o2.getLocalized().put(1, "localized2");
            o1.setPartnerDocument(doc1);
            o2.setPartnerDocument(doc2);
            
            doc1.setAge(10);
            doc1.setOwner(o1);
            doc2.setAge(20);
            doc2.setOwner(o2);
            
            doc1.getContacts().put(1, o1);
            doc2.getContacts().put(1, o2);
            
            doc1.getContacts2().put(2, o1);
            doc2.getContacts2().put(2, o2);
            
            em.persist(o1);
            em.persist(o2);
            
            em.persist(doc1);
            em.persist(doc2);
            
            em.flush();
            tx.commit();
            em.clear();
            
            doc1 = em.find(Document.class, doc1.getId());
            doc2 = em.find(Document.class, doc2.getId());
        } catch (Exception e) {
            tx.rollback();
            throw new RuntimeException(e);
        }
    }
    
    @Test
    public void testAbstractClass() {
        CriteriaBuilder<Document> criteria = cbf.from(em, Document.class, "d")
                .orderByAsc("id");
        List<DocumentViewAbstractClass> results = evm.applyObjectBuilder(DocumentViewAbstractClass.class, criteria).setParameter("contactPersonNumber", 2).getResultList();
        
        assertEquals(2, results.size());
        // Doc1
        assertEquals(doc1.getId(), results.get(0).getId());
        assertEquals(doc1.getName(), results.get(0).getName());
        assertEquals(doc1.getContacts().get(1), results.get(0).getFirstContactPerson());
        assertEquals(doc1.getAge() + 1, results.get(0).getAge());
        assertEquals(doc1.getContacts2().get(2), results.get(0).getMyContactPerson());
        assertEquals(Integer.valueOf(2), results.get(0).getContactPersonNumber());
        assertEquals(Integer.valueOf(2), results.get(0).getContactPersonNumber2());
        assertEquals(Long.valueOf(1), results.get(0).getContactCount());
        // Doc2
        assertEquals(doc2.getId(), results.get(1).getId());
        assertEquals(doc2.getName(), results.get(1).getName());
        assertEquals(doc2.getContacts().get(1), results.get(1).getFirstContactPerson());
        assertEquals(doc2.getAge() + 1, results.get(1).getAge());
        assertEquals(doc2.getContacts2().get(2), results.get(1).getMyContactPerson());
        assertEquals(Integer.valueOf(2), results.get(1).getContactPersonNumber());
        assertEquals(Integer.valueOf(2), results.get(1).getContactPersonNumber2());
        assertEquals(Long.valueOf(1), results.get(1).getContactCount());
    }
}
