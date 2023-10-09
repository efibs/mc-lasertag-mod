package de.kleiner3.lasertag.mixin;

import de.kleiner3.lasertag.item.Items;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into the HandledScreen.class to prevent the player from dropping his weapon out of the inventory.
 *
 * @author Étienne Muser
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
    private void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {

        switch (actionType) {
            case PICKUP -> {
                // Allow to put it back into a slot
                if (slot != null) {
                    return;
                }

                // Get the stack that is currently being held
                var itemStack = ((HandledScreen)(Object)this).getScreenHandler().getCursorStack();

                // If is lasertag weapon
                if (itemStack.isOf(Items.LASERTAG_WEAPON)) {

                    // Don't drop it
                    ci.cancel();
                }
            }
            case THROW -> {
                // If is lasertag weapon
                if (slot.getStack().isOf(Items.LASERTAG_WEAPON)) {

                    // Don't drop it
                    ci.cancel();
                }
            }
        }
    }
}
