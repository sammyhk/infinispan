/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.clustering.infinispan.io;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * Externalizer for Externalizable objects.
 * @author Paul Ferraro
 */
public class ExternalizableExternalizer<T extends Externalizable> extends AbstractSimpleExternalizer<T> {
    private static final long serialVersionUID = -5938201796254055389L;

    public ExternalizableExternalizer(Class<T> targetClass) {
        super(targetClass);
    }

    @Override
    public void writeObject(ObjectOutput output, T object) throws IOException {
        object.writeExternal(output);
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        PrivilegedExceptionAction<T> action = () -> this.getTargetClass().newInstance();
        try {
            T object = AccessController.doPrivileged(action);
            object.readExternal(input);
            return object;
        } catch (PrivilegedActionException e) {
            throw new IOException(e.getCause());
        }
    }
}