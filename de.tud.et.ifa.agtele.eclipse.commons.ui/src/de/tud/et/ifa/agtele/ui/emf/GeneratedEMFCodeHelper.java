/**
 *
 */
package de.tud.et.ifa.agtele.ui.emf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.codegen.ecore.genmodel.GenClass;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.ui.part.FileEditorInput;

import de.tud.et.ifa.agtele.emf.AgteleEcoreUtil;
import de.tud.et.ifa.agtele.resources.ResourceHelper;
import de.tud.et.ifa.agtele.ui.util.UIHelper;

/**
 * A helper class that simplifies working with {@link CompilationUnit Java code} generated by EMF, e.g. by providing
 * access to the related {@link GeneratedEMFCodeHelper#getEcoreFile() Ecore metamodel} or
 * {@link GeneratedEMFCodeHelper#getMetamodelElement() metamodel element}.
 *
 * @author mfreund
 */
@SuppressWarnings("restriction")
public class GeneratedEMFCodeHelper {

	/**
	 * The {@link CompilationUnit} on which this helper is based.
	 */
	protected final CompilationUnit compilationUnit;

	/**
	 * The {@link IFile} containing the {@link #compilationUnit}.
	 */
	protected IFile javaFile;

	/**
	 * The {@link EObject} associated with the given {@link #compilationUnit}.
	 */
	protected EObject metamodelElement;

	/**
	 * The {@link IFile} containing the metamodel associated with the {@link #compilationUnit}.
	 */
	protected IFile ecoreFile;

	/**
	 * The {@link IFile} containing the genmodel associated with the {@link #compilationUnit}.
	 */
	protected IFile genmodelFile;

	/**
	 * This creates an instance for the given {@link CompilationUnit}.
	 *
	 * @param compilationUnit
	 *            The {@link CompilationUnit} about which this helper provides information.
	 */
	public GeneratedEMFCodeHelper(CompilationUnit compilationUnit) {

		this.compilationUnit = compilationUnit;

	}

	/**
	 * Returns the {@link CompilationUnit} on which this helper is based.
	 *
	 * @return the {@link #compilationUnit}
	 */
	public CompilationUnit getCompilationUnit() {

		return this.compilationUnit;
	}

	/**
	 * Returns the {@link IFile} containing the {@link #compilationUnit}.
	 *
	 * @return the {@link #javaFile}
	 */
	public IFile getJavaFile() {

		if (this.javaFile == null) {

			this.javaFile = ResourceHelper.getFileForURI(
					ResourceHelper.getURIForPathString(this.compilationUnit.getResource().getFullPath().toString()));
		}

		return this.javaFile;
	}

	/**
	 * Returns the {@link EMFGeneratedJavaFileType type} of the Java file represented by this helper.
	 *
	 * @return The {@link EMFGeneratedJavaFileType type}.
	 */
	public EMFGeneratedJavaFileType getFileType() {

		return EMFGeneratedJavaFileType.getFileType(this.getJavaFile().getName());
	}

	/**
	 * Returns the {@link EObject} associated with the {@link #compilationUnit CompilationUnit} that this helper is
	 * based on.
	 *
	 * @return the {@link #metamodelElement}
	 *
	 * @throws GeneratedEMFCodeHelperException
	 *             If the associated metamodel element could not be determined.
	 */
	public EObject getMetamodelElement() throws GeneratedEMFCodeHelperException {

		if (this.metamodelElement == null) {

			this.metamodelElement = this.determineAssociatedMetamodelElement(this.compilationUnit);
		}

		return this.metamodelElement;
	}

	/**
	 * Returns the {@link EObject} from the given {@link ResourceSet} that is
	 * {@link AgteleEcoreUtil#getEquivalentElementFrom(EObject, ResourceSet) equivalent} to the
	 * {@link #metamodelElement} that this helper represents.
	 *
	 * @see AgteleEcoreUtil#getEquivalentElementFrom(EObject, ResourceSet)
	 *
	 * @param resourceSet
	 *            The {@link ResourceSet} from which an equivalent element shall be returned.
	 * @return The equivalent element or '<em>null</em>' if no such element exists.
	 *
	 * @throws GeneratedEMFCodeHelperException
	 *             If the associated metamodel element could not be determined.
	 */
	public EObject getMetamodelElement(ResourceSet resourceSet) throws GeneratedEMFCodeHelperException {

		return AgteleEcoreUtil.getEquivalentElementFrom(this.getMetamodelElement(), resourceSet);
	}

	/**
	 * Returns the {@link IFile} representing the Ecore metamodel associated with the {@link #compilationUnit
	 * CompilationUnit} that this helper is based on.
	 *
	 * @return the {@link #ecoreFile}
	 *
	 * @throws GeneratedEMFCodeHelperException
	 *             If the associated metamodel file could not be determined.
	 */
	public IFile getEcoreFile() throws GeneratedEMFCodeHelperException {

		if (this.ecoreFile == null) {

			this.ecoreFile = ResourceHelper.getFileForResource(this.getMetamodelElement().eResource());

			if (this.ecoreFile == null) {
				throw new GeneratedEMFCodeHelperException("Unable to determine associated Ecore metamodel!");
			}
		}

		return this.ecoreFile;
	}

	/**
	 * Returns the {@link IFile} representing the GenModel associated with the {@link #compilationUnit CompilationUnit}
	 * that this helper is based on.
	 *
	 * @return the {@link #genmodelFile}
	 *
	 * @throws GeneratedEMFCodeHelperException
	 *             If the associated GenModel file could not be determined.
	 */
	public IFile getGenModelFile() throws GeneratedEMFCodeHelperException {

		if (this.genmodelFile == null) {

			this.genmodelFile = this.determineAssociatedGenModelFile();

			if (this.genmodelFile == null) {
				throw new GeneratedEMFCodeHelperException("Unable to determine associated GenModel file!");
			}
		}

		return this.genmodelFile;
	}

	/**
	 * Based on the base {@link #metamodelElement} associated with this helper, return a more specific element that
	 * represents the given {@link IJavaElement}.
	 * <p />
	 * E.g.: If the given <em>javaElement</em> is an {@link IMethod}, this may return the associated {@link EOperation}
	 * (if there is any).
	 *
	 * @param javaElement
	 *            A specific {@link IJavaElement element} of the Java file represented by this helper.
	 * @return A more specific {@link EObject element} (compared to the {@link #metamodelElement}) or the
	 *         {@link #metamodelElement} itself if no such element could be determined.
	 * @throws GeneratedEMFCodeHelperException
	 *             If an error occurred during the process.
	 */
	public EObject getMoreSpecificSelection(IJavaElement javaElement) throws GeneratedEMFCodeHelperException {

		EObject baseElement = this.getMetamodelElement();

		// If this represents an EPackage, we cannot return anything more
		// specialized.
		//
		if (!(baseElement instanceof EClass)) {
			return baseElement;
		}

		EClass eClass = (EClass) baseElement;

		IJavaElement baseJavaElement = javaElement;

		if (this.compilationUnit.findPrimaryType() != null) {

			// If this represents an internal/anonymous class, we first need to determine the containing
			// element (class or method) in the primary type defined in the given compilation unit.
			//
			while (baseJavaElement.getParent() != null && !this.compilationUnit.findPrimaryType()
					.equals(baseJavaElement.getParent().getPrimaryElement())) {

				baseJavaElement = baseJavaElement.getParent();
			}
		}

		if (baseJavaElement instanceof IMethod) {

			String methodName = baseJavaElement.getElementName();

			// Check if there is an EOperation corresponding to the selection
			//
			Optional<EOperation> eOperation = eClass.getEOperations().stream()
					.filter(o -> methodName.equals(o.getName())).findAny();
			if (eOperation.isPresent()) {
				return eOperation.get();
			}

			// Check if there is an EAttribute or EReference corresponding to
			// the selection
			//
			Optional<EStructuralFeature> eFeature = eClass.getEAllStructuralFeatures().stream()
					.filter(o -> methodName.equalsIgnoreCase("get" + o.getName())
							|| methodName.equalsIgnoreCase("is" + o.getName())
							|| methodName.equalsIgnoreCase("set" + o.getName())
							|| methodName.equalsIgnoreCase("add" + o.getName() + "propertydescriptor"))
					.findAny();
			if (eFeature.isPresent()) {
				return eFeature.get();
			}

		} else if (baseJavaElement instanceof IField) {

			String fieldName = baseJavaElement.getElementName();

			// Check if there is an EAttribute or EReference corresponding to
			// the selection
			//
			Optional<EStructuralFeature> eFeature = eClass.getEAllStructuralFeatures().stream()
					.filter(o -> fieldName.equalsIgnoreCase(o.getName())).findAny();
			if (eFeature.isPresent()) {
				return eFeature.get();
			}
		}

		return baseElement;
	}

	/**
	 * Determines a metamodel element for the compilation unit specified. Opened Ecore editors will be preferred over
	 * all available genmodels.
	 *
	 * @param root
	 *            The compilation unit to determine the metamodel element for.
	 * @return
	 * @throws GeneratedEMFCodeHelperException
	 *             If the associated metamodel element could not be determined.
	 */
	protected EObject determineAssociatedMetamodelElement(CompilationUnit root) throws GeneratedEMFCodeHelperException {

		IFile genModelFile = this.getGenModelFile();

		return this.getEcoreElementForJavaClass(genModelFile, root)
				.orElseThrow(() -> new GeneratedEMFCodeHelperException(
						"Unable to determine the metamodel element associated with CompilationUnit '"
								+ root.getElementName() + "'!"));

	}

	/**
	 * Determines the GenModel {@link IFile file} for the compilation unit specified. Opened Ecore editors will be
	 * preferred over all available genmodels.
	 *
	 * @return The {@link IFile} representing the GenModel or '<em>null</em>' if no GenModel could be determined.
	 */
	protected IFile determineAssociatedGenModelFile() {

		// The list of currently opened Ecore files (potential targets for our
		// selection)
		//
		List<IFile> ecoreFiles = UIHelper.getAllEditorInputs().stream().filter(
				e -> e instanceof FileEditorInput && "ecore".equals(((FileEditorInput) e).getFile().getFileExtension()))
				.map(e -> ((FileEditorInput) e).getFile()).collect(Collectors.toList());

		// The list of GenModel files corresponding to the opened Ecore files
		//
		List<IFile> genModelFiles = ecoreFiles.stream()
				.map(e -> e.getProject()
						.findMember(e.getProjectRelativePath().removeFileExtension().addFileExtension("genmodel")))
				.filter(g -> g instanceof IFile).map(g -> (IFile) g).collect(Collectors.toList());

		// Check if one of those GenModel files was responsible for generating
		// our Java class
		//
		for (IFile file : genModelFiles) {
			Optional<EObject> element = this.getEcoreElementForJavaClass(file, this.compilationUnit);
			if (element.isPresent()) {
				return file;
			}
		}
		// As none of the opened Ecore files seem to match our selection, we now
		// collect all GenModel files present in the workspace and check if one
		// of those represents the Ecore metamodel we want to open
		//
		genModelFiles = new ArrayList<>(this.collectGenModels(ResourcesPlugin.getWorkspace().getRoot()));
		for (IFile file : genModelFiles) {
			Optional<EObject> element = this.getEcoreElementForJavaClass(file, this.compilationUnit);
			if (element.isPresent()) {
				return file;
			}
		}

		return null;
	}

	/**
	 * Recursively collects all GenModel ({@link IFile IFiles} with the file ending '.genmodel') in the given
	 * {@link IContainer}.
	 *
	 * @param container
	 *            The {@link IContainer} to recursively process.
	 * @return The list of {@link IFile IFiles} representing a GenModel.
	 */
	protected Set<IFile> collectGenModels(IContainer container) {

		Set<IFile> ret = new HashSet<>();

		try {
			List<IResource> members = Arrays.asList(container.members());

			for (IResource member : members) {
				if (member instanceof IFile && ((IFile) member).getName().endsWith(".genmodel")) {
					ret.add((IFile) member);
				} else if (member instanceof IContainer) {
					ret.addAll(this.collectGenModels((IContainer) member));
				}
			}

		} catch (CoreException e) {
			UIHelper.log(e);
		}

		return ret;
	}

	/**
	 * Check if the {@link GenModel} defined by the given {@link IFile} was responsible for creating the given
	 * {@link CompilationUnit Java file}.
	 *
	 * @param genModelFile
	 *            The {@link IFile} defining the {@link GenModel} to check.
	 * @param javaFile
	 *            The {@link CompilationUnit Java file} to check.
	 * @return The {@link EObject metamodel element} associated with the given Java file or an empty optional if the
	 *         metamodel was not responsible for the generation of the Java file.
	 */
	public Optional<EObject> getEcoreElementForJavaClass(IFile genModelFile, CompilationUnit javaFile) {

		ResourceSet resourceSet = new ResourceSetImpl();

		// Load the GenModel
		//
		Resource res = null;
		try {
			res = resourceSet.getResource(URI.createPlatformResourceURI(genModelFile.getFullPath().toString(), true),
					true);
		} catch (RuntimeException e) {
			UIHelper.log(e);
			return Optional.empty();
		}

		if (!(res.getContents().get(0) instanceof GenModel)) {
			return Optional.empty();
		}

		// All GenPackages defined in the GenModel
		//
		Collection<GenPackage> genPackages = ((GenModel) res.getContents().get(0)).getGenPackages().stream()
				.flatMap(e -> AgteleEcoreUtil.getAllSubPackages(e, true).stream()).collect(Collectors.toSet());

		String mainTypeName = String.valueOf(javaFile.getMainTypeName());
		EMFGeneratedJavaFileType type = EMFGeneratedJavaFileType.getFileType(mainTypeName);

		// The name of the metamodel element we will open
		//
		String metamodelElementName = EMFGeneratedJavaFileType.getBaseName(mainTypeName);

		if (type.isClassType()) {

			// Collect all genClasses with the given metamodelElementName
			//
			Collection<GenClass> genClasses = AgteleEcoreUtil.getAllGenPackageGenClasses(genPackages,
					metamodelElementName);

			// Check if one of the found genClasses was responsible for
			// generating
			// the given javaFile
			//
			for (GenClass genClass : genClasses) {

				if (this.checkGenClassCreatedJavaFile(genClass, javaFile)) {
					return Optional.of(genClass.getEcoreClass());
				}

			}

		} else if (type.isPackageType()) {

			// Check if one of the found genPackages was responsible for
			// generating the given javaFile
			//
			for (GenPackage genPackage : genPackages) {

				if (this.checkGenPackageCreatedJavaFile(genPackage, javaFile)) {
					return Optional.of(genPackage.getEcorePackage());
				}

			}
		}

		return Optional.empty();
	}

	/**
	 * This checks if the given {@link GenPackage} was responsible for creating the given <em>javaFile</em>.
	 *
	 * @param genPackage
	 *            The {@link GenPackage} to check.
	 * @param javaFile
	 *            The {@link CompilationUnit Java file} to check.
	 * @return '<em>true</em>' if the given <em>javaFile</em> was created based on the given {@link GenPackage};
	 *         '<em>false</em>' otherwise.
	 */
	public boolean checkGenPackageCreatedJavaFile(GenPackage genPackage, CompilationUnit javaFile) {

		IPath actualJavaFilePath = javaFile.getResource().getFullPath();

		// The type of the given javaFile
		//
		EMFGeneratedJavaFileType type = EMFGeneratedJavaFileType
				.getFileType(String.valueOf(javaFile.getMainTypeName()));

		if (type == null) {
			return false;
		}

		IPath expectedJavaFilePath;

		// Construct the expected path (based on the GenModel settings) for the
		// GenPackage
		//
		if (type.isModelType()) {

			IPath modelDirectoryPath = new Path(genPackage.getGenModel().getModelDirectory());

			if (type.equals(EMFGeneratedJavaFileType.FACTORY)) {
				expectedJavaFilePath = modelDirectoryPath
						.append(this.getJavaPathFromQualifiedName(genPackage.getQualifiedFactoryInterfaceName()));

			} else if (type.equals(EMFGeneratedJavaFileType.PACKAGE)) {
				expectedJavaFilePath = modelDirectoryPath
						.append(this.getJavaPathFromQualifiedName(genPackage.getQualifiedPackageInterfaceName()));

			} else if (type.equals(EMFGeneratedJavaFileType.FACTORYIMPLY)) {
				expectedJavaFilePath = modelDirectoryPath
						.append(this.getJavaPathFromQualifiedName(genPackage.getQualifiedFactoryClassName()));

			} else if (type.equals(EMFGeneratedJavaFileType.PACKAGEIMPL)) {
				expectedJavaFilePath = modelDirectoryPath
						.append(this.getJavaPathFromQualifiedName(genPackage.getQualifiedPackageClassName()));

			} else if (type.equals(EMFGeneratedJavaFileType.ADAPTERFACTORY)) {
				expectedJavaFilePath = modelDirectoryPath
						.append(this.getJavaPathFromQualifiedName(genPackage.getQualifiedAdapterFactoryClassName()));

			} else if (type.equals(EMFGeneratedJavaFileType.SWITCH)) {
				expectedJavaFilePath = modelDirectoryPath
						.append(this.getJavaPathFromQualifiedName(genPackage.getQualifiedSwitchClassName()));

			} else if (type.equals(EMFGeneratedJavaFileType.VALIDATOR)) {
				expectedJavaFilePath = modelDirectoryPath
						.append(this.getJavaPathFromQualifiedName(genPackage.getQualifiedValidatorClassName()));

			} else {
				return false;
			}

		} else if (type.isEditType()) {

			IPath editDirectoryPath = new Path(genPackage.getGenModel().getEditDirectory());

			if (type.equals(EMFGeneratedJavaFileType.ITEMPROVIDERADAPTERFACTORY)) {
				expectedJavaFilePath = editDirectoryPath.append(this
						.getJavaPathFromQualifiedName(genPackage.getQualifiedItemProviderAdapterFactoryClassName()));

			} else {
				return false;
			}

		} else if (type.isEditorType()) {

			IPath editorDirectoryPath = new Path(genPackage.getGenModel().getEditorDirectory());

			if (type.equals(EMFGeneratedJavaFileType.EDITOR)) {
				expectedJavaFilePath = editorDirectoryPath
						.append(this.getJavaPathFromQualifiedName(genPackage.getQualifiedEditorClassName()));

			} else if (type.equals(EMFGeneratedJavaFileType.ACTIONBARCONTRIBUTOR)) {
				expectedJavaFilePath = editorDirectoryPath.append(
						this.getJavaPathFromQualifiedName(genPackage.getQualifiedActionBarContributorClassName()));

			} else {
				return false;
			}

		} else {
			return false;
		}

		// Check if both file paths point to the same actual file
		//
		URI actualJavaFileURI = ResourceHelper
				.convertPlatformToFileURI(URI.createPlatformResourceURI(actualJavaFilePath.toString(), true));
		URI expectedJavaFileURI = ResourceHelper
				.convertPlatformToFileURI(URI.createPlatformResourceURI(expectedJavaFilePath.toString(), true));
		return actualJavaFileURI != null && expectedJavaFileURI != null
				&& actualJavaFileURI.deresolve(expectedJavaFileURI).isEmpty();
	}

	/**
	 * This checks if the given {@link GenClass} was responsible for creating the given <em>javaFile</em>.
	 *
	 * @param genClass
	 *            The {@link GenClass} to check.
	 * @param javaFile
	 *            The {@link CompilationUnit Java file} to check.
	 * @return '<em>true</em>' if the given <em>javaFile</em> was created based on the given {@link GenClass};
	 *         '<em>false</em>' otherwise.
	 */
	public boolean checkGenClassCreatedJavaFile(GenClass genClass, CompilationUnit javaFile) {

		IPath actualJavaFilePath = javaFile.getResource().getFullPath();

		// The type of the given javaFile
		//
		EMFGeneratedJavaFileType type = EMFGeneratedJavaFileType
				.getFileType(String.valueOf(javaFile.getMainTypeName()));

		if (type == null) {
			return false;
		}

		IPath expectedJavaFilePath;

		// Construct the expected path (based on the GenModel settings) for the
		// GenClass
		//
		if (type.isModelType()) {

			IPath modelDirectoryPath = new Path(genClass.getGenModel().getModelDirectory());

			if (type.equals(EMFGeneratedJavaFileType.INTERFACE)) {
				expectedJavaFilePath = modelDirectoryPath
						.append(this.getJavaPathFromQualifiedName(genClass.getQualifiedInterfaceName()));

			} else if (type.equals(EMFGeneratedJavaFileType.IMPL)) {
				expectedJavaFilePath = modelDirectoryPath
						.append(this.getJavaPathFromQualifiedName(genClass.getQualifiedClassName()));

			} else {
				return false;
			}

		} else if (type.isEditType()) {

			IPath editDirectoryPath = new Path(genClass.getGenModel().getEditDirectory());

			if (type.equals(EMFGeneratedJavaFileType.ITEMPROVIDER)) {
				expectedJavaFilePath = editDirectoryPath
						.append(this.getJavaPathFromQualifiedName(genClass.getQualifiedProviderClassName()));

			} else {
				return false;
			}

		} else {

			return false;
		}

		// Check if both file paths point to the same actual file
		//
		URI actualJavaFileURI = ResourceHelper
				.convertPlatformToFileURI(URI.createPlatformResourceURI(actualJavaFilePath.toString(), true));
		URI expectedJavaFileURI = ResourceHelper
				.convertPlatformToFileURI(URI.createPlatformResourceURI(expectedJavaFilePath.toString(), true));
		return actualJavaFileURI != null && expectedJavaFileURI != null
				&& actualJavaFileURI.deresolve(expectedJavaFileURI).isEmpty();

	}

	/**
	 * Computes the path to the Java file for the given qualified name of a Java class.
	 * <p />
	 * Example: Calling this with 'my.fancy.JavaClass' will return 'my/fancy/JavaClass.java'.
	 *
	 * @param qualifiedName
	 *            The qualified name of a Java class.
	 * @return The path to the Java file.
	 */
	protected String getJavaPathFromQualifiedName(String qualifiedName) {

		return qualifiedName.replaceAll("\\.", "/") + ".java";
	}

	/**
	 * An {@link Exception} indicating an error during the execution of one of the functions in the
	 * {@link GeneratedEMFCodeHelper}.
	 *
	 * @author mfreund
	 */
	public class GeneratedEMFCodeHelperException extends Exception {

		/**
		 *
		 */
		private static final long serialVersionUID = -6907908255502340342L;

		/**
		 *
		 * @param message
		 * @param cause
		 */
		public GeneratedEMFCodeHelperException(String message, Throwable cause) {

			super(message, cause);
		}

		/**
		 *
		 * @param message
		 */
		public GeneratedEMFCodeHelperException(String message) {

			super(message);
		}

		/**
		 *
		 * @param cause
		 */
		public GeneratedEMFCodeHelperException(Throwable cause) {

			super(cause);
		}

	}
}