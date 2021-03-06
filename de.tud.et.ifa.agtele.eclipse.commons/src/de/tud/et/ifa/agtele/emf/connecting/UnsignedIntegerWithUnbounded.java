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
package de.tud.et.ifa.agtele.emf.connecting;

import java.util.Objects;

/**
 * A common superclass for classes that 1) wrap a (positive) integer but 2) also have the notion of an
 * <em>unbounded</em> value. This unbounded value is thereby represented by a negative {@link #value}.
 *
 * @author mfreund
 */
public abstract class UnsignedIntegerWithUnbounded implements Comparable<UnsignedIntegerWithUnbounded> {

	protected final int value;

	protected UnsignedIntegerWithUnbounded(int value) {

		this.value = value < 0 ? -1 : value;
	}

	/**
	 * @return the {@link #value}
	 */
	public int getValue() {

		return this.value;
	}

	/**
	 * @return <em>true</em> if this represents an <em>unbounded</em> value
	 */
	public boolean isUnbounded() {

		return this.value < 0;
	}

	@Override
	public boolean equals(Object obj) {

		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}

		return ((UnsignedIntegerWithUnbounded) obj).value == this.value;
	}

	@Override
	public int hashCode() {

		return Objects.hash(this.value);
	}

	@Override
	public String toString() {

		return String.valueOf(this.value);
	}

	@Override
	public int compareTo(UnsignedIntegerWithUnbounded o) {

		if (this.isUnbounded()) {
			return o.isUnbounded() ? 0 : 1;
		} else if (o.isUnbounded()) {
			return -1;
		} else {
			return Integer.compare(this.value, o.value);
		}
	}

}
