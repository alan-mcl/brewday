package mclachlan.brewday.ui.swing.app;

/**
 * Opens the Swing recipe editor in process-template mode for the named template.
 */
@FunctionalInterface
public interface ProcessTemplateEditorNavPort
{
	void openProcessTemplateEditor(String templateName);
}
