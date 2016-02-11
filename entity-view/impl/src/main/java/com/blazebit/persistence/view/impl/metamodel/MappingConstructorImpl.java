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
package com.blazebit.persistence.view.impl.metamodel;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;

import com.blazebit.persistence.impl.expression.ExpressionFactory;
import com.blazebit.persistence.view.MappingParameter;
import com.blazebit.persistence.view.MappingSingular;
import com.blazebit.persistence.view.MappingSubquery;
import com.blazebit.persistence.view.ViewConstructor;
import com.blazebit.persistence.view.impl.MetamodelTargetResolvingExpressionVisitor;
import com.blazebit.persistence.view.metamodel.ManagedViewType;
import com.blazebit.persistence.view.metamodel.MappingConstructor;
import com.blazebit.persistence.view.metamodel.ParameterAttribute;
import com.blazebit.persistence.view.metamodel.PluralAttribute;
import com.blazebit.reflection.ReflectionUtils;

/**
 *
 * @author Christian Beikov
 * @since 1.0
 */
public class MappingConstructorImpl<X> implements MappingConstructor<X> {

    private final String name;
    private final ManagedViewType<X> declaringType;
    private final Constructor<X> javaConstructor;
    private final List<AbstractParameterAttribute<? super X, ?>> parameters;

    public MappingConstructorImpl(ManagedViewType<X> viewType, String name, Constructor<X> constructor, Set<Class<?>> entityViews) {
        this.name = name;
        this.declaringType = viewType;
        this.javaConstructor = constructor;

        if (constructor.getExceptionTypes().length != 0) {
            throw new IllegalArgumentException("The constructor '" + constructor.toString() + "' of the class '" + constructor.getDeclaringClass().getName()
                + "' may not throw an exception!");
        }
        
        int parameterCount = constructor.getParameterTypes().length;
        List<AbstractParameterAttribute<? super X, ?>> parameters = new ArrayList<AbstractParameterAttribute<? super X, ?>>(parameterCount);
        for (int i = 0; i < parameterCount; i++) {
            AbstractParameterAttribute.validate(this, i);
            AbstractParameterAttribute<? super X, ?> parameter = createParameterAttribute(this, i, entityViews);
            parameters.add(parameter);
        }

        this.parameters = Collections.unmodifiableList(parameters);
    }
    
    public void checkParameters(ManagedType<?> managedType, Map<Class<?>, ManagedViewType<?>> managedViews, ExpressionFactory expressionFactory, Metamodel metamodel, Set<String> errors) {
        for (AbstractParameterAttribute<? super X, ?> parameter : parameters) {
            String error = checkParameter(parameter, managedType, managedViews, expressionFactory, metamodel);
            
            if (error != null) {
                errors.add(error);
            }
        }
    }

    private String checkParameter(AbstractParameterAttribute<? super X, ?> parameter, ManagedType<?> managedType, Map<Class<?>, ManagedViewType<?>> managedViews, ExpressionFactory expressionFactory, Metamodel metamodel) {
        String expression = parameter.getMapping();
        
        if (expression == null || parameter.isQueryParameter()) {
            // Subqueries and parameters can't be checked
            return null;
        }
        
        Class<?> expressionType = parameter.getJavaType();
        Class<?> elementType = null;
        
        if (parameter.isCollection()) {
            elementType = ((PluralAttribute<?, ?, ?>) parameter).getElementType();
        }
        
        if (parameter.isCollection() && !((PluralAttribute<?, ?, ?>) parameter).isIndexed() && Collection.class.isAssignableFrom(expressionType)) {
            // We can assign e.g. a Set to a List, so let's use the common supertype
            expressionType = Collection.class;
        } else if (!parameter.isCollection() && parameter.isSubview()) {
            ManagedViewType<?> subviewType = managedViews.get(expressionType);
            
            if (subviewType == null) {
                throw new IllegalStateException("Expected subview '" + expressionType.getName() + "' to exist but couldn't find it!");
            }
            
            expressionType = subviewType.getEntityClass();
        }

        MetamodelTargetResolvingExpressionVisitor visitor = new MetamodelTargetResolvingExpressionVisitor(managedType, metamodel);

        try {
            expressionFactory.createSimpleExpression(expression).accept(visitor);
        } catch (IllegalArgumentException ex) {
            return "An error occurred while trying to resolve the parameter with the index '" + parameter.getIndex() + "' of the constructor '" + parameter.getDeclaringConstructor().getJavaConstructor() + "': " + ex.getMessage();
        }

        Map<Method, Class<?>[]> possibleTargets = visitor.getPossibleTargets();

        if (!possibleTargets.isEmpty()) {
            boolean error = true;
            for (Map.Entry<Method, Class<?>[]> entry : possibleTargets.entrySet()) {
                Class<?> possibleTargetType = entry.getValue()[0];

                // Null is the marker for ANY TYPE
                if (possibleTargetType == null || expressionType.isAssignableFrom(possibleTargetType)
                    || Map.class.isAssignableFrom(possibleTargetType) && expressionType.isAssignableFrom(entry.getValue()[1])) {
                    error = false;
                    break;
                }
            }
            
            if (error) {
                StringBuilder sb = new StringBuilder();
                sb.append('[');
                for (Class<?>[] possibleTargetType : possibleTargets.values()) {
                    sb.append(possibleTargetType[0].getName());
                    sb.append(", ");
                }
                
                sb.setLength(sb.length() - 2);
                sb.append(']');
                return "The resolved possible types " + sb.toString() + " are not assignable to the given expression type '" + parameter.getJavaType().getName() + "' of the expression declared by the parameter with the index '" + parameter.getIndex() + "' of the constructor '" + parameter.getDeclaringConstructor().getJavaConstructor() + "'!";
            }
        }
        
        return null;
    }

    public static String validate(ManagedViewType<?> viewType, Constructor<?> c) {
        ViewConstructor viewConstructor = c.getAnnotation(ViewConstructor.class);

        if (viewConstructor == null) {
            return "init";
        }

        return viewConstructor.value();
    }

    // If you change something here don't forget to also update ViewTypeImpl#createMethodAttribute
    private static <X> AbstractParameterAttribute<? super X, ?> createParameterAttribute(MappingConstructor<X> constructor, int index, Set<Class<?>> entityViews) {
        Annotation mapping = AbstractParameterAttribute.getMapping(constructor, index);
        if (mapping == null) {
            return null;
        }

        Type parameterType = constructor.getJavaConstructor().getGenericParameterTypes()[index];
        Class<?> attributeType;
        
        if (parameterType instanceof TypeVariable<?>) {
            attributeType = ReflectionUtils.resolveTypeVariable(constructor.getDeclaringType().getJavaType(), (TypeVariable<?>) parameterType);
        } else {
            attributeType = constructor.getJavaConstructor().getParameterTypes()[index];
        }
        
        if (mapping instanceof MappingParameter) {
            return new ParameterMappingSingularAttributeImpl<X, Object>(constructor, index, mapping, entityViews);
        }
        
        Annotation[] annotations = constructor.getJavaConstructor().getParameterAnnotations()[index];
        
        for (Annotation a : annotations) {
            // Force singular mapping
            if (MappingSingular.class == a.annotationType()) {
                return new ParameterMappingSingularAttributeImpl<X, Object>(constructor, index, mapping, entityViews);
            }
        }

        if (Collection.class == attributeType) {
            return new ParameterMappingCollectionAttributeImpl<X, Object>(constructor, index, mapping, entityViews);
        } else if (List.class == attributeType) {
            return new ParameterMappingListAttributeImpl<X, Object>(constructor, index, mapping, entityViews);
        } else if (Set.class == attributeType || SortedSet.class == attributeType || NavigableSet.class == attributeType) {
            return new ParameterMappingSetAttributeImpl<X, Object>(constructor, index, mapping, entityViews);
        } else if (Map.class == attributeType || SortedMap.class == attributeType || NavigableMap.class == attributeType) {
            return new ParameterMappingMapAttributeImpl<X, Object, Object>(constructor, index, mapping, entityViews);
        } else if (mapping instanceof MappingSubquery) {
            return new ParameterSubquerySingularAttributeImpl<X, Object>(constructor, index, mapping, entityViews);
        } else {
            return new ParameterMappingSingularAttributeImpl<X, Object>(constructor, index, mapping, entityViews);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ManagedViewType<X> getDeclaringType() {
        return declaringType;
    }

    @Override
    public Constructor<X> getJavaConstructor() {
        return javaConstructor;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ParameterAttribute<? super X, ?>> getParameterAttributes() {
        return (List<ParameterAttribute<? super X, ?>>) (List<?>) parameters;
    }

    @Override
    public ParameterAttribute<? super X, ?> getParameterAttribute(int index) {
        return parameters.get(index);
    }
}
