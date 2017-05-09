package de.tud.et.ifa.agtele.ui.providers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.provider.EModelElementItemProvider;
import org.eclipse.emf.edit.provider.ComposedImage;
import org.eclipse.emf.edit.provider.IItemLabelProvider;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

import de.tud.et.ifa.agtele.resources.BundleContentHelper;
import de.tud.et.ifa.agtele.ui.providers.AgteleEcoreContentProvider.NonContainedChildWrapper;

public class AgteleEcoreAdapterFactoryLabelProvider extends AdapterFactoryLabelProvider {

	public AgteleEcoreAdapterFactoryLabelProvider(AdapterFactory adapterFactory) {
		super(adapterFactory);
	}

	@Override
	public Image getImage(Object object) {
		boolean inherited = false;
		Image result = null;
		if (object instanceof NonContainedChildWrapper) {
			object = ((NonContainedChildWrapper) object).noncontainedChild;
			inherited = true;
		}
		if (object instanceof EReference || object instanceof EClass) {

			EModelElementItemProvider labelProvider = (EModelElementItemProvider) this.adapterFactory
					.adapt(object, IItemLabelProvider.class);

			Object image = labelProvider.getImage(object);
			String imagePath = null;

			if (object instanceof EReference && ((EReference) object).isContainment()) {
				imagePath = "icons/ContainmentReference.gif";
			} else if (object instanceof EClass && ((EClass)object).isInterface()) {
				imagePath = "icons/EInterface.gif";
			} else if (object instanceof EClass && ((EClass)object).isAbstract()) {
				imagePath = "icons/EAbstractClass.gif";
			}

			if (imagePath != null) {
				if (image instanceof ComposedImage) {
					// The first sub-image of the composed image always represent the 'base' image (i.e.
					// the icon for the 'Reference'). Thus we can simply replace this with our special
					// icon.
					//
					((ComposedImage) image).getImages().set(0,
							BundleContentHelper.getBundleImage(
									"de.tud.et.ifa.agtele.eclipse.commons.ui",
									imagePath));

					result = this.getImageFromObject(image);
				} else {
					result = this.getImageFromObject(BundleContentHelper.getBundleImage(
							"de.tud.et.ifa.agtele.eclipse.commons.ui",
							imagePath));
				}
			}
		}
		if (result == null) {
			result = super.getImage(object);
		}

		if (inherited) {
			List<Image> imgs = new ArrayList<>();
			imgs.add(BundleContentHelper.getBundleImage("de.tud.et.ifa.agtele.eclipse.commons.ui",
					"icons/InheritedUnderlay.gif"));
			imgs.add(result);
			ComposedImage inheritedImage = new ComposedImage(imgs);
			result = this.getImageFromObject(inheritedImage);
		}
		return result;
	}

	/**
	 * Determines, if a feature or an operation displayed as contained in an
	 * eClass is actually inherited from another eClass.
	 *
	 * @param object
	 * @return True, if the container of the object is an EClass, if the Object
	 *         is a EStructuralFeature or an EOperation and the
	 */
	public static boolean isChildInherited(Object object) {

		if (!(object instanceof EObject) && !(object instanceof NonContainedChildWrapper)
				&& !(object instanceof NonContainedChildWrapper
						&& ((NonContainedChildWrapper) object).getNoncontainedChild() instanceof EObject)) {
			return false;
		}
		EObject parent = object instanceof NonContainedChildWrapper
				? (EObject) ((NonContainedChildWrapper) object).getParentNode() : ((EObject) object).eContainer();
				if (parent == null || !(parent instanceof EClass)) {
					return false;
				}

				EClass eClass = (EClass) parent;
				if (object instanceof NonContainedChildWrapper) {
					object = ((NonContainedChildWrapper) object).noncontainedChild;
				}

				if (object instanceof EStructuralFeature) {
					return !eClass.getEStructuralFeatures().contains(object);
				} else if (object instanceof EOperation) {
					return !eClass.getEOperations().contains(object);
				}
				return false;
	}

	@Override
	public String getText(Object object) {
		String result = super.getText(object);

		if (AgteleEcoreAdapterFactoryLabelProvider.isChildInherited(object)
				&& object instanceof NonContainedChildWrapper
				&& ((NonContainedChildWrapper) object).getParentNode() instanceof EClass
				&& ((NonContainedChildWrapper) object).getNoncontainedChild() instanceof EObject) {
			result = super.getText(((NonContainedChildWrapper) object).getNoncontainedChild());
			EClass eClass = (EClass) ((EObject) ((NonContainedChildWrapper) object).getNoncontainedChild())
					.eContainer();

			result += " (inherited from '" + eClass.getName() + "' in " + eClass.getEPackage().getNsURI() + ")";
		}

		return result;
	}

	@Override
	public StyledString getStyledText(Object object) {
		StyledString result = super.getStyledText(object);

		if (AgteleEcoreAdapterFactoryLabelProvider.isChildInherited(object)
				&& object instanceof NonContainedChildWrapper
				&& ((NonContainedChildWrapper) object).getParentNode() instanceof EClass
				&& ((NonContainedChildWrapper) object).getNoncontainedChild() instanceof EObject) {
			result = super.getStyledText(((NonContainedChildWrapper) object).getNoncontainedChild());
			EClass eClass = (EClass) ((EObject) ((NonContainedChildWrapper) object).getNoncontainedChild())
					.eContainer();

			result.append(" (inherited from '" + eClass.getName() + "' in " + eClass.getEPackage().getNsURI() + ")",
					StyledString.DECORATIONS_STYLER);
		}

		return result;
	}
}