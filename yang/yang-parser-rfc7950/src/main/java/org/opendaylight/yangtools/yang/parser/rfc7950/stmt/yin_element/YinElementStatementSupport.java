/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.parser.rfc7950.stmt.yin_element;

import com.google.common.collect.ImmutableList;
import org.opendaylight.yangtools.yang.model.api.YangStmtMapping;
import org.opendaylight.yangtools.yang.model.api.meta.DeclaredStatement;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.YinElementEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.YinElementStatement;
import org.opendaylight.yangtools.yang.parser.rfc7950.stmt.BaseBooleanStatementSupport;
import org.opendaylight.yangtools.yang.parser.spi.meta.StmtContext;
import org.opendaylight.yangtools.yang.parser.spi.meta.SubstatementValidator;

public final class YinElementStatementSupport
        extends BaseBooleanStatementSupport<YinElementStatement, YinElementEffectiveStatement> {
    private static final SubstatementValidator SUBSTATEMENT_VALIDATOR = SubstatementValidator.builder(
        YangStmtMapping.YIN_ELEMENT).build();
    private static final YinElementStatementSupport INSTANCE = new YinElementStatementSupport();

    private YinElementStatementSupport() {
        super(YangStmtMapping.YIN_ELEMENT,
            new EmptyYinElementEffectiveStatement(new EmptyYinElementStatement(Boolean.FALSE)),
            new EmptyYinElementEffectiveStatement(new EmptyYinElementStatement(Boolean.TRUE)),
            CopyPolicy.REJECT);
    }

    public static YinElementStatementSupport getInstance() {
        return INSTANCE;
    }

    @Override
    protected SubstatementValidator getSubstatementValidator() {
        return SUBSTATEMENT_VALIDATOR;
    }

    @Override
    protected YinElementStatement createDeclared(final StmtContext<Boolean, YinElementStatement, ?> ctx,
            final ImmutableList<? extends DeclaredStatement<?>> substatements) {
        return new RegularYinElementStatement(ctx.getArgument(), substatements);
    }

    @Override
    protected YinElementEffectiveStatement createEffective(final YinElementStatement declared,
            final ImmutableList<? extends EffectiveStatement<?, ?>> substatements) {
        return new RegularYinElementEffectiveStatement(declared, substatements);
    }

    @Override
    protected YinElementEffectiveStatement createEmptyEffective(final YinElementStatement declared) {
        return new EmptyYinElementEffectiveStatement(declared);
    }
}
