package mod.chiselsandbits.render.chiseledblock;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import mcmultipart.client.multipart.ISmartMultipartModel;
import mod.chiselsandbits.chiseledblock.BlockChiseled;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.chiseledblock.data.VoxelBlobStateInstance;
import mod.chiselsandbits.chiseledblock.data.VoxelBlobStateReference;
import mod.chiselsandbits.chiseledblock.data.VoxelNeighborRenderTracker;
import mod.chiselsandbits.render.BaseSmartModel;
import mod.chiselsandbits.render.ModelCombined;
import mod.chiselsandbits.render.cache.CacheMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraftforge.client.model.IFlexibleBakedModel;
import net.minecraftforge.client.model.ISmartBlockModel;
import net.minecraftforge.client.model.ISmartItemModel;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.Optional.Interface;

@Optional.InterfaceList( { @Interface( iface = "mcmultipart.client.multipart.ISmartMultipartModel", modid = "mcmultipart" ) })
public class ChiseledBlockSmartModel extends BaseSmartModel implements ISmartItemModel, ISmartBlockModel, ISmartMultipartModel
{

	static final CacheMap<VoxelBlobStateReference, ChiseledBlockBaked> solidCache = new CacheMap<VoxelBlobStateReference, ChiseledBlockBaked>();
	static final CacheMap<ItemStack, IBakedModel> itemToModel = new CacheMap<ItemStack, IBakedModel>();
	static final CacheMap<VoxelBlobStateInstance, Integer> sideCache = new CacheMap<VoxelBlobStateInstance, Integer>();

	@SuppressWarnings( "unchecked" )
	static private final Map<ModelRenderState, ChiseledBlockBaked>[] modelCache = new Map[5];

	static public void resetCache()
	{
		for ( final ChiselLayer l : ChiselLayer.values() )
		{
			modelCache[l.ordinal()].clear();
		}

		solidCache.clear();
		itemToModel.clear();
	}

	static
	{
		final int count = ChiselLayer.values().length;

		if ( modelCache.length != count )
		{
			throw new RuntimeException( "Invalid Number of EnumWorldBlockLayer" );
		}

		// setup layers.
		for ( final ChiselLayer l : ChiselLayer.values() )
		{
			modelCache[l.ordinal()] = Collections.synchronizedMap( new WeakHashMap<ModelRenderState, ChiseledBlockBaked>() );
		}
	}

	public static int getSides(
			final TileEntityBlockChiseled te )
	{
		final VoxelBlobStateReference ref = te.getBlobStateReference();
		Integer out = null;

		if ( ref == null )
		{
			return 0;
		}

		synchronized ( sideCache )
		{
			out = sideCache.get( ref.getInstance() );
			if ( out == null )
			{
				final VoxelBlob blob = ref.getVoxelBlob();

				// ignore non-solid, and fluids.
				blob.filter( EnumWorldBlockLayer.SOLID );
				blob.filterFluids( false );

				out = blob.getSideFlags( 0, VoxelBlob.dim_minus_one, VoxelBlob.dim2 );
				sideCache.put( ref.getInstance(), out );
			}
		}

		return out;
	}

	public static ChiseledBlockBaked getCachedModel(
			final TileEntityBlockChiseled te,
			final ChiselLayer layer )
	{
		final IExtendedBlockState myState = te.getBasicState();

		final VoxelBlobStateReference data = myState.getValue( BlockChiseled.UProperty_VoxelBlob );
		final VoxelNeighborRenderTracker rTracker = myState.getValue( BlockChiseled.UProperty_VoxelNeighborState );
		Integer blockP = myState.getValue( BlockChiseled.UProperty_Primary_BlockState );

		blockP = blockP == null ? 0 : blockP;

		return getCachedModel( blockP, data, getRenderState( rTracker, data ), layer, getModelFormat() );
	}

	private static VertexFormat getModelFormat()
	{
		return ChiselsAndBitsBakedQuad.VERTEX_FORMAT;
	}

	private static ChiseledBlockBaked getCachedModel(
			final Integer blockP,
			final VoxelBlobStateReference data,
			final ModelRenderState mrs,
			final ChiselLayer layer,
			final VertexFormat format )
	{
		if ( data == null )
		{
			return new ChiseledBlockBaked( blockP, layer, data, new ModelRenderState( null ), format );
		}

		ChiseledBlockBaked out = null;

		if ( format == ChiselsAndBitsBakedQuad.VERTEX_FORMAT )
		{
			if ( layer == ChiselLayer.SOLID )
			{
				out = solidCache.get( data );
			}
			else
			{
				out = mrs == null ? null : modelCache[layer.ordinal()].get( mrs );
			}
		}

		if ( out == null )
		{
			out = new ChiseledBlockBaked( blockP, layer, data, mrs, format );

			if ( out.isEmpty() )
			{
				out = ChiseledBlockBaked.breakingParticleModel( layer, blockP );
			}

			if ( format == ChiselsAndBitsBakedQuad.VERTEX_FORMAT )
			{
				if ( layer == ChiselLayer.SOLID )
				{
					solidCache.put( data, out );
				}
				else if ( mrs != null )
				{
					modelCache[layer.ordinal()].put( mrs, out );
				}
			}
		}

		return out;
	}

	@Override
	public IBakedModel handlePartState(
			final IBlockState state )
	{
		return handleBlockState( state );
	}

	@Override
	public IBakedModel handleBlockState(
			final IBlockState state )
	{
		final IExtendedBlockState myState = (IExtendedBlockState) state;

		final VoxelBlobStateReference data = myState.getValue( BlockChiseled.UProperty_VoxelBlob );
		final VoxelNeighborRenderTracker rTracker = myState.getValue( BlockChiseled.UProperty_VoxelNeighborState );
		Integer blockP = myState.getValue( BlockChiseled.UProperty_Primary_BlockState );

		blockP = blockP == null ? 0 : blockP;

		final EnumWorldBlockLayer layer = net.minecraftforge.client.MinecraftForgeClient.getRenderLayer();

		if ( rTracker != null && rTracker.isDynamic() )
		{
			return ChiseledBlockBaked.breakingParticleModel( ChiselLayer.fromLayer( layer, false ), blockP );
		}

		IBakedModel baked = null;
		int faces = 0;

		if ( layer == EnumWorldBlockLayer.SOLID )
		{
			final ChiseledBlockBaked a = getCachedModel( blockP, data, getRenderState( rTracker, data ), ChiselLayer.fromLayer( layer, false ), getModelFormat() );
			final ChiseledBlockBaked b = getCachedModel( blockP, data, getRenderState( rTracker, data ), ChiselLayer.fromLayer( layer, true ), getModelFormat() );

			faces = a.faceCount() + b.faceCount();

			if ( a.isEmpty() )
			{
				baked = b;
			}
			else if ( b.isEmpty() )
			{
				baked = a;
			}
			else
			{
				baked = new ModelCombined( a, b );
			}
		}
		else
		{
			final ChiseledBlockBaked t = getCachedModel( blockP, data, getRenderState( rTracker, data ), ChiselLayer.fromLayer( layer, false ), getModelFormat() );
			faces = t.faceCount();
			baked = t;
		}

		if ( rTracker != null )
		{
			rTracker.setAbovelimit( layer, faces );
		}

		return baked;
	}

	private static ModelRenderState getRenderState(
			final VoxelNeighborRenderTracker renderTracker,
			final VoxelBlobStateReference data )
	{
		if ( renderTracker != null )
		{
			return renderTracker.getRenderState( data );
		}

		return null;
	}

	@Override
	public IBakedModel handleItemState(
			final ItemStack stack )
	{
		IBakedModel mdl;
		mdl = itemToModel.get( stack );

		if ( mdl != null )
		{
			return mdl;
		}

		NBTTagCompound c = stack.getTagCompound();
		if ( c == null )
		{
			return this;
		}

		c = c.getCompoundTag( "BlockEntityTag" );
		if ( c == null )
		{
			return this;
		}

		final byte[] data = c.getByteArray( "v" );
		byte[] vdata = c.getByteArray( "X" );
		final Integer blockP = c.getInteger( "b" );

		if ( ( vdata == null || vdata.length == 0 ) && data != null && data.length > 0 )
		{
			final VoxelBlob xx = new VoxelBlob();

			try
			{
				xx.fromLegacyByteArray( data );
			}
			catch ( final IOException e )
			{
				// :_(
			}

			vdata = xx.blobToBytes( VoxelBlob.VERSION_COMPACT );
		}

		final IFlexibleBakedModel[] models = new IFlexibleBakedModel[ChiselLayer.values().length];
		for ( final ChiselLayer l : ChiselLayer.values() )
		{
			models[l.ordinal()] = getCachedModel( blockP, new VoxelBlobStateReference( vdata, 0L ), null, l, DefaultVertexFormats.ITEM );
		}

		mdl = new ModelCombined( models );

		itemToModel.put( stack, mdl );

		return mdl;
	}

}
