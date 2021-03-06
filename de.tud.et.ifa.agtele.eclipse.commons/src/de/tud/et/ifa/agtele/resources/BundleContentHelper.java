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
package de.tud.et.ifa.agtele.resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;

/**
 * This allows to retrieve assets from a bundle (an installed eclipse plug-in).
 *
 */
public interface BundleContentHelper {

	/**
	 * Get a file from a plugin
	 *
	 * @param bundleId
	 *            The id of the plugin that shall be used to resolve the path.
	 * @param path
	 *            path relative to the bundle root
	 * @return file from the bundle or <em>null</em> if no file could be retrieved.
	 */
	public static File getBundleEntry(String bundleId, String path) {

		Bundle bundle = Platform.getBundle(bundleId);

		if (bundle == null) {
			throw new RuntimeException("Could not load bundle with id '" + bundleId + "'");
		}

		URL fileURL = bundle.getEntry(path);

		if (fileURL == null) {
			throw new RuntimeException("Could not resolve entry '" + path + "' in bundle '" + bundleId + "'");
		}

		File f = null;

		try {
			URL resolvedFileURL = FileLocator.toFileURL(fileURL);
			f = new File(new URI(resolvedFileURL.getProtocol(), resolvedFileURL.getPath(), null));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return f;
	}

	/**
	 * Get contents of a folder inside a plugin.
	 *
	 * @param dir
	 *            path of the folder relative to the bundle root
	 * @param bundleId
	 *            The id of the plugin that shall be used to resolve the path.
	 * @return list of files from the bundle folder
	 */
	public static List<File> getBundleContents(String bundleId, String dir) {

		Bundle bundle = Platform.getBundle(bundleId);
		ArrayList<File> files = new ArrayList<>();

		// get all bundle entries of the folder
		Enumeration<String> entries = bundle.getEntryPaths(dir);

		// skip if there are no entries in the folder
		if (entries != null) {
			// iterate over the BundleContents
			for (String file : Collections.list(bundle.getEntryPaths(dir))) {
				// get the file from the bundle
				File f = BundleContentHelper.getBundleEntry(bundleId, file);

				// add the file to the file list
				files.add(f);

				// if the current file is a directory then get its entry recursively
				if (f.isDirectory()) {
					files.addAll(BundleContentHelper.getBundleContents(bundleId, file));
				}
			}
		}
		return files;
	}

	/**
	 * Get an image descriptor from a plugin.
	 *
	 * @see #getBundleImage(String, String)
	 *
	 * @param bundleId
	 *            The id of the plugin that shall be used to resolve the path.
	 * @param path
	 *            path relative to the bundle root
	 * @return The image descriptor resolved from the bundle or <em>null</em> if no descriptor could be retrieved.
	 */
	public static ImageDescriptor getBundleImageDescriptor(String bundleId, String path) {

		Bundle bundle = Platform.getBundle(bundleId);
		return ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path(path), null));
	}

	/**
	 * Get an image from a plugin.
	 *
	 * @see #getBundleImageDescriptor(String, String)
	 *
	 * @param bundleId
	 *            The id of the plugin that shall be used to resolve the path.
	 * @param path
	 *            path relative to the bundle root
	 * @return The image resolved from the bundle or <em>null</em> if no image could be retrieved.
	 */
	public static Image getBundleImage(String bundleId, String path) {

		ImageDescriptor desc = BundleContentHelper.getBundleImageDescriptor(bundleId, path);
		return desc != null ? desc.createImage() : null;
	}

	/**
	 * Get the content of a file from a plugin as a single String.
	 *
	 * @param bundleId
	 *            The id of the plugin that shall be used to resolve the path.
	 * @param path
	 *            path relative to the bundle root
	 * @return The content of the file (lines are separated by a system-specific line separator).
	 * @throws IOException
	 *             If the resolved file does not exist or if something goes wrong while reading it.
	 */
	public static String getBundleFileString(String bundleId, String path) throws IOException {

		BufferedReader reader = new BufferedReader(new FileReader(BundleContentHelper.getBundleEntry(bundleId, path)));
		String line = null;
		StringBuilder stringBuilder = new StringBuilder();
		String ls = System.getProperty("line.separator");

		try {
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line);
				stringBuilder.append(ls);
			}

			return stringBuilder.toString();
		} finally {
			reader.close();
		}
	}

}
