/**
 *
 */
package de.tud.et.ifa.agtele.ui.listeners;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import de.tud.et.ifa.agtele.ui.AgteleUIPlugin;
import de.tud.et.ifa.agtele.ui.emf.HandleChangedGeneratedCodeExecutor;
import de.tud.et.ifa.agtele.ui.emf.PushCodeToEcoreExecutor.PushCodeToEcoreResult;
import de.tud.et.ifa.agtele.ui.util.UIHelper;

/**
 * A {@link WorkspaceCommandListener} that listens to 'save' events in {@link CompilationUnitEditor Java editors}. If
 * generated Java code (tagged with '@generated') was changed by the user, this reminds the user to change the
 * annotation to '@generated NOT'.
 * <p />
 * Note: The listener has to be initialized via {@link #init()}.
 * <p />
 * Note: Currently, only changes to generated methods are addressed. However, it could maybe be useful to also handled
 * changes to fields or class declarations in the future.
 * <p />
 * Note: The listener can work in various {@link GeneratedCodeChangedListenerMode modes} that can be enabled via
 * {@link #setMode(GeneratedCodeChangedListenerMode)}. Specified modes are persisted in the {@link IPreferenceStore} of
 * the {@link AgteleUIPlugin} by use of the {@link #OPERATING_MODE_SETTING_KEY}.
 *
 *
 * @author mfreund
 */
@SuppressWarnings("restriction")
public class GeneratedCodeChangedListener extends WorkspaceCommandListener {

	/**
	 * The singleton instance of this listener.
	 */
	protected static GeneratedCodeChangedListener instance;

	/**
	 * The {@link GeneratedCodeChangedListenerMode} that the listener currently operates on.
	 */
	private static GeneratedCodeChangedListenerMode mode;

	/**
	 * The {@link IPreferenceStore} of the {@link AgteleUIPlugin}.
	 */
	protected static IPreferenceStore store = null;

	/**
	 * The settings key for the {@link IPreferenceStore}.
	 */
	protected static final String OPERATING_MODE_SETTING_KEY = "GENERATED_CODE_CHANGED_LISTENER_OPERATING_MODE";

	/**
	 * Initializes the singleton {@link #instance} of this listener and registers it to the {@link ICommandService}.
	 *
	 */
	public static void init() {

		if (GeneratedCodeChangedListener.instance != null) {

			// The listener was already initialized
			//
			return;
		}

		GeneratedCodeChangedListener.instance = new GeneratedCodeChangedListener();

		IWorkbench wb = PlatformUI.getWorkbench();

		final ICommandService commandService = wb.getService(ICommandService.class);

		commandService.addExecutionListener(new GeneratedCodeChangedListener());
	}

	/**
	 * This creates an instance.
	 *
	 */
	protected GeneratedCodeChangedListener() {

		// Only listen to 'save' events in the Java editor
		//
		super(Arrays.asList(IWorkbenchCommandConstants.FILE_SAVE, IWorkbenchCommandConstants.FILE_SAVE_AS,
				IWorkbenchCommandConstants.FILE_SAVE_ALL), Arrays.asList(CompilationUnitEditor.class));
	}

	/**
	 * Listen to the {@link IExecutionListener#preExecute(String, ExecutionEvent) preExecute} event so that we can make
	 * changes to the file before it is actually being saved.
	 */
	@Override
	protected void doPreExecute(String commandId, ExecutionEvent event) {

		if (GeneratedCodeChangedListener.getMode() == GeneratedCodeChangedListenerMode.OFF) {
			return;
		}

		IEditorPart currentEditor = UIHelper.getCurrentEditor();
		ISelection currentSelection = UIHelper.getCurrentSelection();

		if (commandId.equals(IWorkbenchCommandConstants.FILE_SAVE)) {

			// Only handle the current editor
			//
			this.handleEditorBeforeSave(currentEditor);

		} else if (commandId.equals(IWorkbenchCommandConstants.FILE_SAVE_ALL)) {

			// Handle all open editors
			//
			for (IEditorPart editor : UIHelper.getAllEditors()) {

				if (GeneratedCodeChangedListener.getMode() == GeneratedCodeChangedListenerMode.USER) {

					// Bring the editor to the top to allow the user identify the changes
					//
					UIHelper.activateEditor(editor);
				}

				this.handleEditorBeforeSave(editor);
			}

		}

		// Reset the original UI state (bring the 'currentEditor' back to the top and reset the 'currentSelection')
		//
		UIHelper.activateEditor(currentEditor);
		if (currentEditor instanceof ISelectionProvider) {
			((ISelectionProvider) currentEditor).setSelection(currentSelection);
		}
	}

	/**
	 * This creates an instance of {@link HandleChangedGeneratedCodeExecutor} and let it do the actual work.
	 *
	 * @return A Map that, for each of the handled {@link SourceMethod methods}, provides either a
	 *         {@link PushCodeToEcoreResult} if the method was {@link #pushToEcore(SourceMethod) pushed to Ecore} or an
	 *         empty Optional if {@link #addNotTag(SourceMethod, Optional) a NOT tag was added} instead.
	 *
	 */
	protected Map<SourceMethod, Optional<PushCodeToEcoreResult>> handleEditorBeforeSave(IEditorPart editor) {

		if (!(editor instanceof CompilationUnitEditor)) {
			return new HashMap<>();
		}

		try {
			return new HandleChangedGeneratedCodeExecutor((CompilationUnitEditor) editor)
					.execute(GeneratedCodeChangedListener.getMode() == GeneratedCodeChangedListenerMode.USER);
		} catch (Exception e) {
			AgteleUIPlugin.getPlugin().getLog().log(new Status(Status.ERROR, "de.tud.et.ifa.agtele.eclipse.commons.ui",
					e.getMessage() != null ? e.getMessage() : e.toString(), e));
		}

		return new HashMap<>();
	}

	@Override
	protected void doNotHandled(String commandId, NotHandledException exception) {

		// nothing to be done
		//
	}

	@Override
	protected void doPostExecuteFailure(String commandId, ExecutionException exception) {

		// nothing to be done
		//
	}

	@Override
	protected void doPostExecuteSuccess(String commandId, Object returnValue) {

		// nothing to be done
		//
	}

	/**
	 * @return the {@link #mode}
	 */
	public static GeneratedCodeChangedListenerMode getMode() {

		if (GeneratedCodeChangedListener.mode == null) {
			// Read the mode from the preferences
			//
			if (GeneratedCodeChangedListener.store == null) {
				GeneratedCodeChangedListener.store = AgteleUIPlugin.getPlugin().getPreferenceStore();
			}
			int storedModeValue = GeneratedCodeChangedListener.store
					.getInt(GeneratedCodeChangedListener.OPERATING_MODE_SETTING_KEY);
			GeneratedCodeChangedListenerMode storedMode = GeneratedCodeChangedListenerMode.getMode(storedModeValue);

			GeneratedCodeChangedListener.mode = storedMode != null ? storedMode : GeneratedCodeChangedListenerMode.USER;
		}

		return GeneratedCodeChangedListener.mode;
	}

	/**
	 * This sets the new {@link GeneratedCodeChangedListenerMode}.
	 *
	 * @param mode
	 *            the {@link GeneratedCodeChangedListenerMode} to set
	 */
	public static void setMode(GeneratedCodeChangedListenerMode mode) {

		if (GeneratedCodeChangedListener.store == null) {
			GeneratedCodeChangedListener.store = AgteleUIPlugin.getPlugin().getPreferenceStore();
		}

		GeneratedCodeChangedListener.mode = mode;
		GeneratedCodeChangedListener.store.setValue(GeneratedCodeChangedListener.OPERATING_MODE_SETTING_KEY,
				mode.getValue());
	}

	/**
	 * This sets the new {@link GeneratedCodeChangedListenerMode}.
	 *
	 * @param modeValue
	 *            the value of the {@link GeneratedCodeChangedListenerMode} to set
	 */
	public static void setMode(int modeValue) {

		GeneratedCodeChangedListener.setMode(GeneratedCodeChangedListenerMode.getMode(modeValue));

	}

	/**
	 * An enum describing the different operating modes of the {@link GeneratedCodeChangedListener}.
	 *
	 * @author mfreund
	 */
	public enum GeneratedCodeChangedListenerMode {

		/**
		 * The listener works in user mode (the user is asked whether NOT tags should be added via a dialog).
		 */
		USER(0),

		/**
		 * The listener works in automatic mode (NOT tags are added automatically).
		 */
		AUTOMATIC(1),

		/**
		 * The listener is turned off.
		 */
		OFF(2);

		private final int value;

		private GeneratedCodeChangedListenerMode(final int value) {

			this.value = value;
		}

		/**
		 * Returns the integer value of the enum constant.
		 *
		 * @return The integer value of the enum constant.
		 */
		public int getValue() {

			return this.value;
		}

		/**
		 * Returns the mode for the specified value.
		 *
		 * @param value
		 *            the int value of the mode to return
		 * @return The {@link GeneratedCodeChangedListenerMode} or '<em>null</em>' if no mode for the specified value
		 *         exists.
		 */
		public static GeneratedCodeChangedListenerMode getMode(int value) {

			if (value == 0) {
				return USER;
			} else if (value == 1) {
				return GeneratedCodeChangedListenerMode.AUTOMATIC;
			} else if (value == 2) {
				return GeneratedCodeChangedListenerMode.OFF;
			} else {
				throw new IllegalArgumentException(
						"The value " + value + " does not represent a valid GeneratedCodeChangedListenerMode!");
			}
		}
	}

}
