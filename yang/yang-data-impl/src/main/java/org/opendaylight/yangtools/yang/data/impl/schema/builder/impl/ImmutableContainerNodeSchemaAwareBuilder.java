/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.impl.schema.builder.impl;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.valid.DataNodeContainerValidator;
import org.opendaylight.yangtools.yang.model.api.ContainerLike;

public final class ImmutableContainerNodeSchemaAwareBuilder extends ImmutableContainerNodeBuilder {
    private final DataNodeContainerValidator validator;

    private ImmutableContainerNodeSchemaAwareBuilder(final ContainerLike schema) {
        this.validator = new DataNodeContainerValidator(schema);
        super.withNodeIdentifier(NodeIdentifier.create(schema.getQName()));
    }

    private ImmutableContainerNodeSchemaAwareBuilder(final ContainerLike schema, final ImmutableContainerNode node) {
        super(node);
        this.validator = new DataNodeContainerValidator(schema);
        super.withNodeIdentifier(NodeIdentifier.create(schema.getQName()));
    }

    public static @NonNull DataContainerNodeBuilder<NodeIdentifier, ContainerNode> create(final ContainerLike schema) {
        return new ImmutableContainerNodeSchemaAwareBuilder(schema);
    }

    public static @NonNull DataContainerNodeBuilder<NodeIdentifier, ContainerNode> create(final ContainerLike schema,
            final ContainerNode node) {
        if (!(node instanceof ImmutableContainerNode)) {
            throw new UnsupportedOperationException(String.format("Cannot initialize from class %s", node.getClass()));
        }
        return new ImmutableContainerNodeSchemaAwareBuilder(schema, (ImmutableContainerNode)node);
    }

    @Override
    public DataContainerNodeBuilder<NodeIdentifier, ContainerNode> withNodeIdentifier(
            final NodeIdentifier withNodeIdentifier) {
        throw new UnsupportedOperationException("Node identifier created from schema");
    }

    @Override
    public DataContainerNodeBuilder<NodeIdentifier, ContainerNode> withChild(final DataContainerChild child) {
        validator.validateChild(child.getIdentifier());
        return super.withChild(child);
    }

    @Override
    public ContainerNode build() {
        // TODO check when statements... somewhere
        return super.build();
    }
}
