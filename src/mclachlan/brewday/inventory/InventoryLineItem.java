package mclachlan.brewday.inventory;

import mclachlan.brewday.db.v2.V2DataObject;
import mclachlan.brewday.math.ArbitraryPhysicalQuantity;
import mclachlan.brewday.recipe.IngredientAddition;

/**
 *
 */
public class InventoryLineItem implements V2DataObject
{
	/** unique ID */
	private String id;

	/** unique name of the ingredient */
	private String ingredient;

	/** type of this ingredient */
	private IngredientAddition.Type type;

	/**
	 * Amount of the ingredient, unit varies by type.
	 */
	private ArbitraryPhysicalQuantity amount;

	/**
	 * Price of this item, in minor denomination per unit of amount.
	 */
	private int price;

	/*-------------------------------------------------------------------------*/
	public InventoryLineItem()
	{
	}

	/*-------------------------------------------------------------------------*/
	public InventoryLineItem(
		String id,
		String ingredient,
		IngredientAddition.Type type,
		ArbitraryPhysicalQuantity amount,
		int price)
	{
		this.id = id;
		this.ingredient = ingredient;
		this.type = type;
		this.amount = amount;
		this.price = price;
	}

	/*-------------------------------------------------------------------------*/
	@Override
	public String getName()
	{
		return id;
	}

	/*-------------------------------------------------------------------------*/

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getIngredient()
	{
		return ingredient;
	}

	public void setIngredient(String ingredient)
	{
		this.ingredient = ingredient;
	}

	public ArbitraryPhysicalQuantity getAmount()
	{
		return amount;
	}

	public void setAmount(ArbitraryPhysicalQuantity amount)
	{
		this.amount = amount;
	}

	public IngredientAddition.Type getType()
	{
		return type;
	}

	public void setType(IngredientAddition.Type type)
	{
		this.type = type;
	}

	public int getPrice()
	{
		return price;
	}

	public void setPrice(int price)
	{
		this.price = price;
	}
}
