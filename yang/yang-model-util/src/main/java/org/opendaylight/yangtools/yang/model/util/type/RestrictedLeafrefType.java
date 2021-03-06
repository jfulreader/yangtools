/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.model.util.type;

import java.util.Collection;
import org.opendaylight.yangtools.yang.model.api.PathExpression;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.opendaylight.yangtools.yang.model.api.type.LeafrefTypeDefinition;

final class RestrictedLeafrefType extends AbstractRestrictedType<LeafrefTypeDefinition>
        implements LeafrefTypeDefinition {

    private final boolean requireInstance;

    RestrictedLeafrefType(final LeafrefTypeDefinition baseType, final SchemaPath path,
            final Collection<? extends UnknownSchemaNode> unknownSchemaNodes, final boolean requireInstance) {
        super(baseType, path, unknownSchemaNodes);

        this.requireInstance = requireInstance;
    }

    @Override
    public PathExpression getPathStatement() {
        return getBaseType().getPathStatement();
    }

    @Override
    public boolean requireInstance() {
        return requireInstance;
    }

    @Override
    public int hashCode() {
        return LeafrefTypeDefinition.hashCode(this);
    }

    @Override
    public boolean equals(final Object obj) {
        return LeafrefTypeDefinition.equals(this, obj);
    }

    @Override
    public String toString() {
        return LeafrefTypeDefinition.toString(this);
    }
}
