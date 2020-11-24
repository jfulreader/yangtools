/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.parser.spi.meta;

import static com.google.common.base.Verify.verifyNotNull;

import com.google.common.annotations.Beta;
import com.google.common.base.VerifyException;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.model.api.meta.StatementDefinition;
import org.opendaylight.yangtools.yang.model.api.meta.StatementSource;
import org.opendaylight.yangtools.yang.parser.spi.source.StatementSourceReference;

/**
 * Common interface for all statement contexts, exposing information which is always available. Note this includes only
 * stateless information -- hence we have {@link #rawArgument()} but do not have an equivalent {@code argument()}.
 */
@Beta
public interface CommonStmtCtx {
    /**
     * See {@link StatementSupport#getPublicView()}.
     */
    @NonNull StatementDefinition publicDefinition();

    /**
     * Returns a reference to statement source.
     *
     * @return reference of statement source
     */
    @NonNull StatementSourceReference sourceReference();

    /**
     * Returns the origin of the statement.
     *
     * @return origin of statement
     */
    default @NonNull StatementSource source() {
        return sourceReference().getStatementSource();
    }

    /**
     * Return the statement argument in literal format.
     *
     * @return raw statement argument string, or null if this statement does not have an argument.
     */
    @Nullable String rawArgument();

    /**
     * Return the statement argument in literal format.
     *
     * @return raw statement argument string
     * @throws VerifyException if this statement does not have an argument
     */
    default @NonNull String getRawArgument() {
        return verifyNotNull(rawArgument(), "Statement context %s does not have an argument", this);
    }
}
