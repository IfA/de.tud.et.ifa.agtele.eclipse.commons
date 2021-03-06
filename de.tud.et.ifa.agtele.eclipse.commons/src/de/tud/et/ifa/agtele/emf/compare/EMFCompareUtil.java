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
package de.tud.et.ifa.agtele.emf.compare;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.diff.DefaultDiffEngine;
import org.eclipse.emf.compare.diff.DiffBuilder;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

/**
 * A class that provides various utility methods for performing comparisons with the help of {@link EMFCompare}.
 *
 * @author mfreund
 */
public class EMFCompareUtil {

	/**
	 * This creates an instance.
	 */
	private EMFCompareUtil() {
		// just to prevent the implicit public constructor
		//
	}

	/**
	 * This finds and returns a match for the given <em>origin</em> after comparing the two {@link Notifier Notifiers}
	 * <em>left</em> and <em>right</em>.
	 * <p />
	 * Note: If the given <em>origin</em> is part of <em>left</em>, the returned match will be part of <em>right</em>
	 * and vice versa.
	 * <p />
	 * Note: This method only returns a match of the same {@link EClass type} - if there is a matching element of
	 * another type, <em>null</em> is returned instead.
	 *
	 * @see #getMatchOfSameType(Notifier, Notifier, EObject)
	 *
	 * @param left
	 *            The {@link Notifier} representing the left part of the comparison (this might e.g. be a resource, a
	 *            model, or an excerpt of a model).
	 * @param right
	 *            The {@link Notifier} representing the right part of the comparison (this might e.g. be a resource, a
	 *            model, or an excerpt of a model).
	 * @param origin
	 *            The element for that a match shall be returned.
	 * @return The returned match of the same {@link EClass type} as the given <em>origin</em>, or <em>null</em> if no
	 *         match of the same type was found.
	 */
	public static EObject getMatch(Notifier left, Notifier right, EObject origin) {

		// Compare the left and right sides
		//
		Comparison result = EMFCompareUtil.compare(left, right);

		// Find a match for the origin
		//
		Match match = result.getMatch(origin);

		if (match == null) {
			return null;
		}

		if (match.getLeft().equals(origin)) {
			return match.getRight();
		} else {
			return match.getLeft();
		}
	}

	public static List<EObject> getMatches(Notifier left, Notifier right, List<EObject> origin) {
		// Compare the left and right sides
		//
		Comparison comparison = EMFCompareUtil.compare(left, right);

		ArrayList<EObject> result = new ArrayList<>(origin.size());

		for (int i = 0; i < origin.size(); i += 1) {
			EObject obj = origin.get(i);
			Match match = comparison.getMatch(obj);
			if (match != null) {
				if (match.getLeft() == obj && match.getRight() != null) {
					result.add(i, match.getRight());
				} else if (match.getLeft() != null) {
					result.add(i, match.getLeft());
				}
			}
		}
		return result;
	}

	public static Comparison compare(Notifier left, Notifier right) {
		IComparisonScope scope = new DefaultComparisonScope(left, right, null);
		EMFCompare comparator = EMFComparatorFactory.getComparator(new DefaultDiffEngine(new DiffBuilder()));
		return comparator.compare(scope);
	}

	/**
	 * This finds and returns a match for the given <em>origin</em> after comparing the two {@link Notifier Notifiers}
	 * <em>left</em> and <em>right</em>.
	 * <p />
	 * Note: If the given <em>origin</em> is part of <em>left</em>, the returned match will be part of <em>right</em>
	 * and vice versa.
	 * <p />
	 * Note: This method only returns a match of the same {@link EClass type} - if there is a matching element of
	 * another type, <em>null</em> is returned instead.
	 *
	 * @see #getMatch(Notifier, Notifier, EObject)
	 *
	 * @param <T>
	 *            The concrete type of the match to be found and returned.
	 * @param left
	 *            The {@link Notifier} representing the left part of the comparison (this might e.g. be a resource, a
	 *            model, or an excerpt of a model).
	 * @param right
	 *            The {@link Notifier} representing the right part of the comparison (this might e.g. be a resource, a
	 *            model, or an excerpt of a model).
	 * @param origin
	 *            The element for that a match shall be returned.
	 * @return The returned match of the same {@link EClass type} as the given <em>origin</em>, or <em>null</em> if no
	 *         match of the same type was found.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends EObject> T getMatchOfSameType(Notifier left, Notifier right, T origin) {

		// Find a match for the origin
		//
		EObject match = EMFCompareUtil.getMatch(left, right, origin);

		return match == null || !origin.eClass().equals(match.eClass()) ? null : (T) match;

	}

	/**
	 * This compares the two {@link Notifier Notifiers} <em>left</em> and <em>right</em> and checks if they match
	 * without difference.
	 *
	 * @param left
	 *            The {@link Notifier} representing the left part of the comparison (this might e.g. be a resource, a
	 *            model, or an excerpt of a model).
	 * @param right
	 *            The {@link Notifier} representing the right part of the comparison (this might e.g. be a resource, a
	 *            model, or an excerpt of a model).
	 * @return '<em><b>true</b></em>' if the two given notifiers match, '<em><b>false</b></em>' otherwise.
	 */
	public static boolean isMatch(Notifier left, Notifier right) {

		Comparison result = EMFCompareUtil.compare(left, right);

		return result.getDifferences().isEmpty();

	}
}
