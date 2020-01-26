/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.parser.rfc7950.stmt.prefix;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.model.api.meta.DeclaredStatement;
import org.opendaylight.yangtools.yang.parser.spi.meta.StmtContext;

final class RegularPrefixStatement extends AbstractPrefixStatement {
    private final @NonNull Object substatements;

    RegularPrefixStatement(final StmtContext<String, ?, ?> context,
            final ImmutableList<? extends DeclaredStatement<?>> substatements) {
        super(context);
        this.substatements = maskSubstatements(substatements);
    }

    @Override
    public Collection<? extends DeclaredStatement<?>> declaredSubstatements() {
        return unmaskSubstatements(substatements);
    }
}