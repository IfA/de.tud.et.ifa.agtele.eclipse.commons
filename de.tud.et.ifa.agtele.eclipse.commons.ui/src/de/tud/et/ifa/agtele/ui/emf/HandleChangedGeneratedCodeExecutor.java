/**
 *
 */
package de.tud.et.ifa.agtele.ui.emf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.edit.domain.IEditingDomainProvider;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.SourceField;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.core.SourceRefElement;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

import de.tud.et.ifa.agtele.ui.editors.AgteleEcoreEditor;
import de.tud.et.ifa.agtele.ui.emf.PushCodeToEcoreExecutor.PushCodeToEcoreResult;
import de.tud.et.ifa.agtele.ui.listeners.SelectionListener2;
import de.tud.et.ifa.agtele.ui.util.UIHelper;

/**
 * Instances of this class are responsible for handling manual changes to Java code generated by EMF by either pushing
 * the changes to the corresponding metamodel (using a {@link PushCodeToEcoreExecutor} or by adding a 'NOT' tag to the
 * existing '@generated' tag.
 *
 * @author mfreund
 */
@SuppressWarnings("restriction")
public class HandleChangedGeneratedCodeExecutor {

	/**
	 * A regex that can be used to check if the JavaDoc of a method is equipped with the '@generated' tag. Also, it can
	 * be used to separate the JavaDoc (group 1) of the actual source (group 2) of a Java element.
	 */
	protected static final String JAVADOC_WITH_GENERATED_TAG_REGEX = "(/\\*(?:[^*]|(?:\\*+[^*/]))*" // JavaDoc beginning
			+ "\\*[\\s]*@generated(?![\\s]+NOT)" // '@generated' tag (starting a new line; not followed by 'NOT')
			+ "(?:[^*]|(?:\\*+[^*/]))*\\*+/)" // JavaDoc ending
			+ "([\\s\\S]+)"; // Actual content of the method

	/**
	 * The {@link Pattern} for the {@link #JAVADOC_WITH_GENERATED_TAG_REGEX}.
	 */
	protected final Pattern javaDocPattern = Pattern
			.compile(HandleChangedGeneratedCodeExecutor.JAVADOC_WITH_GENERATED_TAG_REGEX);

	/**
	 * The {@link CompilationUnitEditor} displaying the Java file to be handled by this executor.
	 */
	protected final CompilationUnitEditor javaEditor;

	/**
	 * The {@link GeneratedEMFCodeHelper} representing the Java file from the {@link #javaEditor}.
	 */
	protected final GeneratedEMFCodeHelper helper;

	/**
	 * The {@link ITextFileBuffer} that knows about all changes to the Java file displayed in the {@link #javaEditor}.
	 */
	protected ITextFileBuffer textFileBuffer;

	/**
	 * An {@link IEditorPart} displaying the {@link GeneratedEMFCodeHelper#ecoreFile Ecore metamodel} associated with
	 * the Java file displayed in the {@link #javaEditor}.
	 */
	protected IEditorPart ecoreEditor;

	/**
	 * This creates an instance.
	 *
	 * @param javaEditor
	 *            The {@link CompilationUnitEditor} displaying the Java file to be handled by this executor.
	 * @throws HandleChangedGeneratedCodeExecutorException
	 *             If the executor could not be initialized properly.
	 */
	public HandleChangedGeneratedCodeExecutor(CompilationUnitEditor javaEditor)
			throws HandleChangedGeneratedCodeExecutorException {

		this.javaEditor = javaEditor;

		// In order to prevent manual parsing of the Java document, we make use of the CompilationUnit type that
		// represents a structured Java document
		//
		ITypeRoot root = EditorUtility.getEditorInputJavaElement((IEditorPart) this.javaEditor, false);

		if (!(root instanceof ICompilationUnit)) {

			// No Java source file is edited
			//
			throw new HandleChangedGeneratedCodeExecutorException(
					"The input of the given CompilationUnitEditor does not seem to be a Java file!");
		}

		this.helper = new GeneratedEMFCodeHelper((CompilationUnit) root);

	}

	/**
	 * This will do the actual work: Check if any elements that are tagged with '@generated' have been
	 * {@link #getChangedElementsWithGeneratedTag() changed} and, if this is the case,
	 * {@link #handleChangedGeneratedElements(List, boolean) react} accordingly.
	 *
	 * @param askUser
	 *            Whether the user shall be asked what to do with changed methods.
	 * @return A {@link HandleChangedGeneratedCodeExecutionResult} indicating the result of the execution.
	 * @throws CoreException
	 * @throws BadLocationException
	 */
	public HandleChangedGeneratedCodeExecutionResult execute(boolean askUser)
			throws CoreException, BadLocationException {

		ITextFileBufferManager fileBufferManager = FileBuffers.getTextFileBufferManager();

		// Normally, the manager should already be connected to the location (because the file is opened in the
		// editor). But just to be sure, we connect again (will not do anything if already connected).
		//
		fileBufferManager.connect(this.helper.getCompilationUnit().getPath(), LocationKind.NORMALIZE, null);

		// The TextFileBuffer that knows about all changes to the document
		//
		this.textFileBuffer = fileBufferManager.getTextFileBuffer(this.helper.getCompilationUnit().getPath(),
				LocationKind.NORMALIZE);

		if (!this.textFileBuffer.isDirty()) {
			// Nothing to do as there are no changes that we need to check
			//
			return new HandleChangedGeneratedCodeExecutionResult((IEditorPart) this.javaEditor, this.ecoreEditor,
					new HashMap<>());
		}

		// All elements that are tagged with '@generated' and have been changed by the user since the last save
		//
		List<SourceRefElement> changedElementsWithGeneratedTag = this.getChangedElementsWithGeneratedTag();

		// Add 'NOT' to the '@generated' tag if necessary
		//
		return this.handleChangedGeneratedElements(changedElementsWithGeneratedTag, askUser);

	}

	/**
	 * Determine all {@link SourceRefElement Java elements} inside the Java file represented by the {@link #helper} that
	 * 1. are tagged with '@generated' and 2. have been changed since the last save.
	 * <p />
	 * Note: This uses
	 * {@link EditorUtility#calculateChangedLineRegions(ITextFileBuffer, org.eclipse.core.runtime.IProgressMonitor)} to
	 * determine the changed regions. This however does not report deleted lines as changes but only added or changed
	 * lines!
	 *
	 * @return A list of changed {@link SourceRefElement elements} tagged with '@generated'.
	 * @throws CoreException
	 *             If an exception occurs while accessing the resource corresponding to the given buffer.
	 */
	protected List<SourceRefElement> getChangedElementsWithGeneratedTag() throws CoreException {

		// This represents the main class inside the Java document
		//
		Optional<SourceType> sourceType = Arrays.asList(this.helper.getCompilationUnit().getTypeRoot().getChildren())
				.parallelStream().filter(j -> j instanceof SourceType).map(j -> (SourceType) j).findFirst();

		if (!sourceType.isPresent()) {
			return new ArrayList<>();
		}

		// A list of all Java elements that we will check for an '@generated' tag
		//
		List<SourceRefElement> elementsToCheck = new ArrayList<>();
		//elementsToCheck.add(sourceType.get());
		elementsToCheck.addAll(
				Arrays.asList(sourceType.get().getChildren()).stream().filter(e -> e instanceof SourceRefElement)
						.map(e -> (SourceRefElement) e).collect(Collectors.toList()));

		// The list of Java elements that are tagged with '@generated' (but not with '@generate NOT')
		//
		List<SourceRefElement> elementsWithGeneratedTag = elementsToCheck.stream().filter(this::isTaggedWithGenerated)
				.collect(Collectors.toList());

		if (elementsWithGeneratedTag.isEmpty()) {
			// Nothing to be done as there are no methods tagged with '@generated'
			//
			return new ArrayList<>();
		}

		// The list of regions that have been changed since the last save.
		//
		List<IRegion> changedRegions = Arrays
				.asList(EditorUtility.calculateChangedLineRegions(this.textFileBuffer, new NullProgressMonitor()));

		if (changedRegions.isEmpty()) {
			// Nothing to be done as there are changed regions. This should usually not happen as we checked that
			// the editor is dirty above.
			//
			return new ArrayList<>();
		}

		// Check if any changes have been made to methods that are tagged with '@generated'
		//
		return elementsWithGeneratedTag.stream().filter(m -> this.hasElementBeenChanged(m, changedRegions))
				.collect(Collectors.toList());
	}

	/**
	 * Whether the given {@link SourceRefElement} is tagged with '@generated' (but not with '@generated NOT').
	 *
	 * @param javaElement
	 *            The {@link SourceRefElement} to check.
	 * @return '<em>true</em>' if the given {@link SourceRefElement} is tagged with '@generated'.
	 */
	protected boolean isTaggedWithGenerated(SourceRefElement javaElement) {

		try {
			// Theoretically, it should be better to only query '#getJavadocRange'. However, this seems to sometimes
			// return 'null' although there is a JavaDoc for the elemet.
			//
			String source = javaElement.getSource();

			return source != null && this.javaDocPattern.matcher(source).matches();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Whether the given {@link SourceRefElement} has been changed since the last save.
	 *
	 * @param javaElement
	 *            The {@link SourceRefElement} to check.
	 * @param changedRegions
	 *            The list of changed {@link IRegion regions}.
	 *
	 * @return '<em>true</em>' if the given {@link SourceRefElement} has been changed.
	 */
	protected boolean hasElementBeenChanged(SourceRefElement javaElement, List<IRegion> changedRegions) {

		try {

			ISourceRange sourceRange = javaElement.getSourceRange();

			// We are not interested in changes to the JavaDoc of an element but only in changes to the actual source.
			// In order to determine this, we use the 'javaDocPattern' from above.
			//
			Matcher matcher = this.javaDocPattern.matcher(javaElement.getSource());

			if (!matcher.matches()) {
				// This should not happen because we have checked this before as part of 'isTaggedWithGenerated'
				//
				throw new RuntimeException("Internal Error while determining change methods in the Java source file!");
			}

			String javaDoc = matcher.group(1);
			String source = matcher.group(2);

			// For classes (SourceType), we do not consider all changes to the source but only changes affecting the
			// class definition (i.e. changed extends and imports).
			//
			int sourceLength = javaElement instanceof SourceType ? source.indexOf('{')
					: sourceRange.getLength() - javaDoc.length();

			SourceRange sourceRangeWithoutJavadoc = new SourceRange(sourceRange.getOffset() + javaDoc.length(),
					sourceLength);

			// Check if any of the changed regions overlap with the determined range of the source of the Java element
			//
			return changedRegions.parallelStream()
					.anyMatch(cr -> sourceRangeWithoutJavadoc.getOffset() <= cr.getOffset()
							&& cr.getOffset() <= sourceRangeWithoutJavadoc.getOffset()
									+ sourceRangeWithoutJavadoc.getLength()
							|| cr.getOffset() <= sourceRangeWithoutJavadoc.getOffset()
									&& sourceRangeWithoutJavadoc.getOffset() <= cr.getOffset() + cr.getLength());

		} catch (JavaModelException e) {
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Handle all of the given {@link SourceRefElement Java elements} by either {@link #pushToEcore(SourceMethod)
	 * pushing them to Ecore}, {@link #addNotTag(SourceMethod, boolean, Optional) adding a NOT to the '@generated' tag}
	 * or ignoring them.
	 *
	 * @param javaElements
	 *            A list of changed {@link SourceRefElement Java elements} tagged with '@generated'.
	 * @param askUser
	 *            Whether the user shall be asked what to do with changed methods. If this is set to '<em>false</em>'
	 *            all of the given <em>methods</em> are handled automatically.
	 * @return A {@link HandleChangedGeneratedCodeExecutionResult} representing the result of the handling process.
	 * @throws JavaModelException
	 *             If an exception occurs while accessing the resource corresponding to the given buffer.
	 * @throws BadLocationException
	 */
	protected HandleChangedGeneratedCodeExecutionResult handleChangedGeneratedElements(
			List<SourceRefElement> javaElements, boolean askUser) throws JavaModelException, BadLocationException {

		if (javaElements.isEmpty()) {
			// Nothing to be done
			//
			return new HandleChangedGeneratedCodeExecutionResult((IEditorPart) this.javaEditor, this.ecoreEditor,
					new HashMap<>());
		}

		Map<SourceRefElement, Optional<PushCodeToEcoreResult>> resultMap = new HashMap<>();

		for (int i = 0; i < javaElements.size(); i++) {

			SourceRefElement javaElement = javaElements.get(i);

			// Assume we are in GeneratedCodeChangedListenerMode#AUTOMATIC mode
			//
			boolean addNotTag = true;
			boolean pushToEcore = false;
			boolean addTodo = true;
			Optional<String> explanation = Optional.empty();
			boolean doNotAskAnyMore = true;

			if (askUser) {

				// If we are instead in USER mode, present a dialog to the user
				//
				HandleChangedGeneratedCodeDialog dialog = new HandleChangedGeneratedCodeDialog(javaElement, this.helper,
						javaElements.size() - i - 1);
				dialog.create();

				int result = dialog.open();

				explanation = dialog.getAddNotTagExplanation();
				doNotAskAnyMore = dialog.isDoNotAskAnyMore();
				addTodo = dialog.isAddTodo();
				pushToEcore = result == IDialogConstants.OPEN_ID;
				addNotTag = result == Window.OK;
			}

			// The user chose 'No' -> do nothing
			//
			if (!addNotTag && !pushToEcore) {
				if (doNotAskAnyMore) {
					break;
				} else {
					continue;
				}
			}

			// The user chose 'Yes' -> push to ecore/add NOT tag
			//
			if (doNotAskAnyMore) {
				for (SourceRefElement m : javaElements.subList(i, javaElements.size())) {
					Optional<PushCodeToEcoreResult> result = this.handleChangedGeneratedElement(m, pushToEcore, addTodo,
							explanation);
					resultMap.put(m, result);
				}
				break;

			} else {
				Optional<PushCodeToEcoreResult> result = this.handleChangedGeneratedElement(javaElement, pushToEcore,
						addTodo, explanation);
				resultMap.put(javaElement, result);
			}

		}

		return new HandleChangedGeneratedCodeExecutionResult((IEditorPart) this.javaEditor, this.ecoreEditor,
				resultMap);
	}

	/**
	 * Handles a changed {@link SourceRefElement Java element} by trying to {@link #pushToEcore(SourceRefElement) push
	 * the changes to the Ecore metamodel} if <em>pushToEcore</em> is set to '<em>true</em>' and, if this is not
	 * successful or if <em>pushToEcore</em> is set to '<em>false</em>', {@link #addNotTag(SourceRefElement, Optional)
	 * adding a 'NOT' tag} instead.
	 *
	 * @param javaElement
	 *            The {@link SourceRefElement} to handle.
	 * @param pushToEcore
	 *            Whether the executor shall try to {@link #pushToEcore(SourceRefElement) push the changes to the Ecore
	 *            metamodel}.
	 * @param addTodo
	 *            Whether a 'TO DO' tag shall be added to the JavaDoc reminding the user to integrate these changes into
	 *            the Ecore metamodel.
	 * @param explanation
	 *            An optional additional String explaining while the generated element body was changed.
	 * @return A {@link PushCodeToEcoreResult} if the {@link #pushToEcore(SourceRefElement) push to Ecore} was
	 *         successful or an empty Optional if {@link #addNotTag(SourceRefElement, Optional) a NOT tag was added}
	 *         instead.
	 * @throws JavaModelException
	 * @throws BadLocationException
	 */
	protected Optional<PushCodeToEcoreResult> handleChangedGeneratedElement(SourceRefElement javaElement,
			boolean pushToEcore, boolean addTodo, Optional<String> explanation)
			throws JavaModelException, BadLocationException {

		if (pushToEcore) {

			try {
				return Optional.of(this.pushToEcore(javaElement));

			} catch (HandleChangedGeneratedCodeExecutorException e) {
				// Nothing to do, just use 'addNotTag' instead
				//
			}
		}

		this.addNotTag(javaElement, addTodo, explanation);
		return Optional.empty();

	}

	/**
	 * Pushes the code of the given {@link SourceRefElement} to the Ecore metamodel.
	 *
	 * @param javaElement
	 *            The {@link SourceRefElement} to be pushed.
	 * @return The {@link PushCodeToEcoreResult} of the push.
	 * @throws HandleChangedGeneratedCodeExecutorException
	 *             If something went wrong, e.g. if the required associated metamodel element could not be determined or
	 *             if the given <em>javaElement</em> is not pushable.
	 */
	protected PushCodeToEcoreResult pushToEcore(SourceRefElement javaElement)
			throws HandleChangedGeneratedCodeExecutorException {

		try {

			if (this.ecoreEditor == null) {
				this.ecoreEditor = this.openEcoreEditor(this.helper.getEcoreFile());
			}

			PushCodeToEcoreExecutor executor = new PushCodeToEcoreExecutor(this.helper,
					((IEditingDomainProvider) this.ecoreEditor).getEditingDomain().getResourceSet());

			return executor.pushToEcore(javaElement);

		} catch (Exception e) {
			throw new HandleChangedGeneratedCodeExecutorException(e);
		} finally {
			UIHelper.activateEditor((IEditorPart) this.javaEditor);
		}

	}

	/**
	 * Add a 'NOT' to the '@generated' tag of the given {@link SourceRefElement}.
	 *
	 * @param javaElement
	 *            The {@link SourceRefElement} to which the 'NOT' shall be added.
	 * @param addTodo
	 *            Whether a 'TO DO' tag shall be added to the JavaDoc reminding the user to integrate these changes into
	 *            the Ecore metamodel.
	 * @param explanation
	 *            An optional additional String that will be appended to the 'NOT' explaining while the generated
	 *            element body was changed.
	 */
	protected void addNotTag(SourceRefElement javaElement, boolean addTodo, Optional<String> explanation)
			throws JavaModelException, BadLocationException {

		String oldSource = javaElement.getSource();
		javaElement.getJavaModel().getChildren();

		String todoString = addTodo
				? "TODO Don't forget to incorporate your manual changes into the Ecore metamodel!\n\t * "
				: "";

		String newSource = oldSource.replaceFirst("@generated",
				todoString + "@generated NOT" + (explanation.isPresent() ? " " + explanation.get() : ""));

		this.textFileBuffer.getDocument().replace(javaElement.getSourceRange().getOffset(),
				javaElement.getSourceRange().getLength(), newSource);

	}

	/**
	 * Tries to open the default editor for the specified ecore file. If the opening fails, the method displays an error
	 * using {@link #showError(String)}. The editor is accepted only, if it is an {@link IEditingDomainProvider}. If the
	 * default editor is not capable to use, the {@link AgteleEcoreEditor} will be opened.
	 *
	 * @param ecoreFile
	 * @return the opened ecore editor
	 */
	protected IEditorPart openEcoreEditor(IFile ecoreFile) {

		List<IEditorPart> openEditors = UIHelper.getAllEditors();

		// This will be returned in the end.
		//
		IEditorPart editor;

		try {
			editor = UIHelper.openEditor(ecoreFile);

			if (!(editor instanceof IEditingDomainProvider)) {
				if (!openEditors.contains(editor)) {
					editor.getEditorSite().getPage().closeEditor(editor, false);
				}

				editor = UIHelper.openEditor(ecoreFile, "de.tud.et.ifa.agtele.ui.editors.EcoreEditorID");
			}
		} catch (PartInitException e1) {
			throw new RuntimeException("Unable to open editor for associated Ecore metamodel!", e1);
		}

		if (!(editor instanceof AgteleEcoreEditor)) {
			throw new RuntimeException("Unable to open compatible ecore editor!");
		}

		return editor;
	}

	/**
	 * A {@link Dialog} that allows the user to choose whether manually changed generated elements shall be pushed to
	 * Ecore or if their '@generated' tag shall be changed to '@generated NOT'.
	 *
	 * @author mfreund
	 */
	public class HandleChangedGeneratedCodeDialog extends TitleAreaDialog {

		/**
		 * The {@link SourceRefElement} that is the subject of this dialog.
		 */
		protected final SourceRefElement javaElement;

		/**
		 * The {@link GeneratedEMFCodeHelper} representing the Java file from the {@link #javaEditor}.
		 */
		protected final GeneratedEMFCodeHelper helper;

		/**
		 * A {@link Text} that allows the user to enter an explanation text why the generated method was changed
		 * manually.
		 */
		protected Text addNotTagExplanationText;

		/**
		 * The explanation entered by the user in the {@link #addNotTagExplanationText}.
		 */
		protected String addNotTagExplanation;

		/**
		 * A {@link Button} that allows the user to trigger the additional generation of a 'to do' in the JavaDoc.
		 */
		protected Button addTodoButton;

		/**
		 * A boolean indicating whether the user checked the {@link #addTodoButton}.
		 */
		protected boolean addTodo;

		/**
		 * A {@link Button} that allows the user to prevent further inquiries during the current save action.
		 */
		protected Button doNotAskAnyMoreButton;

		/**
		 * A boolean indicating whether the user checked the {@link #doNotAskAnyMoreButton}.
		 */
		protected boolean doNotAskAnyMore;

		/**
		 * An Integer indicating the number of further inquiries that will happen during the current save action (the
		 * rest of the changed methods).
		 */
		protected int pendingRequests;

		/**
		 * This creates an instance.
		 *
		 * @param javaElement
		 *            The {@link SourceRefElement} that is the subject of this dialog.
		 * @param helper
		 *            The {@link GeneratedEMFCodeHelper} representing the Java file that the given {@link SourceMethod}
		 *            is a part of.
		 * @param pendingRequests
		 *            An Integer indicating the number of further inquiries that will happen during the current save
		 *            action (the rest of the changed methods).
		 */
		public HandleChangedGeneratedCodeDialog(SourceRefElement javaElement, GeneratedEMFCodeHelper helper,
				int pendingRequests) {

			super(UIHelper.getShell());

			this.javaElement = javaElement;
			this.helper = helper;

			this.pendingRequests = pendingRequests;

			this.addNotTagExplanation = "";
			this.doNotAskAnyMore = false;
			this.addTodo = false;
		}

		@Override
		public void create() {

			super.create();

			this.setTitle(this.compileTitle());
			this.setMessage("Push to Ecore or change '@generated' tag to '@generated NOT'?", IMessageProvider.WARNING);
		}

		/**
		 * Compile a meaningful title for this dialog based on the concrete type of the {@link #javaElement}.
		 *
		 * @return The title for the dialog.
		 */
		protected String compileTitle() {

			String type = "element";
			StringBuilder name = new StringBuilder(this.javaElement.getElementName());

			if (this.javaElement instanceof SourceMethod) {
				type = "method";
				name.append("(").append(((IMethod) this.javaElement).getNumberOfParameters() > 0 ? "..." : "")
						.append(")");
			} else if (this.javaElement instanceof SourceField) {
				type = "field";
			} else if (this.javaElement instanceof SourceType) {
				type = "class";
			}

			StringBuilder title = new StringBuilder("Generated ").append(type).append(" '").append(name)
					.append("' was changed!");

			return title.toString();
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {

			Button pushToEcoreButton = this.createButton(parent, IDialogConstants.OPEN_ID, "Push to Ecore", false);
			pushToEcoreButton.addSelectionListener((SelectionListener2) e -> {
				HandleChangedGeneratedCodeDialog.this.saveInput();
				HandleChangedGeneratedCodeDialog.this.setReturnCode(IDialogConstants.OPEN_ID);
				HandleChangedGeneratedCodeDialog.this.close();

			});
			if (!new PushCodeToEcoreExecutor(this.helper, null).isPushable(this.javaElement)) {
				pushToEcoreButton.setEnabled(false);
			}
			this.createButton(parent, IDialogConstants.OK_ID, "Add NOT tag", true);
			this.createButton(parent, IDialogConstants.CANCEL_ID, "Ignore", false);
		}

		@Override
		protected Control createDialogArea(Composite parent) {

			Composite area = (Composite) super.createDialogArea(parent);
			Composite container = new Composite(area, SWT.NONE);
			container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			GridLayout layout = new GridLayout(2, false);
			layout.verticalSpacing = 8;
			layout.marginRight = 5;
			layout.marginLeft = 5;
			container.setLayout(layout);

			Label lblAdditionalTextoptional = new Label(container, SWT.NONE);
			lblAdditionalTextoptional.setToolTipText(
					"An explanation why this element was changed manually (will only be used if 'NOT' tag is used)");
			lblAdditionalTextoptional.setText("Additional text (optional):");

			this.addNotTagExplanationText = new Text(container, SWT.BORDER);
			this.addNotTagExplanationText.setToolTipText(
					"An explanation why this element was changed manually (will only be used if 'NOT' tag is used)");
			this.addNotTagExplanationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

			this.addTodoButton = new Button(container, SWT.CHECK);
			this.addTodoButton.setSelection(true);
			this.addTodoButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			this.addTodoButton.setText("Create TODO");
			this.addTodoButton.setToolTipText(
					"Checking this will create a 'TODO' in the JavaDoc reminding the user to integrate the manual changes into the Ecore model");

			if (this.pendingRequests > 0) {
				this.doNotAskAnyMoreButton = new Button(container, SWT.CHECK);
				this.doNotAskAnyMoreButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
				this.doNotAskAnyMoreButton
						.setText("Remember my decision (" + this.pendingRequests + " additional changes)");
			}

			Label lblInfo = new Label(container, SWT.WRAP);
			lblInfo.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, true, 2, 1));
			lblInfo.setText(
					"Note: This dialog can be disabled via \"Agtele Settings -> React to changed '@generated' elements\"!");

			return area;
		}

		@Override
		protected boolean isResizable() {

			return true;
		}

		// save content of the UI elements because they get disposed
		// as soon as the Dialog closes
		protected void saveInput() {

			this.addNotTagExplanation = this.addNotTagExplanationText.getText();
			this.doNotAskAnyMore = this.doNotAskAnyMoreButton != null && this.doNotAskAnyMoreButton.getSelection();
			this.addTodo = this.addTodoButton.getSelection();
		}

		@Override
		protected void cancelPressed() {

			this.saveInput();
			super.cancelPressed();
		}

		@Override
		protected void okPressed() {

			this.saveInput();
			super.okPressed();
		}

		/**
		 * The text that the user entered as additional explanation or an empty optional if nothing was entered.
		 *
		 * @return the {@link #addNotTagExplanation}
		 */
		public Optional<String> getAddNotTagExplanation() {

			return this.addNotTagExplanation.isEmpty() ? Optional.empty() : Optional.of(this.addNotTagExplanation);
		}

		/**
		 * Whether the user selected the {@link #doNotAskAnyMore} checkbox.
		 * <p />
		 * Note: This will return <em>false</em> if {@link #pendingRequests} was set to null in the
		 * {@link #HandleChangedGeneratedCodeDialog(SourceMethod, GeneratedEMFCodeHelper, int) constructor}.
		 *
		 * @return the {@link #doNotAskAnyMore}
		 */
		public boolean isDoNotAskAnyMore() {

			return this.doNotAskAnyMore;
		}

		/**
		 * Whether the user selected the {@link #addTodoButton}.
		 *
		 * @return the {@link #addTodo}
		 */
		public boolean isAddTodo() {

			return this.addTodo;
		}

	}

	/**
	 * An {@link Exception} indicating an error during the execution of a
	 * {@link HandleChangedGeneratedCodeExecutorException}.
	 *
	 * @author mfreund
	 */
	public class HandleChangedGeneratedCodeExecutorException extends Exception {

		/**
		 *
		 */
		private static final long serialVersionUID = 744525392807163282L;

		/**
		 *
		 * @param message
		 * @param cause
		 */
		public HandleChangedGeneratedCodeExecutorException(String message, Throwable cause) {

			super(message, cause);
		}

		/**
		 *
		 * @param message
		 */
		public HandleChangedGeneratedCodeExecutorException(String message) {

			super(message);
		}

		/**
		 *
		 * @param cause
		 */
		public HandleChangedGeneratedCodeExecutorException(Throwable cause) {

			super(cause);
		}

	}

	/**
	 * A simple POJO indicating the result of an {@link HandleChangedGeneratedCodeExecutor#execute(boolean) execution}
	 * of a {@link HandleChangedGeneratedCodeExecutor}.
	 *
	 * @author mfreund
	 */
	public class HandleChangedGeneratedCodeExecutionResult {

		/**
		 * This create an instance.
		 *
		 * @param editor
		 * @param ecoreEditor
		 * @param pushedElements
		 */
		public HandleChangedGeneratedCodeExecutionResult(IEditorPart editor, IEditorPart ecoreEditor,
				Map<SourceRefElement, Optional<PushCodeToEcoreResult>> pushedElements) {

			super();
			this.editor = editor;
			this.ecoreEditor = ecoreEditor;
			this.pushedElements = pushedElements;
		}

		/**
		 * The {@link IEditorPart} on which the save was performed.
		 */
		protected IEditorPart editor;

		/**
		 * If at least one method was {@link HandleChangedGeneratedCodeExecutor pushed to ecore}, this contains the
		 * {@link IEditorPart editor} used for the push.
		 */
		protected IEditorPart ecoreEditor;

		/**
		 * A Map that, for each of the handled {@link SourceRefElement Java elements}, provides either a
		 * {@link PushCodeToEcoreResult} if the method was {@link #pushToEcore(SourceRefElement) pushed to Ecore} or an
		 * empty Optional if {@link #addNotTag(SourceRefElement, Optional) a NOT tag was added} instead.
		 */
		protected Map<SourceRefElement, Optional<PushCodeToEcoreResult>> pushedElements;

		/**
		 * @return the {@link #editor}
		 */
		public IEditorPart getEditor() {

			return this.editor;
		}

		/**
		 * @return the {@link #editor} if it is a {@link CompilationUnitEditor} or an empty Optional otherwise
		 */
		public Optional<CompilationUnitEditor> getJavaEditor() {

			return this.editor instanceof CompilationUnitEditor ? Optional.of((CompilationUnitEditor) this.editor)
					: Optional.empty();
		}

		/**
		 * @return the {@link #ecoreEditor}
		 */
		public Optional<IEditorPart> getEcoreEditor() {

			return Optional.ofNullable(this.ecoreEditor);
		}

		/**
		 * @return the {@link #pushedElements}
		 */
		public Map<SourceRefElement, Optional<PushCodeToEcoreResult>> getPushedElements() {

			return this.pushedElements;
		}
	}
}
