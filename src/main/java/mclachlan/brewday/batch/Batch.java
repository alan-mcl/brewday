package mclachlan.brewday.batch;

import java.util.*;
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
	private Date date;

	/** Actual volumes measured during the session */
	private Volumes actualVolumes;

	/** free form desc for this batch*/
	private String description;

	/*-------------------------------------------------------------------------*/
	public Batch()
	{
	}

	public Batch(
		String id,
		String description,
		String recipe,
		Date date,
		Volumes actualVolumes)
	{
		this.id = id;
		this.description = description;
		this.recipe = recipe;
		this.date = date;
		this.actualVolumes = actualVolumes;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String getName()
	{
		return id;
	}

	public String getRecipe()
	{
		return recipe;
	}

	public void setRecipe(String recipe)
	{
		this.recipe = recipe;
	}

	public Date getDate()
	{
		return date;
	}

	public void setDate(Date date)
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

	/*-------------------------------------------------------------------------*/

}
