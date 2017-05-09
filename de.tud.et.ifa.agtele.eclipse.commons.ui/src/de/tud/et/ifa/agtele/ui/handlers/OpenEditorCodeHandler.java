package de.tud.et.ifa.agtele.ui.handlers;

import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.codegen.ecore.genmodel.GenBase;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.codegen.ecore.genmodel.impl.GenPackageImpl;
import org.eclipse.emf.ecore.impl.EPackageImpl;

/**
 * An {@link OpenCodeHandler} that opens the generated Java editor file for the
 * model object.
 *
 * @author mfreund
 */
public class OpenEditorCodeHandler extends OpenCodeHandler {

	@Override
	protected List<Class<?>> getAllowedElementTypes() {
		return Arrays.asList(EPackageImpl.class, GenPackageImpl.class);
	}

	@Override
	protected String getDirectory(GenBase selectedElement) {

		return selectedElement.getGenModel().getEditorDirectory();
	}

	@Override
	protected String getQualifiedClassName(GenBase element) {

		if (element instanceof GenPackage) {
			return ((GenPackage) element).getQualifiedEditorClassName();
		} else {
			return null;
		}
	}
}