package net.marwinka.mysticalcrops.blockentities;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.marwinka.mysticalcrops.block.BotanicalTableBlock;
import net.marwinka.mysticalcrops.init.BlockEntities;
import net.marwinka.mysticalcrops.networking.ModMessages;
import net.marwinka.mysticalcrops.recipe.BotanicalTableRecipe;
import net.marwinka.mysticalcrops.recipe.InfusionTableRecipe;
import net.marwinka.mysticalcrops.screen.BotanicalTableScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
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

public class BotanicalTableEntity extends BlockEntity implements ExtendedScreenHandlerFactory, ImplementedInventory {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(3, ItemStack.EMPTY);

    protected final PropertyDelegate propertyDelegate;
    private int progress = 0;
    private int maxProgress = 100;

    public void setInventory(DefaultedList<ItemStack> inventory) {
        for (int i = 0; i < inventory.size(); i++) {
            this.inventory.set(i, inventory.get(i));
        }
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (this.world instanceof ServerWorld world) {
            world.getChunkManager().markForUpdate(this.pos);
        }
    }


    public BotanicalTableEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.BOTANICAL_TABLE, pos, state);
        this.propertyDelegate = new PropertyDelegate() {
            public int get(int index) {
                switch (index) {
                    case 0: return BotanicalTableEntity.this.progress;
                    case 1: return BotanicalTableEntity.this.maxProgress;
                    default: return 0;
                }
            }

            public void set(int index, int value) {
                switch(index) {
                    case 0: BotanicalTableEntity.this.progress = value; break;
                    case 1: BotanicalTableEntity.this.maxProgress = value; break;
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
        return Text.literal("Botanical Table");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new BotanicalTableScreenHandler(syncId, inv, this, this.propertyDelegate);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, this.inventory);
        nbt.putInt("progress", this.progress);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, this.inventory);
        this.progress = nbt.getInt("progress");
    }

    private void resetProgress() {
        this.progress = 0;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction side) {
        Direction localDir = this.getWorld().getBlockState(this.pos).get(BotanicalTableBlock.FACING);

        if(side == Direction.UP || side == Direction.DOWN) {
            return slot == 1;
        }

        // Top insert 1
        // Right insert 1
        // Left insert 0
        return switch (localDir) {
            default ->
                    side.getOpposite() == Direction.NORTH && slot == 1 ||
                            side.getOpposite() == Direction.EAST && slot == 1 ||
                            side.getOpposite() == Direction.WEST && slot == 1;
            case EAST ->
                    side.rotateYClockwise() == Direction.NORTH && slot == 1 ||
                            side.rotateYClockwise() == Direction.EAST && slot == 1 ||
                            side.rotateYClockwise() == Direction.WEST && slot == 1;
            case SOUTH ->
                    side == Direction.NORTH && slot == 1 ||
                            side == Direction.EAST && slot == 1 ||
                            side == Direction.WEST && slot == 1;
            case WEST ->
                    side.rotateYCounterclockwise() == Direction.NORTH && slot == 1 ||
                            side.rotateYCounterclockwise() == Direction.EAST && slot == 1 ||
                            side.rotateYCounterclockwise() == Direction.WEST && slot == 1;
        };
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction side) {
        Direction localDir = this.getWorld().getBlockState(this.pos).get(BotanicalTableBlock.FACING);

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

    public void tick() {
        if (!this.world.isClient) {
            SimpleInventory inventory = new SimpleInventory(this.size());
            for (int i = 0; i < this.size(); i++) {
                inventory.setStack(i, this.getStack(i));
            }
            Optional<BotanicalTableRecipe> recipe = this.getWorld().getRecipeManager()
                    .getFirstMatch(BotanicalTableRecipe.Type.INSTANCE, inventory, this.getWorld());

            if (recipe.isPresent() && canInsertAmountIntoOutputSlot(inventory) && canInsertItemIntoOutputSlot(inventory, recipe.get().getOutput().getItem())) {
                this.progress++;
                if (this.progress >= this.maxProgress) {

                    if (this.getStack(0).getItem().isDamageable()) {
                        this.getStack(0).setDamage(this.getStack(0).getDamage() + 1);
                        if (this.getStack(0).getDamage() >= this.getStack(0).getItem().getMaxDamage()) {
                            this.removeStack(0, 1);
                        }
                    }

                    this.removeStack(1, 1);
                    this.setStack(2, new ItemStack(recipe.get().getOutput().getItem(), this.getStack(2).getCount() + 1));
                    resetProgress();
                    this.markDirty();
                }
            } else {
                resetProgress();
            }
        }
    }


    private static boolean canInsertItemIntoOutputSlot(SimpleInventory inventory, Item output) {
        return inventory.getStack(2).getItem() == output || inventory.getStack(2).isEmpty();
    }

    private static boolean canInsertAmountIntoOutputSlot(SimpleInventory inventory) {
        return inventory.getStack(2).getMaxCount() > inventory.getStack(2).getCount();
    }
}