/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.stmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.yangtools.yang.stmt.StmtTestUtils.sourceForResource;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.GroupingDefinition;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.parser.rfc7950.reactor.RFC7950Reactors;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.parser.spi.source.StatementStreamSource;

public class EffectiveBuildTest {

    private static final StatementStreamSource SIMPLE_MODULE = sourceForResource(
            "/stmt-test/effective-build/simple-module.yang");
    private static final QNameModule SIMPLE_MODULE_QNAME = QNameModule.create(URI.create("simple.yang"));
    private static final StatementStreamSource YANG_EXT = sourceForResource(
            "/stmt-test/extensions/yang-ext.yang");

    @Test
    public void effectiveBuildTest() throws ReactorException {
        SchemaContext result = RFC7950Reactors.defaultReactor().newBuild().addSources(SIMPLE_MODULE)
                .buildEffective();

        assertNotNull(result);

        Module simpleModule = result.findModules("simple-module").iterator().next();
        assertNotNull(simpleModule);

        final QName q1 = QName.create(SIMPLE_MODULE_QNAME, "root-container");
        final QName q2 = QName.create(SIMPLE_MODULE_QNAME, "sub-container");
        final QName q3 = QName.create(SIMPLE_MODULE_QNAME, "sub-sub-container");
        final QName q4 = QName.create(SIMPLE_MODULE_QNAME, "root-container2");
        final QName q5 = QName.create(SIMPLE_MODULE_QNAME, "sub-container2");
        final QName q6 = QName.create(SIMPLE_MODULE_QNAME, "sub-sub-container2");
        final QName q7 = QName.create(SIMPLE_MODULE_QNAME, "grp");

        ContainerSchemaNode rootCon = (ContainerSchemaNode) simpleModule.getDataChildByName(q1);
        assertNotNull(rootCon);

        ContainerSchemaNode subCon = (ContainerSchemaNode) rootCon.getDataChildByName(q2);
        assertNotNull(subCon);

        ContainerSchemaNode subSubCon = (ContainerSchemaNode) subCon.getDataChildByName(q3);
        assertNotNull(subSubCon);

        ContainerSchemaNode rootCon2 = (ContainerSchemaNode) simpleModule.getDataChildByName(q4);
        assertNotNull(rootCon2);

        ContainerSchemaNode subCon2 = (ContainerSchemaNode) rootCon2.getDataChildByName(q5);
        assertNotNull(subCon2);

        ContainerSchemaNode subSubCon2 = (ContainerSchemaNode) subCon2.getDataChildByName(q6);
        assertNotNull(subSubCon2);

        GroupingDefinition grp = simpleModule.getGroupings().iterator().next();
        assertNotNull(grp);
        assertEquals(q7, grp.getQName());

        ContainerSchemaNode grpSubCon2 = (ContainerSchemaNode) grp.getDataChildByName(q5);
        assertNotNull(grpSubCon2);

        ContainerSchemaNode grpSubSubCon2 = (ContainerSchemaNode) grpSubCon2.getDataChildByName(q6);
        assertNotNull(grpSubSubCon2);

        assertEquals(SchemaPath.create(true, q1, q2, q3), subSubCon.getPath());
        assertEquals(SchemaPath.create(true, q4, q5, q6), subSubCon2.getPath());
        assertEquals(SchemaPath.create(true, q7, q5, q6), grpSubSubCon2.getPath());
    }

    @Test
    public void extensionsTest() throws ReactorException {
        SchemaContext result = RFC7950Reactors.defaultReactor().newBuild().addSource(YANG_EXT).buildEffective();
        assertNotNull(result);

        Collection<? extends GroupingDefinition> groupings = result.getGroupings();
        assertEquals(1, groupings.size());

        GroupingDefinition grp = groupings.iterator().next();

        Collection<? extends DataSchemaNode> childNodes = grp.getChildNodes();
        assertEquals(1, childNodes.size());
        DataSchemaNode child = childNodes.iterator().next();

        assertTrue(child instanceof LeafSchemaNode);
        LeafSchemaNode leaf = (LeafSchemaNode) child;

        assertNotNull(leaf.getType());
    }

    @Test
    public void mockTest() throws ReactorException, FileNotFoundException, URISyntaxException {
        SchemaContext result = RFC7950Reactors.defaultReactor().newBuild().addSource(YANG_EXT).buildEffective();
        assertNotNull(result);
    }
}
