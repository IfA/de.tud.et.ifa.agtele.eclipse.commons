package de.tud.et.ifa.agtele.ui.views;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.*;

import de.tud.et.ifa.agtele.emf.AgteleEcoreUtil;
import de.tud.et.ifa.agtele.resources.BundleContentHelper;
import de.tud.et.ifa.agtele.ui.AgteleUIPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.edit.command.CommandParameter;
import org.eclipse.emf.edit.provider.IItemPropertyDescriptor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.HelpEvent;

/**
 * This view displays Amino UIs help contents
 */

public class EMFModelHelpView extends ViewPart implements IPersistable {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "de.tud.et.ifa.agtele.ui.views.EMFModelHelpView";

	private Browser browser;
	private Action linkAction;

	private String currentText;
	
	private ISelectionListener selectionListener;

	private Boolean linkEditor;

	/**
	 * The constructor.
	 */
	public EMFModelHelpView() {
		selectionListener = new ISelectionListener() {

			@Override
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				showHelp(selection);
			}
		};

		linkEditor = AgteleUIPlugin.getPlugin().getDialogSettings().getBoolean("link");
		if (linkEditor)
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService()
					.addSelectionListener(selectionListener);

	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	@Override
	public void createPartControl(Composite parent) {
		browser = new Browser(parent, SWT.NONE);

		makeActions();
		contributeToActionBars();
		
		currentText = AgteleUIPlugin.getPlugin().getDialogSettings().get("browserText");
		browser.setText(currentText);
		if (linkEditor) {
			show();
		}
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		browser.setFocus();
	}

	@Override
	public void dispose() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService()
				.removeSelectionListener(selectionListener);
		AgteleUIPlugin.getPlugin().getDialogSettings().put("link", linkEditor);
		
		AgteleUIPlugin.getPlugin().getDialogSettings().put("browserText", currentText);
		
		super.dispose();
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(linkAction);
	}

	private void makeActions() {
		linkAction = new Action("Link Editor",
				BundleContentHelper.getBundleImageDescriptor(AgteleUIPlugin.PLUGIN_ID, "icons/synced.gif")) {
			public void run() {
				linkEditor = !linkEditor;
				if (linkEditor) {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService()
							.addSelectionListener(selectionListener);
				} else {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService()
							.removeSelectionListener(selectionListener);
				}
				linkAction.setChecked(linkEditor);
			}
		};
		linkAction.setToolTipText("Link with Selection");
		linkAction.setChecked(linkEditor);
	}

	/**
	 * Opens the view and shows help based on the current selection
	 */
	public static void show() {
		showHelp(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection());
	}

	/**
	 * Opens the {@link EMFModelHelpView}, generates the help page for the given
	 * AminoUI {@link EObject} and displays it
	 * 
	 * @param eObject
	 *            AminoUI model element
	 */
	public static void showHelp(EObject eObject) {
		showText(getHtml(eObject));
	}

	/**
	 * Opens the {@link EMFModelHelpView} and displays text in it
	 * 
	 * @param text
	 *            text to be displayed
	 */
	public static void showText(String text) {
		try {
			EMFModelHelpView helpView = (EMFModelHelpView) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getActivePage().showView(ID, null, IWorkbenchPage.VIEW_VISIBLE);
			helpView.browser.setText(text);
			helpView.currentText = text;
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Opens the {@link EMFModelHelpView} and shows the index help page
	 */
	public static void showIndex() {
		// TODO create index.html that explains how to use this help
		showText("EMF Model Help Index");
	}

	/**
	 * Determine the current selection and show the help if it is an
	 * {@link EObject}
	 * 
	 * @param selection
	 */
	private static void showHelp(ISelection selection) {
		if (selection instanceof StructuredSelection) {
			StructuredSelection structuredSelection = (StructuredSelection) selection;
			// show Help if one EObject is selected
			if (structuredSelection.size() == 1) {
				if (structuredSelection.getFirstElement() instanceof EObject) {
					EMFModelHelpView.showHelp((EObject) structuredSelection.getFirstElement());
				}
			}
			// else show nothing new
			else {
				Iterator<?> it = structuredSelection.iterator();
				while (it.hasNext()) {
					Object type = it.next();
					if (!(type instanceof EObject)) {
						return;
					}
				}
				if (!structuredSelection.isEmpty()) {
					EMFModelHelpView.showText(AgteleUIPlugin.getPlugin().getString("_UI_EMFModelHelpView_TooManyElementsSelected"));
				}
			}
		}
	}

	/**
	 * Generates the documentation page of an eObject
	 * 
	 * @param eObject
	 * @return
	 */
	private static String getHtml(EObject eObject) {
		String html = "<HTML><BODY>";

		html += getEClassHelp(eObject);
		html += getPropertyHelp(eObject);
		html += getContainmentReferenceHelp(eObject);

		html += "</BODY></HTML>";

		return html;
	}
	
	/**
	 * Generates the Documentation of the EClass of a given {@link EObject}
	 * 
	 * @param eObject
	 * @return
	 */
	private static String getEClassHelp(EObject eObject) {
		String eClassHelp = "";
		// EClass Name
		eClassHelp += eObject.eClass().getName();

		// EClass Documentation
		if (EcoreUtil.getDocumentation(eObject.eClass()) != null) {
			eClassHelp += "<br/><br/>" + EcoreUtil.getDocumentation(eObject.eClass());
		}
		return eClassHelp;
	}
	
	/**
	 * Generates the Documentation of the properties of a given {@link EObject}
	 * 
	 * @param eObject
	 * @return
	 */
	private static String getPropertyHelp(EObject eObject) {
		String propertyHelp = "";
		// Non-containment reference and attribute documentation
		if (AgteleEcoreUtil.getAdapterFactoryItemDelegatorFor(eObject) != null) {
			List<IItemPropertyDescriptor> propertyDescriptors = AgteleEcoreUtil.getAdapterFactoryItemDelegatorFor(eObject).getPropertyDescriptors(eObject);
			String attributeHtml = "";
			String nonContainmentReferenceHtml = "";
			
			for (IItemPropertyDescriptor itemPropertyDescriptor : propertyDescriptors) {
				// EAttribute
				if (itemPropertyDescriptor.getFeature(null) instanceof EAttribute) {
					EAttribute attr = (EAttribute) itemPropertyDescriptor.getFeature(null);
					attributeHtml += "<br/><br/>" + attr.getName() + " : " + attr.getEAttributeType().getName();
					attributeHtml += EcoreUtil.getDocumentation(attr) != null ? "<br/>"+EcoreUtil.getDocumentation(attr) : "";
				}
				// Non-Containment References
				else if (itemPropertyDescriptor.getFeature(null) instanceof EReference) {
					EReference nonContainmentReference = (EReference) itemPropertyDescriptor.getFeature(null);
					nonContainmentReferenceHtml += "<br/><br/>" + nonContainmentReference.getName() + " : " + nonContainmentReference.getEGenericType().getEClassifier().getName();
					nonContainmentReferenceHtml += EcoreUtil.getDocumentation(nonContainmentReference) != null ? "<br/>"+EcoreUtil.getDocumentation(nonContainmentReference) : "";
				}
			}
			// add the attribute and reference documentation to the actual documentation
			if (!attributeHtml.isEmpty()) {
				propertyHelp += "<br/><br/>Attributes" + attributeHtml;
			}
			if (!nonContainmentReferenceHtml.isEmpty()) {
				propertyHelp += "<br/><br/>Non-containment References" + nonContainmentReferenceHtml;
			}
		}
		return propertyHelp;
	}

	/**
	 * Generates the Documentation of the {@link EReference Containment
	 * References} and the possible Children of a given {@link EObject}
	 * 
	 * @param eObject
	 * @return
	 */
	private static String getContainmentReferenceHelp(EObject eObject) {
		String eClassHelp = "";
		// Containment reference and possible targets documentation
		if (!AgteleEcoreUtil.getEditingDomainFor(eObject).getNewChildDescriptors(eObject, null).isEmpty()) {
			LinkedHashMap<EStructuralFeature, List<EObject>> childDescriptors = sortChildDescriptors(
					AgteleEcoreUtil.getEditingDomainFor(eObject).getNewChildDescriptors(eObject, null));
			eClassHelp += "<br/><br/>Containment-References and possible children";
			
			for (EStructuralFeature reference : childDescriptors.keySet()) {
				// EReference documentation
				eClassHelp += "<br/><br/>" + reference.getName();
				eClassHelp += EcoreUtil.getDocumentation(reference) != null ? "<br/>" + EcoreUtil.getDocumentation(reference) : "";
				for (EObject eValue : childDescriptors.get(reference)) {
					// Children Documentation
					eClassHelp += "<br/><br/>" + eValue.eClass().getName();
					eClassHelp += EcoreUtil.getDocumentation(eValue.eClass()) != null ? "<br/>" + EcoreUtil.getDocumentation(eValue.eClass()) : "";
				}
			}

		}
		return eClassHelp;
	}
	
	/**
	 * Sorts {@link CommandParameter}s of the getNewChildDescriptors method of
	 * an EObject into a {@link HashMap} in such way, that each child (
	 * {@link EObject}) is matched with its {@link EStructuralFeature}
	 * 
	 * @param childDescriptors
	 *            Collection of CommandParameters (ChildDescriptors)
	 * @return Sorted HashMap
	 */
	private static LinkedHashMap<EStructuralFeature, List<EObject>> sortChildDescriptors(Collection<?> childDescriptors) {
		LinkedHashMap<EStructuralFeature, List<EObject>> children = new LinkedHashMap<EStructuralFeature, List<EObject>>();

		for (Object childDescriptor : childDescriptors) {
			if (childDescriptor instanceof CommandParameter) {
				EStructuralFeature feature = ((CommandParameter) childDescriptor).getEStructuralFeature();
				if (children.containsKey(feature)) {
					children.get(feature).add(((CommandParameter) childDescriptor).getEValue());
				} else {
					ArrayList<EObject> objects = new ArrayList<>();
					objects.add(((CommandParameter) childDescriptor).getEValue());
					children.put(feature, objects);
				}
			}
		}
		return children;
	}

	public static class HelpListener implements org.eclipse.swt.events.HelpListener {

		@Override
		public void helpRequested(HelpEvent e) {
			show();
		}
	}
}