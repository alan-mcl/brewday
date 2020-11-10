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

package mclachlan.brewday.batch;

import java.time.LocalDate;
import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.process.Volumes;

/**
 *
 */
public class Batch implements V2DataObject
{
	/**
	 * A unique name for this batch.
	 */
	private String id;

	/** The recipe in use */
	private String recipe;

	/** The date of the brew session */
	private LocalDate date;

	/** Actual volumes measured during the session */
	private Volumes actualVolumes;

	/** free form desc for this batch*/
	private String description;

	/** true if inventory for this batch has been consumed */
	private boolean inventoryConsumed;

	/*-------------------------------------------------------------------------*/
	public Batch()
	{
	}

	public Batch(
		String id,
		String description,
		String recipe,
		LocalDate date,
		Volumes actualVolumes,
		boolean inventoryConsumed)
	{
		this.id = id;
		this.description = description;
		this.recipe = recipe;
		this.date = date;
		this.actualVolumes = actualVolumes;
		this.inventoryConsumed = inventoryConsumed;
	}

	public Batch(Batch other)
	{
		this(other.id,
			other.description,
			other.recipe,
			other.date,
			new Volumes(other.actualVolumes),
			other.inventoryConsumed);
	}

	public Batch(String id)
	{
		this.id = id;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String getName()
	{
		return id;
	}

	@Override
	public void setName(String newName)
	{
		this.id = id;
	}

	public String getRecipe()
	{
		return recipe;
	}

	public void setRecipe(String recipe)
	{
		this.recipe = recipe;
	}

	public LocalDate getDate()
	{
		return date;
	}

	public void setDate(LocalDate date)
	{
		this.date = date;
	}

	public Volumes getActualVolumes()
	{
		return actualVolumes;
	}

	public void setActualVolumes(Volumes actualVolumes)
	{
		this.actualVolumes = actualVolumes;
	}

	public void setId(String newName)
	{
		this.id = newName;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public boolean isInventoryConsumed()
	{
		return inventoryConsumed;
	}

	public void setInventoryConsumed(boolean inventoryConsumed)
	{
		this.inventoryConsumed = inventoryConsumed;
	}

	/*-------------------------------------------------------------------------*/

}
