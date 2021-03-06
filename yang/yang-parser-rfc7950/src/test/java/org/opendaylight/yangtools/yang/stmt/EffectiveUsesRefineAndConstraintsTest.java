/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.stmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.yangtools.yang.stmt.StmtTestUtils.sourceForResource;

import com.google.common.collect.Iterables;
import java.util.Optional;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ElementCountConstraint;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.util.SchemaContextUtil;
import org.opendaylight.yangtools.yang.parser.rfc7950.reactor.RFC7950Reactors;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;

public class EffectiveUsesRefineAndConstraintsTest {

    @Test
    public void refineTest() throws ReactorException {
        SchemaContext result = RFC7950Reactors.defaultReactor().newBuild()
                .addSource(sourceForResource("/stmt-test/uses/refine-test.yang"))
                .buildEffective();
        assertNotNull(result);

        final QNameModule qnameModule = Iterables.getOnlyElement(result.getModules()).getQNameModule();
        final QName rootContainer = QName.create(qnameModule, "root-container");
        final QName grp1 = QName.create(qnameModule, "grp-1");

        final QName containerFromGrouping = QName.create(qnameModule, "container-from-grouping");
        final QName listInContainer = QName.create(qnameModule, "list-in-container");
        final QName choiceFromGrp = QName.create(qnameModule, "choice-from-grp");

        final QName containerFromGrouping2 = QName.create(qnameModule, "container-from-grouping2");
        final QName presenceContainer = QName.create(qnameModule, "presence-container");

        SchemaPath listInContainerPath = SchemaPath.create(true, rootContainer, containerFromGrouping, listInContainer);
        SchemaPath choiceFromGrpPath = SchemaPath.create(true, rootContainer, containerFromGrouping, choiceFromGrp);
        SchemaPath presenceContainerPath = SchemaPath.create(true, rootContainer, containerFromGrouping2,
            presenceContainer);

        checkRefinedList(result, listInContainerPath);
        checkRefinedChoice(result, choiceFromGrpPath);
        checkRefinedContainer(result, presenceContainerPath);

        SchemaPath originalListInContainerPath = SchemaPath.create(true, grp1, containerFromGrouping, listInContainer);
        SchemaPath originalChoiceFromGrpPath = SchemaPath.create(true, grp1, containerFromGrouping, choiceFromGrp);
        SchemaPath originalPresenceContainerPath = SchemaPath.create(true, grp1, containerFromGrouping2,
            presenceContainer);

        checkOriginalList(result, originalListInContainerPath);
        checkOriginalChoice(result, originalChoiceFromGrpPath);
        checkOriginalContainer(result, originalPresenceContainerPath);
    }

    private static void checkOriginalContainer(final SchemaContext result, final SchemaPath path) {
        SchemaNode containerInContainerNode = SchemaContextUtil.findDataSchemaNode(result, path);
        assertNotNull(containerInContainerNode);

        ContainerSchemaNode containerSchemaNode = (ContainerSchemaNode) containerInContainerNode;
        assertFalse(containerSchemaNode.getReference().isPresent());
        assertFalse(containerSchemaNode.getDescription().isPresent());
        assertEquals(Optional.empty(), containerSchemaNode.effectiveConfig());
        assertFalse(containerSchemaNode.isPresenceContainer());

        assertEquals(0, containerSchemaNode.getMustConstraints().size());
    }

    private static void checkOriginalChoice(final SchemaContext result, final SchemaPath path) {
        SchemaNode choiceInContainerNode = SchemaContextUtil.findDataSchemaNode(result, path);
        assertNotNull(choiceInContainerNode);

        ChoiceSchemaNode choiceSchemaNode = (ChoiceSchemaNode) choiceInContainerNode;
        assertFalse(choiceSchemaNode.isMandatory());
    }

    private static void checkOriginalList(final SchemaContext result, final SchemaPath path) {
        SchemaNode listInContainerNode = SchemaContextUtil.findDataSchemaNode(result, path);
        assertNotNull(listInContainerNode);

        ListSchemaNode listSchemaNode = (ListSchemaNode) listInContainerNode;
        assertEquals(Optional.of("original reference"), listSchemaNode.getReference());
        assertEquals(Optional.of("original description"), listSchemaNode.getDescription());
        assertEquals(Optional.of(Boolean.FALSE), listSchemaNode.effectiveConfig());

        ElementCountConstraint listConstraints = listSchemaNode.getElementCountConstraint().get();
        assertEquals((Object) 10, listConstraints.getMinElements());
        assertEquals((Object) 20, listConstraints.getMaxElements());
        assertEquals(1, listSchemaNode.getMustConstraints().size());
    }

    private static void checkRefinedContainer(final SchemaContext result, final SchemaPath path) {
        SchemaNode containerInContainerNode = SchemaContextUtil.findDataSchemaNode(result, path);
        assertNotNull(containerInContainerNode);

        ContainerSchemaNode containerSchemaNode = (ContainerSchemaNode) containerInContainerNode;
        assertEquals(Optional.of("new reference"), containerSchemaNode.getReference());
        assertEquals(Optional.of("new description"), containerSchemaNode.getDescription());
        assertEquals(Optional.of(Boolean.TRUE), containerSchemaNode.effectiveConfig());
        assertTrue(containerSchemaNode.isPresenceContainer());
        assertEquals(1, containerSchemaNode.getMustConstraints().size());
    }

    private static void checkRefinedChoice(final SchemaContext result, final SchemaPath path) {
        SchemaNode choiceInContainerNode = SchemaContextUtil.findDataSchemaNode(result, path);
        assertNotNull(choiceInContainerNode);

        ChoiceSchemaNode choiceSchemaNode = (ChoiceSchemaNode) choiceInContainerNode;
        assertTrue(choiceSchemaNode.isMandatory());
    }

    private static void checkRefinedList(final SchemaContext result, final SchemaPath path) {
        SchemaNode listInContainerNode = SchemaContextUtil.findDataSchemaNode(result, path);
        assertNotNull(listInContainerNode);

        ListSchemaNode listSchemaNode = (ListSchemaNode) listInContainerNode;
        assertEquals(Optional.of("new reference"), listSchemaNode.getReference());
        assertEquals(Optional.of("new description"), listSchemaNode.getDescription());
        assertEquals(Optional.of(Boolean.TRUE), listSchemaNode.effectiveConfig());

        ElementCountConstraint listConstraints = listSchemaNode.getElementCountConstraint().get();
        assertEquals((Object) 5, listConstraints.getMinElements());
        assertEquals((Object) 7, listConstraints.getMaxElements());
        assertEquals(2, listSchemaNode.getMustConstraints().size());
    }
}
