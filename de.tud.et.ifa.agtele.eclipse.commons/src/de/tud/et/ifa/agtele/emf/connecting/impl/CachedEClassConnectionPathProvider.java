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
package de.tud.et.ifa.agtele.emf.connecting.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EPackage;

import de.tud.et.ifa.agtele.emf.connecting.EClassConnectionPath;
import de.tud.et.ifa.agtele.emf.connecting.EClassConnectionPathProvider;
import de.tud.et.ifa.agtele.emf.connecting.EClassConnectionPathRequirement;

/**
 * An {@link EClassConnectionPathProvider} that redirects to an {@link EClassConnectionPathFactory} in order to
 * determine possible connection paths and caches determined paths to reduce computation time.
 *
 * @author mfreund
 */
@SuppressWarnings("javadoc")
public class CachedEClassConnectionPathProvider implements EClassConnectionPathProvider {

	private final Map<EClassConnectionPathRequirement, List<EClassConnectionPath>> cachedConnections;

	private final EClassConnectionPathFactory pathFactory;

	public CachedEClassConnectionPathProvider(Collection<EPackage> ePackages) {

		this(ePackages, null);
	}

	public CachedEClassConnectionPathProvider(Collection<EPackage> ePackages, Logger logger) {

		cachedConnections = Collections.synchronizedMap(new HashMap<>());
		pathFactory = logger != null ? new EClassConnectionPathFactory(ePackages, logger)
				: new EClassConnectionPathFactory(ePackages);
	}

	@Override
	public List<EClassConnectionPath> getConnections(EClassConnectionPathRequirement connectionRequirement) {

		return cachedConnections.computeIfAbsent(connectionRequirement, pathFactory::getConnections);
	}

}
