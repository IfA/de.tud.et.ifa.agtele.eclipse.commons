/**
 */
package de.tud.et.ifa.agtele.eclipse.webpage.webpagemodel.impl;

import de.tud.et.ifa.agtele.eclipse.webpage.webpagemodel.SubPage;
import de.tud.et.ifa.agtele.eclipse.webpage.webpagemodel.WebPageModelPackage;

import java.util.Collection;

import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Sub Page</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link de.tud.et.ifa.agtele.eclipse.webpage.webpagemodel.impl.SubPageImpl#getSubPage <em>Sub Page</em>}</li>
 * </ul>
 *
 * @generated
 */
public class SubPageImpl extends AbstractHTMLImpl implements SubPage {
	/**
	 * The cached value of the '{@link #getSubPage() <em>Sub Page</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSubPage()
	 * @generated
	 * @ordered
	 */
	protected EList<SubPage> subPage;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected SubPageImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return WebPageModelPackage.Literals.SUB_PAGE;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<SubPage> getSubPage() {
	
		if (subPage == null) {
			subPage = new EObjectContainmentEList<SubPage>(SubPage.class, this, WebPageModelPackage.SUB_PAGE__SUB_PAGE);
		}
		return subPage;
	}
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case WebPageModelPackage.SUB_PAGE__SUB_PAGE:
				return ((InternalEList<?>)getSubPage()).basicRemove(otherEnd, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case WebPageModelPackage.SUB_PAGE__SUB_PAGE:
				return getSubPage();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case WebPageModelPackage.SUB_PAGE__SUB_PAGE:
				getSubPage().clear();
				getSubPage().addAll((Collection<? extends SubPage>)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case WebPageModelPackage.SUB_PAGE__SUB_PAGE:
				getSubPage().clear();
				return;
		}
		super.eUnset(featureID);
	}

 	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case WebPageModelPackage.SUB_PAGE__SUB_PAGE:
				return subPage != null && !subPage.isEmpty();
		}
		return super.eIsSet(featureID);
	}
} //SubPageImpl
