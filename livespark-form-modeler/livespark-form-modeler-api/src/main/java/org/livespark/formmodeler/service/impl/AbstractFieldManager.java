/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

package org.livespark.formmodeler.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.common.client.api.Assert;
import org.livespark.formmodeler.model.FieldDefinition;
import org.livespark.formmodeler.model.FieldTypeInfo;
import org.livespark.formmodeler.model.impl.relations.EntityRelationField;
import org.livespark.formmodeler.model.impl.relations.MultipleSubFormFieldDefinition;
import org.livespark.formmodeler.model.impl.relations.SubFormFieldDefinition;
import org.livespark.formmodeler.service.impl.fieldProviders.BasicTypeFieldProvider;
import org.livespark.formmodeler.service.FieldManager;
import org.livespark.formmodeler.service.FieldProvider;
import org.livespark.formmodeler.service.MultipleFieldProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pere Fernandez <pefernan@redhat.com>
 */
@Dependent
public abstract class AbstractFieldManager implements FieldManager {
    private static transient Logger log = LoggerFactory.getLogger( FieldManager.class );

    protected Set<BasicTypeFieldProvider> basicProviders = new TreeSet<>( new Comparator<BasicTypeFieldProvider>() {
        @Override
        public int compare( BasicTypeFieldProvider o1, BasicTypeFieldProvider o2 ) {
            return o1.getPriority() - o2.getPriority();
        }
    } );

    protected Map<String, FieldProvider> entityTypeFieldProvider = new HashMap<>();

    protected Map<String, FieldProvider> multipleEntityTypeFieldProvider = new HashMap<>();

    protected Map<String, FieldProvider> providersByFieldCode = new HashMap<>();

    protected String defaultSingleEntity = SubFormFieldDefinition.CODE;
    protected String defaultMultipleEntity = MultipleSubFormFieldDefinition.CODE;


    protected void registerFieldProvider( FieldProvider provider ) {

        boolean isMultiple = provider instanceof MultipleFieldProvider;

        if ( provider instanceof BasicTypeFieldProvider ) {
            BasicTypeFieldProvider basicTypeProvider = (BasicTypeFieldProvider) provider;

            basicProviders.add( basicTypeProvider );

        } else {
            if ( isMultiple ) {
                multipleEntityTypeFieldProvider.put( provider.getProviderCode(), provider );
            } else {
                entityTypeFieldProvider.put( provider.getProviderCode(), provider );
            }
        }

        providersByFieldCode.put( provider.getProviderCode(), provider );
    }

    @Override
    public Collection<String> getBaseFieldTypes() {
        List<String> fieldCodes = new ArrayList<>();

        for ( BasicTypeFieldProvider provider : basicProviders ) {
            fieldCodes.add( provider.getProviderCode() );
        }

        fieldCodes.addAll( entityTypeFieldProvider.keySet() );
        fieldCodes.addAll( multipleEntityTypeFieldProvider.keySet() );

        return fieldCodes;
    }

    @Override
    public FieldDefinition getDefinitionByTypeCode( String fieldTypeCode ) {
        FieldProvider provider = providersByFieldCode.get( fieldTypeCode );

        if ( provider != null ) {
            return provider.getDefaultField();
        }

        return null;
    }

    @Override
    public FieldDefinition getDefinitionByValueType( FieldTypeInfo typeInfo ) {

        for ( BasicTypeFieldProvider basicProvider : basicProviders ) {
            FieldDefinition field = basicProvider.getFieldByType( typeInfo );
            if ( field != null ) {
                field.setStandaloneClassName( typeInfo.getType() );
                return field;
            }
        }

        FieldProvider provider;

        if ( typeInfo.isList() ) {
            provider = multipleEntityTypeFieldProvider.get( defaultMultipleEntity );
        } else {
            provider = entityTypeFieldProvider.get( defaultSingleEntity );
        }

        if ( provider != null ) {
            FieldDefinition instance = provider.getFieldByType( typeInfo );
            instance.setStandaloneClassName( typeInfo.getType() );
            return instance;
        }
        return null;
    }

    @Override
    public Collection<String> getCompatibleFields( FieldDefinition fieldDefinition ) {
        if (fieldDefinition.getStandaloneClassName() != null ) {
            if ( fieldDefinition instanceof EntityRelationField ) {
                if ( fieldDefinition.getFieldTypeInfo().isList() ) {
                    return new TreeSet<>( multipleEntityTypeFieldProvider.keySet() );
                }
                return new TreeSet<>( multipleEntityTypeFieldProvider.keySet() );
            }

            Set result = new TreeSet();
            for ( BasicTypeFieldProvider provider : basicProviders ) {
                if ( provider.isCompatible( fieldDefinition ) ) {
                    result.add( provider.getProviderCode() );
                }
            }

            return result;
        } else {
            if ( fieldDefinition instanceof EntityRelationField ) {
                if ( fieldDefinition.getFieldTypeInfo().isList() ) {
                    return new TreeSet<>( multipleEntityTypeFieldProvider.keySet() );
                }
                return new TreeSet<>( entityTypeFieldProvider.keySet() );
            }

            BasicTypeFieldProvider provider = (BasicTypeFieldProvider) providersByFieldCode.get( fieldDefinition.getCode() );

            Set result = new TreeSet();
            for ( Class clazz : provider.getSupportedTypes() ) {
                result.addAll( getCompatibleTypes( clazz ) );
            }
            return result;
        }
    }

    @Override
    public FieldDefinition getFieldFromProvider( String typeCode, FieldTypeInfo typeInfo ) {
        Assert.notNull( "TypeInfo cannot be null", typeInfo );

        if ( typeCode == null ) {
            return getDefinitionByValueType( typeInfo );
        }

        for ( BasicTypeFieldProvider basicProvider : basicProviders ) {
            if ( basicProvider.getProviderCode().equals( typeCode ) ) {
                return basicProvider.getFieldByType( typeInfo );
            }
        }

        FieldProvider provider = entityTypeFieldProvider.get( typeCode );

        if ( provider == null ) {
            provider = multipleEntityTypeFieldProvider.get( typeCode );
        }

        if ( provider != null ) {
            return provider.getFieldByType( typeInfo );
        }

        return null;
    }

    @Override
    public FieldDefinition getFieldFromProviderWithType( String typeCode, FieldTypeInfo typeInfo ) {
        Assert.notNull( "TypeCode cannot be null", typeCode );
        Assert.notNull( "TypeInfo cannot be null", typeInfo );

        FieldProvider provider = entityTypeFieldProvider.get( typeCode );

        if ( provider == null ) {
            provider = multipleEntityTypeFieldProvider.get( typeCode );
        }

        if ( provider != null ) {
            return provider.getFieldByType( typeInfo );
        }

        for ( BasicTypeFieldProvider basicProvider : basicProviders ) {
            if ( basicProvider.getProviderCode().equals( typeCode ) ) {
                return basicProvider.createFieldByType( typeInfo );
            }
        }

        return null;
    }



    protected List<String> getCompatibleTypes( Class clazz ) {
        List<String> result = new ArrayList<>();

        for ( BasicTypeFieldProvider provider : basicProviders ) {
            if ( provider.supports( clazz ) ) {
                result.add( provider.getProviderCode() );
            }
        }

        return result;
    }
}
