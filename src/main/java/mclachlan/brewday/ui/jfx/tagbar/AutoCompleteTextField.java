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

import java.util.SortedSet;
import java.util.TreeSet;

import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

/**
 * @author GOXR3PLUS
 * Apache 2.0 License
 * https://github.com/goxr3plus/JavaFX-TagsBar
 */
public class AutoCompleteTextField extends TextField {

	/** The existing auto complete entries. */
	private final SortedSet<String> entries = new TreeSet<>();
	/** The pop up used to select an entry. */
	private ContextMenu contextMenu = new ContextMenu();
	private int maximumEntries = 15;

	private StringBuilder sb = new StringBuilder();
	private int lastLength;

	// Constructor
	public AutoCompleteTextField() {

		// TextChanged Listener
		textProperty().addListener(l -> {
			if (getText().length() == 0)
				contextMenu.hide();
			else {
				if (entries.size() > 0) {
					populatePopup();
					if (!contextMenu.isShowing()) {
						contextMenu.show(AutoCompleteTextField.this, Side.BOTTOM, 0, 0);
						// Request focus on first item
						if (!contextMenu.getItems().isEmpty())
							contextMenu.getSkin().getNode().lookup(".menu-item:nth-child(1)").requestFocus();
					}

				} else
					contextMenu.hide();

			}

		});

		// FocusListener
		focusedProperty().addListener(l -> {
			lastLength = 0;
			sb.delete(0, sb.length());
			contextMenu.hide();
		});

	}

	/**
	 * Get the existing set of autocomplete entries.
	 * 
	 * @return The existing autocomplete entries.
	 */
	public SortedSet<String> getEntries() {
		return entries;
	}

	// TODO ---------------This method not works perfect needs to be redone....
	public void buggedMethod() {
		// KeyReleased Listener
		setOnKeyReleased(key -> {
			KeyCode k = key.getCode();
			// this variable is used to bypass the auto complete process if the
			// length is the same. this occurs if user types fast, the length of
			// textfield will record after the user has typed after a certain
			// delay.
			if (lastLength != (getLength() - getSelectedText().length()))
				lastLength = getLength() - getSelectedText().length();

			// Not causing problems by these buttons
			if (key.isControlDown() || k == KeyCode.BACK_SPACE || k == KeyCode.RIGHT || k == KeyCode.LEFT
					|| k == KeyCode.DELETE || k == KeyCode.HOME || k == KeyCode.END || k == KeyCode.TAB)
				return;

			IndexRange ir = getSelection();
			sb.delete(0, sb.length());
			sb.append(getText());
			// remove selected string index until end so only unselected text
			// will be recorded
			try {
				sb.delete(ir.getStart(), sb.length());
			} catch (Exception e) {
				e.printStackTrace();
			}

			String originalLowered = getText().toLowerCase();
			// Select the first Matching
			for (String s : entries)
				if (s.toLowerCase().startsWith(originalLowered)) {
					try {
						setText(s);
					} catch (Exception e) {
						setText(sb.toString());
					}
					positionCaret(sb.toString().length());
					selectEnd();
					break;
				}

		});

	}

	/**
	 * Populate the entry set with the given search results.
	 * 
	 * @param sortedSet
	 *            The set of matching strings.
	 */
	private void populatePopup() {
		contextMenu.getItems().clear();

		String text = getText().toLowerCase();
		// Filter the first maximumEntries matching the text
		entries.stream().filter(string -> {
			return string.toLowerCase().startsWith(text);
		}).limit(maximumEntries).forEach(s -> {
			// Add the element
			MenuItem item = new MenuItem(s);
			item.setOnAction(a -> {
				setText(s);
				positionCaret(getLength());
			});
			contextMenu.getItems().add(item);
		});

		// Entries to be shown
		// for (int i = 0; i < count; i++) {
		// final String result = searchResult.get(i);
		// Label entryLabel = new Label(result);
		// CustomMenuItem item = new CustomMenuItem(entryLabel, true);
		// item.setOnAction(a->{
		// setText(result);
		// contextMenu.hide();
		// });
		// menuItems.add(item);
		// }

	}
}