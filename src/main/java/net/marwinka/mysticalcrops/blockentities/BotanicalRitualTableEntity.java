package net.marwinka.mysticalcrops.blockentities;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.marwinka.mysticalcrops.blocks.BotanicalRitualTableBlock;
import net.marwinka.mysticalcrops.init.BlockEntities;
import net.marwinka.mysticalcrops.networking.ModMessages;
import net.marwinka.mysticalcrops.recipe.BotanicalRitualTableRecipe;
import net.marwinka.mysticalcrops.screen.BotanicalRitualTableScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BotanicalRitualTableEntity extends net.minecraft.block.entity.BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(10, ItemStack.EMPTY);

    protected final PropertyDelegate propertyDelegate;
    private int progress = 0;
    private int maxProgress = 90;

    public BotanicalRitualTableEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.BOTANICAL_RITUAL_TABLE, pos, state);
        this.propertyDelegate = new PropertyDelegate() {
            public int get(int index) {
                switch (index) {
                    case 0: return BotanicalRitualTableEntity.this.progress;
                    case 1: return BotanicalRitualTableEntity.this.maxProgress;
                    default: return 0;
                }
            }

            public void set(int index, int value) {
                switch(index) {
                    case 0: BotanicalRitualTableEntity.this.progress = value; break;
                    case 1: BotanicalRitualTableEntity.this.maxProgress = value; break;
                }
            }

            public int size() {
                return 2;
            }
        };
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return this.inventory;
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Botanical Ritual Table");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new BotanicalRitualTableScreenHandler(syncId, inv, this, this.propertyDelegate);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, inventory);
        nbt.putInt("botanical_ritual_table.progress", progress);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        Inventories.readNbt(nbt, inventory);
        super.readNbt(nbt);
        progress = nbt.getInt("botanical_ritual_table.progress");
    }

    private void resetProgress() {
        this.progress = 0;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction side) {
        Direction localDir = this.getWorld().getBlockState(this.pos).get(BotanicalRitualTableBlock.FACING);

        if(side == Direction.UP) {
            return side == Direction.UP && slot == 1;
        }
        if(side == Direction.DOWN) {
            return false;
        }

        // Top insert 1
        // Right insert 1
        // Left insert 0
        return switch (localDir) {
            default ->
                    side.getOpposite() == Direction.NORTH && slot == 1 ||
                            side.getOpposite() == Direction.EAST && slot == 0 ||
                            side.getOpposite() == Direction.WEST && slot == 1 ||
                            side.getOpposite() == Direction.SOUTH && slot == 0;
            case EAST ->
                    side.rotateYClockwise() == Direction.NORTH && slot == 1 ||
                            side.rotateYClockwise() == Direction.EAST && slot == 0 ||
                            side.rotateYClockwise() == Direction.WEST && slot == 1||
                            side.rotateYClockwise() == Direction.SOUTH && slot == 0;
            case SOUTH ->
                    side == Direction.NORTH && slot == 1 ||
                            side == Direction.EAST && slot == 1 ||
                            side == Direction.WEST && slot == 0||
                            side == Direction.SOUTH && slot == 0;
            case WEST ->
                    side.rotateYCounterclockwise() == Direction.NORTH && slot == 1 ||
                            side.rotateYCounterclockwise() == Direction.EAST && slot == 0 ||
                            side.rotateYCounterclockwise() == Direction.WEST && slot == 1||
                            side.rotateYCounterclockwise() == Direction.SOUTH && slot == 0;
        };
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction side) {
        Direction localDir = this.getWorld().getBlockState(this.pos).get(BotanicalRitualTableBlock.FACING);

        if(side == Direction.UP) {
            return slot == 2;
        }

        // Down extract 2
        if(side == Direction.DOWN) {
            return slot == 2;
        }

        // bottom extract 2
        // right extract 2
        return switch (localDir) {
            default -> side.getOpposite() == Direction.SOUTH && slot == 2 ||
                    side.getOpposite() == Direction.EAST && slot == 2;
            case EAST -> side.rotateYClockwise() == Direction.SOUTH && slot == 2 ||
                    side.rotateYClockwise() == Direction.EAST && slot == 2;
            case SOUTH -> side == Direction.SOUTH && slot == 2 ||
                    side == Direction.EAST && slot == 2;
            case WEST -> side.rotateYCounterclockwise() == Direction.SOUTH && slot == 2 ||
                    side.rotateYCounterclockwise() == Direction.EAST && slot == 2;
        };
    }


    public static void tick(World world, BlockPos blockPos, BlockState state, BotanicalRitualTableEntity entity) {
        if(world.isClient()) {
            return;
        }

        if(hasRecipe(entity)) {
            entity.progress++;
            markDirty(world, blockPos, state);
            if(entity.progress >= entity.maxProgress) {
                craftItem(entity);
            }
        } else {
            entity.resetProgress();
            markDirty(world, blockPos, state);
        }
    }

    private static void craftItem(BotanicalRitualTableEntity entity) {
        SimpleInventory inventory = new SimpleInventory(entity.size());
        for (int i = 0; i < entity.size(); i++) {
            inventory.setStack(i, entity.getStack(i));
        }

        Optional<BotanicalRitualTableRecipe> recipe = entity.getWorld().getRecipeManager()
                .getFirstMatch(BotanicalRitualTableRecipe.Type.INSTANCE, inventory, entity.getWorld());

        if(recipe.isPresent()) {
            entity.getStack(0).setDamage(entity.getStack(0).getDamage() + 1);
            if(entity.getStack(0).getDamage() >= 250)
            {
                entity.removeStack(0, 1);
            }
            entity.removeStack(1, 1);
            entity.removeStack(2, 1);
            entity.removeStack(3, 1);
            entity.removeStack(4, 1);
            entity.removeStack(5, 1);
            entity.removeStack(6, 1);
            entity.removeStack(7, 1);
            entity.removeStack(8, 1);

            entity.setStack(9, new ItemStack(recipe.get().getOutput().getItem(),
                    entity.getStack(9).getCount() + recipe.get().getOutput().getCount()));

            entity.resetProgress();
        }
    }

    private static boolean hasRecipe(BotanicalRitualTableEntity entity) {
        SimpleInventory inventory = new SimpleInventory(entity.size());
        for (int i = 0; i < entity.size(); i++) {
            inventory.setStack(i, entity.getStack(i));
        }

        Optional<BotanicalRitualTableRecipe> match = entity.getWorld().getRecipeManager()
                .getFirstMatch(BotanicalRitualTableRecipe.Type.INSTANCE, inventory, entity.getWorld());


            return match.isPresent() && canInsertAmountIntoOutputSlot(inventory)
                    && canInsertItemIntoOutputSlot(inventory,match.get().getOutput().getItem());

    }

    private static boolean canInsertItemIntoOutputSlot(SimpleInventory inventory, Item output) {
        return inventory.getStack(9).getItem() == output || inventory.getStack(9).isEmpty();
    }

    private static boolean canInsertAmountIntoOutputSlot(SimpleInventory inventory) {
        return inventory.getStack(9).getMaxCount() > inventory.getStack(9).getCount();
    }
    public void setInventory(DefaultedList<ItemStack> inventory) {
        for (int i = 0; i < inventory.size(); i++) {
            this.inventory.set(i, inventory.get(i));
        }
    }

    @Override
    public void markDirty() {
        if(!world.isClient()) {
            PacketByteBuf data = PacketByteBufs.create();
            data.writeInt(inventory.size());
            for(int i = 0; i < inventory.size(); i++) {
                data.writeItemStack(inventory.get(i));
            }
            data.writeBlockPos(getPos());

            for (ServerPlayerEntity player : PlayerLookup.tracking((ServerWorld) world, getPos())) {
                ServerPlayNetworking.send(player, ModMessages.ITEM_SYNC, data);
            }
        }

        super.markDirty();
    }
    public ItemStack getRenderStack() {
        return this.getStack(1);
    }
    public ItemStack getRenderStack1() {
        return this.getStack(2);
    }
    public ItemStack getRenderStack2() {
        return this.getStack(3);
    }
    public ItemStack getRenderStack3() {
        return this.getStack(4);
    }
    public ItemStack getRenderStack4() {
        return this.getStack(5);
    }
    public ItemStack getRenderStack5() {
        return this.getStack(6);
    }
    public ItemStack getRenderStack6() {
        return this.getStack(7);
    }
    public ItemStack getRenderStack7() {
        return this.getStack(8);
    }
    public ItemStack getRenderStack8() {
        return this.getStack(9);
    }
}
