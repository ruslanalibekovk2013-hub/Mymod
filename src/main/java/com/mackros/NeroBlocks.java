package com.mackros;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Собственные компактные блоки NERO (с настоящими моделями):
 * SmallButton (маленькая кнопка с подсветкой и эффектом нажатия, даёт сигнал), IndicatorLight (индикатор: выкл, зелёный, красный, оранжевый),
 * ElevatorPanel (панель кнопок лифта), DigitalDisplay (цифровой дисплей: текст меняется командой data merge block ... {Text:"3",Color:5635925}).
 */
public final class NeroBlocks {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = Properties.LIT;
    public static final IntProperty COLOR = IntProperty.of("color", 0, 3);

    public static Block SMALL_BUTTON, INDICATOR, LIFT_PANEL, DISPLAY;
    public static BlockEntityType<DisplayBE> DISPLAY_BE;

    private static Block reg(String id, Block b) {
        Identifier key = new Identifier(MackrosMod.ID, id);
        Registry.register(Registries.BLOCK, key, b);
        Registry.register(Registries.ITEM, key, new BlockItem(b, new Item.Settings()));
        return b;
    }

    public static void init() {
        AbstractBlock.Settings s = AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque().strength(0.5f);
        SMALL_BUTTON = reg("small_button", new SmallButton(s));
        INDICATOR = reg("indicator_light", new Indicator(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque().strength(0.5f).luminance(st -> st.get(COLOR) == 0 ? 0 : 7)));
        LIFT_PANEL = reg("lift_panel", new Panel(s));
        DISPLAY = reg("digital_display", new Display(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque().strength(0.5f).luminance(st -> 6)));
        DISPLAY_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(MackrosMod.ID, "digital_display"), FabricBlockEntityTypeBuilder.create(DisplayBE::new, DISPLAY).build());
    }

    /** Тонкая настенная форма: front смотрит наружу, пластина прижата к стене позади. */
    static VoxelShape wall(Direction d, double x1, double y1, double x2, double y2, double depth) {
        double dd = 16 - depth;
        return switch (d) {
            case NORTH -> Block.createCuboidShape(x1, y1, dd, x2, y2, 16);
            case SOUTH -> Block.createCuboidShape(x1, y1, 0, x2, y2, depth);
            case WEST -> Block.createCuboidShape(dd, y1, x1, 16, y2, x2);
            default -> Block.createCuboidShape(0, y1, x1, depth, y2, x2);
        };
    }

    private static Direction placement(ItemPlacementContext c) {
        Direction side = c.getSide();
        return side.getAxis().isHorizontal() ? side : c.getHorizontalPlayerFacing().getOpposite();
    }

    // ------------------------------------------------------------------ маленькая кнопка
    public static class SmallButton extends Block {
        public SmallButton(Settings s) { super(s); setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(LIT, false)); }

        @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(FACING, LIT); }

        @Override public BlockState getPlacementState(ItemPlacementContext c) { return getDefaultState().with(FACING, placement(c)); }

        @Override public VoxelShape getOutlineShape(BlockState st, BlockView w, BlockPos p, ShapeContext c) { return wall(st.get(FACING), 5, 6, 11, 10, 3); }

        @Override
        public ActionResult onUse(BlockState st, World w, BlockPos p, PlayerEntity pl, Hand h, BlockHitResult hit) {
            if (!st.get(LIT)) {
                if (!w.isClient) {
                    w.setBlockState(p, st.with(LIT, true), 3);
                    w.scheduleBlockTick(p, this, 20);
                    w.updateNeighborsAlways(p, this);
                    w.playSound(null, p, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 0.4f, 1.3f);
                }
            }
            return ActionResult.SUCCESS;
        }

        @Override
        public void scheduledTick(BlockState st, ServerWorld w, BlockPos p, Random r) {
            if (st.get(LIT)) {
                w.setBlockState(p, st.with(LIT, false), 3);
                w.updateNeighborsAlways(p, this);
                w.playSound(null, p, SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF, SoundCategory.BLOCKS, 0.4f, 1.1f);
            }
        }

        @Override public boolean emitsRedstonePower(BlockState st) { return true; }

        @Override public int getWeakRedstonePower(BlockState st, BlockView w, BlockPos p, Direction d) { return st.get(LIT) ? 15 : 0; }
    }

    // ------------------------------------------------------------------ индикатор
    public static class Indicator extends Block {
        public Indicator(Settings s) { super(s); setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(COLOR, 0)); }

        @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(FACING, COLOR); }

        @Override public BlockState getPlacementState(ItemPlacementContext c) { return getDefaultState().with(FACING, placement(c)); }

        @Override public VoxelShape getOutlineShape(BlockState st, BlockView w, BlockPos p, ShapeContext c) { return wall(st.get(FACING), 6, 6, 10, 10, 3); }
    }

    // ------------------------------------------------------------------ панель лифта
    public static class Panel extends Block {
        public Panel(Settings s) { super(s); setDefaultState(getDefaultState().with(FACING, Direction.NORTH)); }

        @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(FACING); }

        @Override public BlockState getPlacementState(ItemPlacementContext c) { return getDefaultState().with(FACING, placement(c)); }

        @Override public VoxelShape getOutlineShape(BlockState st, BlockView w, BlockPos p, ShapeContext c) { return wall(st.get(FACING), 3, 1, 13, 15, 2); }
    }

    // ------------------------------------------------------------------ цифровой дисплей
    public static class Display extends Block implements BlockEntityProvider {
        public Display(Settings s) { super(s); setDefaultState(getDefaultState().with(FACING, Direction.NORTH)); }

        @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(FACING); }

        @Override public BlockState getPlacementState(ItemPlacementContext c) { return getDefaultState().with(FACING, placement(c)); }

        @Override public VoxelShape getOutlineShape(BlockState st, BlockView w, BlockPos p, ShapeContext c) { return wall(st.get(FACING), 1, 4, 15, 12, 2); }

        @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new DisplayBE(pos, state); }
    }

    public static class DisplayBE extends BlockEntity {
        public String text = "";
        public int color = 0x55FF55;

        public DisplayBE(BlockPos pos, BlockState state) { super(DISPLAY_BE, pos, state); }

        @Override
        protected void writeNbt(NbtCompound nbt) {
            super.writeNbt(nbt);
            nbt.putString("Text", text);
            nbt.putInt("Color", color);
        }

        @Override
        public void readNbt(NbtCompound nbt) {
            super.readNbt(nbt);
            text = nbt.getString("Text");
            color = nbt.contains("Color") ? nbt.getInt("Color") : 0x55FF55;
        }

        @Override public Packet<ClientPlayPacketListener> toUpdatePacket() { return BlockEntityUpdateS2CPacket.create(this); }

        @Override public NbtCompound toInitialChunkDataNbt() { return createNbt(); }
    }

    private NeroBlocks() { }
}
