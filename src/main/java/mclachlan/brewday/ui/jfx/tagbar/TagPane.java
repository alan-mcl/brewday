/*
 * This file is part of brewday.
 *
 * brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.jfx.tagbar;

import java.util.*;
import java.util.function.*;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.ui.jfx.Icons;
import mclachlan.brewday.ui.jfx.JfxUi;

/**
 * This is a modified version of the code by Terah here:
 * https://bitbucket.org/snippets/Elltz/aKjMG/tagdisplay
 */
public class TagPane extends FlowPane
{
	private AutoCompleteTextField textField;
	private final Label addTagLabel;
	private Consumer<String> tagAddHandler;
	private Consumer<String> tagRemoveHandler;

	/*-------------------------------------------------------------------------*/
	public TagPane(List<String> tags)
	{
		this.setHgap(3);
		this.setVgap(3);


		textField = new AutoCompleteTextField();
		textField.getEntries().addAll(tags);

		addTagLabel = new Label(StringUtils.getUiString("ui.add.tag"));
		addTagLabel.setTextFill(Paint.valueOf("gray"));

		// ------------
		textField.setOnKeyPressed(evt -> onKeyPressed(evt, textField));

		addTagLabel.setOnMouseClicked(
			event ->
			{
				getChildren().remove(addTagLabel);
				getChildren().add(textField);
				textField.requestFocus();
			});

		this.setOnMouseClicked(mouseEvent ->
		{
			if (mouseEvent.getTarget() == this && textField.getParent() == null)
			{
				getChildren().remove(addTagLabel);
				getChildren().add(textField);
				textField.requestFocus();
			}
		});
	}

	/*-------------------------------------------------------------------------*/
	private void onKeyPressed(KeyEvent evt, AutoCompleteTextField textField)
	{
		if (evt.getCode() == KeyCode.ENTER || evt.getCode() == KeyCode.TAB)
		{
			String text = textField.getText();

			// no empty tag
			if (text.isEmpty())
			{
				return;
			}

			// no duplicates
			for (Node n : getChildren())
			{
				if (n instanceof CloseTag)
				{
					if (((CloseTag)n).getText().equalsIgnoreCase(text))
					{
						return;
					}
				}
			}

			// if we got this far it's time to add the tag
			addTagWidget(text);

			// clear the text field for new tag entry
			textField.clear();

			// add this tag to the existing tags in the autocomplete
			textField.getEntries().add(text);

			// hide the text field
			getChildren().remove(this.textField);

			// put back the add tag label
			getChildren().add(addTagLabel);

			// notify any tag add handler
			if (tagAddHandler != null)
			{
				tagAddHandler.accept(text);
			}
		}

		// on a TAB, allow more tag entry
		if (evt.getCode() == KeyCode.TAB)
		{
			getChildren().remove(addTagLabel);
			getChildren().add(this.textField);
			this.textField.requestFocus();
		}
	}

	/*-------------------------------------------------------------------------*/
	private void addTagWidget(String text)
	{
		CloseTag tag = new CloseTag(text);
		tag.setOnCloseAction(evt -> removeTag(tag));
		getChildren().add(tag);
	}

	/*-------------------------------------------------------------------------*/
	private void removeTag(CloseTag tag)
	{
		String text = tag.getText();

		getChildren().remove(tag);
		textField.getEntries().add(text);

		if (tagRemoveHandler != null)
		{
			tagRemoveHandler.accept(text);
		}
	}

	/*-------------------------------------------------------------------------*/
	public void refresh(List<String> tags, List<String> allTags)
	{
		textField.getEntries().clear();
		textField.getEntries().addAll(allTags);

		getChildren().clear();

		for (String s : tags)
		{
			addTagWidget(s);
		}

		// make sure the add tag label is there if it is not already
		getChildren().remove(addTagLabel);
		getChildren().add(addTagLabel);
	}

	/*-------------------------------------------------------------------------*/
	public void onTagAdd(Consumer<String> handler)
	{
		tagAddHandler = handler;
	}

	public void onTagRemove(Consumer<String> handler)
	{
		tagRemoveHandler = handler;
	}
}

/*-------------------------------------------------------------------------*/

class CloseTag extends HBox implements Comparable<CloseTag>
{
	private final Label label;
	private final Label closeIcon;

	public CloseTag(String text)
	{
		setStyle("-fx-padding:3;");

		setBorder(
			new Border(
				new BorderStroke(
					Color.GRAY,
					BorderStrokeStyle.SOLID,
					new CornerRadii(5),
					BorderWidths.DEFAULT)));

//    Text icon = GlyphsDude.createIcon(FontAwesomeIcon.TIMES_CIRCLE);
//		ImageView graphic = new ImageView(new Image(getClass().getResourceAsStream("x.png")));
		ImageView graphic = JfxUi.getImageView(Icons.deleteIcon, 15);
		closeIcon = new Label(null, graphic);

		label = new Label(text, new StackPane(closeIcon));
		label.setContentDisplay(ContentDisplay.RIGHT);
		getChildren().add(label);
	}

	public void setOnCloseAction(EventHandler<? super MouseEvent> action)
	{
		closeIcon.setOnMouseClicked(action);
	}

	public String getText()
	{
		return label.getText();
	}

	@Override
	public int compareTo(CloseTag other)
	{
		return getText().compareTo(other.getText());
	}
}
