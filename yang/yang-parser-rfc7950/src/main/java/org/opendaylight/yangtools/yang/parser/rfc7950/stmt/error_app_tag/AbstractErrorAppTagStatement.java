/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.parser.rfc7950.stmt.error_app_tag;

import org.opendaylight.yangtools.yang.model.api.YangStmtMapping;
import org.opendaylight.yangtools.yang.model.api.meta.StatementDefinition;
import org.opendaylight.yangtools.yang.model.api.stmt.ErrorAppTagStatement;
import org.opendaylight.yangtools.yang.parser.rfc7950.stmt.AbstractDeclaredStatement.WithRawStringArgument;
import org.opendaylight.yangtools.yang.parser.spi.meta.StmtContext;

abstract class AbstractErrorAppTagStatement extends WithRawStringArgument implements ErrorAppTagStatement {
    AbstractErrorAppTagStatement(final StmtContext<String, ?, ?> context) {
        super(context);
    }

    @Override
    public final StatementDefinition statementDefinition() {
        return YangStmtMapping.ERROR_APP_TAG;
    }
}