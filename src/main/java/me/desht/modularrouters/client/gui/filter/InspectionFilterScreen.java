package me.desht.modularrouters.client.gui.filter;

import com.google.common.base.Joiner;
import me.desht.modularrouters.ModularRouters;
import me.desht.modularrouters.client.gui.widgets.button.BackButton;
import me.desht.modularrouters.client.gui.widgets.textfield.IntegerTextField;
import me.desht.modularrouters.item.smartfilter.InspectionFilter;
import me.desht.modularrouters.logic.filter.matchers.InspectionMatcher;
import me.desht.modularrouters.logic.filter.matchers.InspectionMatcher.ComparisonList;
import me.desht.modularrouters.logic.filter.matchers.InspectionMatcher.InspectionOp;
import me.desht.modularrouters.logic.filter.matchers.InspectionMatcher.InspectionSubject;
import me.desht.modularrouters.network.FilterOp;
import me.desht.modularrouters.network.messages.FilterSettingsMessage;
import me.desht.modularrouters.util.MFLocator;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.widget.ExtendedButton;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.commons.lang3.Range;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static me.desht.modularrouters.client.util.ClientUtil.xlate;

public class InspectionFilterScreen extends AbstractFilterScreen {
    private static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation(ModularRouters.MODID, "textures/gui/inspectionfilter.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 191;

    private int xPos, yPos;
    private ComparisonList comparisonList;
    private IntegerTextField valueTextField;
    private InspectionSubject currentSubject = InspectionSubject.NONE;
    private InspectionOp currentOp = InspectionOp.NONE;
    private final List<Buttons.DeleteButton> deleteButtons = new ArrayList<>();
    private Button matchButton;

    public InspectionFilterScreen(ItemStack filterStack, MFLocator locator) {
        super(filterStack, locator);

        comparisonList = InspectionFilter.getComparisonList(filterStack);
    }

    @Override
    public void init() {
        super.init();

        xPos = (width - GUI_WIDTH) / 2;
        yPos = (height - GUI_HEIGHT) / 2;

        if (locator.filterSlot() >= 0) {
            addRenderableWidget(new BackButton(xPos - 12, yPos, button -> closeGUI()));
        }

        addRenderableWidget(new ExtendedButton(xPos + 8, yPos + 22, 90, 20, xlate(currentSubject.getTranslationKey()), button -> {
            currentSubject = currentSubject.cycle(Screen.hasShiftDown() ? -1 : 1);
            button.setMessage(xlate(currentSubject.getTranslationKey()));
        }));

        addRenderableWidget(new ExtendedButton(xPos + 95, yPos + 22, 20, 20, xlate(currentOp.getTranslationKey()), button -> {
            currentOp = currentOp.cycle(Screen.hasShiftDown() ? -1 : 1);
            button.setMessage(xlate(currentOp.getTranslationKey()));
        }));

        addRenderableWidget(new Buttons.AddButton(xPos + 152, yPos + 23, button -> addEntry()));

        matchButton = new ExtendedButton(xPos + 8, yPos + 167, 60, 20, xlate("modularrouters.guiText.label.matchAll." + comparisonList.isMatchAll()), button -> {
            CompoundTag ext = Util.make(new CompoundTag(), tag -> tag.putBoolean("MatchAll", !comparisonList.isMatchAll()));
            PacketDistributor.SERVER.noArg().send(new FilterSettingsMessage(FilterOp.ANY_ALL_FLAG, locator, ext));
        });
        addRenderableWidget(matchButton);

        deleteButtons.clear();
        for (int i = 0; i < InspectionFilter.MAX_SIZE; i++) {
            Buttons.DeleteButton b = new Buttons.DeleteButton(xPos + 8, yPos + 52 + i * 19, i, button -> sendRemovePosMessage(((Buttons.DeleteButton) button).getId()));
            addRenderableWidget(b);
            deleteButtons.add(b);
        }
        updateDeleteButtonVisibility();

        valueTextField = new IntegerTextField(font, xPos + 120, yPos + 28, 20, 14, Range.of(0, 100)) {
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                    addEntry();
                    return true;
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        };
        valueTextField.useGuiTextBackground();

        addRenderableWidget(valueTextField);
    }

    private void updateDeleteButtonVisibility() {
        for (int i = 0; i < deleteButtons.size(); i++) {
            deleteButtons.get(i).visible = i < comparisonList.items.size();
        }
    }

    private void addEntry() {
        if (currentOp != InspectionOp.NONE && currentSubject != InspectionSubject.NONE) {
            int val = valueTextField.getIntValue();
            sendAddStringMessage(InspectionFilter.NBT_COMPARISON, Joiner.on(" ").join(currentSubject, currentOp, val));
            valueTextField.setValue("");
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        graphics.drawString(font, title, xPos + GUI_WIDTH / 2 - font.width(title) / 2, yPos + 6, 0x404040, false);

        for (int i = 0; i < comparisonList.items.size(); i++) {
            InspectionMatcher.Comparison comparison = comparisonList.items.get(i);
            graphics.drawString(font, comparison.asLocalizedText(), xPos + 28, yPos + 55 + i * 19, 0x404080, false);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.renderBackground(graphics, pMouseX, pMouseY, pPartialTick);

        graphics.blit(TEXTURE_LOCATION, xPos, yPos, 0, 0, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    public void resync(ItemStack stack) {
        comparisonList = InspectionFilter.getComparisonList(stack);
        matchButton.setMessage(xlate("modularrouters.guiText.label.matchAll." + comparisonList.isMatchAll()));
        updateDeleteButtonVisibility();
    }
}
