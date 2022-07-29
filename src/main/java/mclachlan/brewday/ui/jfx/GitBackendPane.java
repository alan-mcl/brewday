/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.jfx;

import javafx.scene.control.*;
import mclachlan.brewday.Settings;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.util.StringUtils;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class GitBackendPane extends MigPane
{
	private final ToggleButton enable;
	private final TextField remoteUrl;
	private final TextArea textArea;
	private final Button commitAndPush, pullAndOverwrite;

	private boolean refreshing;

	/*-------------------------------------------------------------------------*/
	public GitBackendPane()
	{
		MigPane leftPane = new MigPane();

		Label intro = new Label(StringUtils.getUiString("settings.git.intro"));
		intro.setWrapText(true);
		intro.setMaxWidth(500);
		leftPane.add(intro, "span, wrap");

		enable = new ToggleButton();
		leftPane.add(enable, "span, wrap");

		remoteUrl = new TextField();
		remoteUrl.setPrefWidth(400);
		leftPane.add(new Label(StringUtils.getUiString("settings.git.remote.url")));
		leftPane.add(remoteUrl, "wrap");

		commitAndPush = new Button(StringUtils.getUiString("settings.git.commit.and.push"));
		leftPane.add(commitAndPush, "wrap");

		pullAndOverwrite = new Button(StringUtils.getUiString("settings.git.restore.from.remote"));
		leftPane.add(pullAndOverwrite, "wrap");

		MigPane rightPane = new MigPane();
		rightPane.add(new Label(StringUtils.getUiString("settings.git.command.log")), "wrap");

		textArea = new TextArea();
		textArea.setEditable(false);
		textArea.setPrefRowCount(30);
		rightPane.add(textArea, "spany 5, wrap");

		this.add(leftPane);
		this.add(rightPane);

		refresh();

		Settings settings = Database.getInstance().getSettings();

		// ----

		commitAndPush.setOnAction(event -> Database.getInstance().syncToGitBackend(textArea::appendText));

		pullAndOverwrite.setOnAction(event -> Database.getInstance().syncFromGitBackend(textArea::appendText));

		enable.selectedProperty().addListener(observable ->
		{
			if (!refreshing)
			{
				boolean enabled = enable.isSelected();

				if (enabled)
				{
					boolean isRemote = remoteUrl.getText() != null && remoteUrl.getText().length() > 0;

					String dialogText = isRemote ?
						StringUtils.getUiString("settings.git.enable.dialog.text.remote") :
						StringUtils.getUiString("settings.git.enable.dialog.text");

					JfxUi.OkCancelDialog dialog = new JfxUi.OkCancelDialog(
						StringUtils.getUiString("settings.git.enable.dialog.title"),
						dialogText);

					dialog.showAndWait();

					if (dialog.getOutput())
					{
						// enable the git backend

						if (isRemote)
						{
							settings.set(Settings.GIT_REMOTE_REPO, remoteUrl.getText());
						}

						Database.getInstance().enableGitBackend(textArea::appendText);

						settings.set(Settings.GIT_BACKEND_ENABLED, "true");
						Database.getInstance().saveSettings();
					}
					else
					{
						refreshing = true;
						enable.setSelected(false);
						refreshing = false;
					}

					settings.set(Settings.GIT_BACKEND_ENABLED, "true");
					Database.getInstance().saveSettings();
				}
				else
				{
					// Disabling git backend

					JfxUi.OkCancelDialog dialog = new JfxUi.OkCancelDialog(
						StringUtils.getUiString("settings.git.disable.dialog.title"),
						StringUtils.getUiString("settings.git.disable.dialog.text"));

					dialog.showAndWait();

					if (dialog.getOutput())
					{
						Database.getInstance().disableGitBackend(textArea::appendText);

						settings.set(Settings.GIT_BACKEND_ENABLED, "false");
						Database.getInstance().saveSettings();
					}
					else
					{
						refreshing = true;
						enable.setSelected(true);
						refreshing = false;
					}
				}
				enable.setText(enable.isSelected() ? StringUtils.getUiString("settings.git.disable") : StringUtils.getUiString("settings.git.enable"));
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	public void refresh()
	{
		this.refreshing = true;

		Settings settings = Database.getInstance().getSettings();

		boolean enabled = Boolean.parseBoolean(settings.get(Settings.GIT_BACKEND_ENABLED));
		enable.setSelected(enabled);
		enable.setText(enabled?StringUtils.getUiString("settings.git.disable"):StringUtils.getUiString("settings.git.enable"));
		remoteUrl.setText(settings.get(Settings.GIT_REMOTE_REPO));

		this.refreshing = false;
	}

}
