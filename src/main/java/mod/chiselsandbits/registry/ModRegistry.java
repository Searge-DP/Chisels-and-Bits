package mod.chiselsandbits.registry;

import mod.chiselsandbits.client.CreativeClipboardTab;
import mod.chiselsandbits.client.ModCreativeTab;
import mod.chiselsandbits.core.ChiselsAndBits;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

public class ModRegistry
{

	public static final String unlocalizedPrefix = "mod." + ChiselsAndBits.MODID + ".";

	static ModCreativeTab creativeTab = new ModCreativeTab();
	static CreativeClipboardTab creativeClipboard = null;

	static
	{
		if ( ChiselsAndBits.getConfig().creativeClipboardSize > 0 )
		{
			creativeClipboard = new CreativeClipboardTab();
		}
	}

	protected <T extends Item> T registerItem(
			final boolean enabled,
			final T item,
			final String name )
	{
		if ( enabled )
		{
			item.setCreativeTab( creativeTab );
			GameRegistry.registerItem( item.setUnlocalizedName( unlocalizedPrefix + name ), name );
			return item;
		}

		return null;
	}

	protected void registerBlock(
			final Block block,
			final Class<? extends ItemBlock> itemBlock,
			final String name )
	{
		block.setCreativeTab( creativeTab );
		GameRegistry.registerBlock( block.setUnlocalizedName( unlocalizedPrefix + name ), itemBlock == null ? ItemBlock.class : itemBlock, name );
	}

	protected void ShapedOreRecipe(
			final Block result,
			final Object... recipe )
	{
		ShapedOreRecipe( Item.getItemFromBlock( result ), recipe );
	}

	protected void ShapelessOreRecipe(
			final Block result,
			final Object... recipe )
	{
		ShapelessOreRecipe( Item.getItemFromBlock( result ), recipe );
	}

	protected void ShapedOreRecipe(
			final Item result,
			final Object... recipe )
	{
		if ( result != null )
		{
			for ( final Object o : recipe )
			{
				if ( o == null || o instanceof ItemStack && ( (ItemStack) o ).getItem() == null )
				{
					return;
				}
			}

			GameRegistry.addRecipe( new ShapedOreRecipe( result, recipe ) );
		}
	}

	protected void ShapelessOreRecipe(
			final Item result,
			final Object... recipe )
	{
		if ( result != null )
		{
			for ( final Object o : recipe )
			{
				if ( o == null || o instanceof ItemStack && ( (ItemStack) o ).getItem() == null )
				{
					return;
				}
			}

			GameRegistry.addRecipe( new ShapelessOreRecipe( result, recipe ) );
		}
	}
}
