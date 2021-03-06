/*******************************************************************************
 * Copyright (C) 2016-2018 Institute of Automation, TU Dresden.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Institute of Automation, TU Dresden - initial API and implementation
 ******************************************************************************/
package de.tud.et.ifa.agtele.emf.edit;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

/**
 * The agtele XMIResource will be used by the
 * {@link AgteleAdapterFactoryEditingDomain} in order to replace a standard
 * {@link XMIResource}.
 *
 * @author Baron
 *
 */
public class AgteleXMIResourceImpl extends XMIResourceImpl implements IAgteleResource {

	protected boolean useUUIDs = false;

	public AgteleXMIResourceImpl(URI uri) {
		super(uri);
	}

	@Override
	public boolean isSaveUsingUUIDs() {
		return this.useUUIDs;
	}

	@Override
	public void setSaveUsingUUIDs(boolean useUUIDs) {
		this.useUUIDs = useUUIDs;
	}

	@Override
	protected boolean useUUIDs() {
		return this.useUUIDs;
	}
}
