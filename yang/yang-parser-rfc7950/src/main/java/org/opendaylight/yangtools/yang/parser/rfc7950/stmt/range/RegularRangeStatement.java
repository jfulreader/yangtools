/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.parser.rfc7950.stmt.range;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.opendaylight.yangtools.yang.model.api.meta.DeclaredStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.RangeStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ValueRange;
import org.opendaylight.yangtools.yang.parser.rfc7950.stmt.AbstractDeclaredStatement.WithArgument.WithSubstatements;

final class RegularRangeStatement extends WithSubstatements<List<ValueRange>> implements RangeStatement {
    RegularRangeStatement(final String rawArgument, final List<ValueRange> argument,
            final ImmutableList<? extends DeclaredStatement<?>> substatements) {
        super(rawArgument, argument, substatements);
    }
}
