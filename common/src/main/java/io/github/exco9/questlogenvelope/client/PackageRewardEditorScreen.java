package io.github.exco9.questlogenvelope.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.mortuusars.envelope.client.gui.screen.PackageScreen;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.infernalstudios.questlog.core.quests.rewards.ItemReward;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Visual editor for questlog_envelope:package.
 *
 * Each page represents one native Envelope package (six slots). Clicking an
 * inventory item copies its full stack into the selected package slot without
 * removing or changing the player's real inventory. Right-click copies one.
 */
public final class PackageRewardEditorScreen extends Screen {
    private static final int SLOTS_PER_PACKAGE = PackageContents.SLOTS;

    private final Screen parent;
    private final JsonObject rewardEntry;
    private final List<ItemStack> configuredItems = new ArrayList<>();

    private EditBox senderBox;
    private EditBox packageTitleBox;
    private Button previousPageButton;
    private Button nextPageButton;
    private Button removePageButton;

    private boolean autoClaim;
    private boolean contentsLoaded;
    private int currentPage;
    private int selectedSlot = -1;

    private int packageLeft;
    private int packageTop;
    private int panelX;
    private int panelY;
    private int panelWidth;

    @Nullable
    private Component validationError;

    public PackageRewardEditorScreen(Screen parent, JsonObject rewardEntry) {
        super(Component.translatable("questlog_envelope.editor.package.title"));
        this.parent = parent;
        this.rewardEntry = rewardEntry;
        this.autoClaim = rewardEntry.has("auto_claim") && rewardEntry.get("auto_claim").getAsBoolean();
    }

    @Override
    protected void init() {
        if (!this.contentsLoaded) {
            loadContents();
            this.contentsLoaded = true;
        }
        ensureWholePages();

        int packageWidth = 176;
        int packageHeight = 178;
        this.panelWidth = 170;
        int gap = 12;
        int totalWidth = packageWidth + gap + this.panelWidth;

        this.packageLeft = Math.max(4, (this.width - totalWidth) / 2);
        this.packageTop = Math.max(8, (this.height - packageHeight) / 2);
        this.panelX = this.packageLeft + packageWidth + gap;
        this.panelY = this.packageTop;

        if (this.panelX + this.panelWidth > this.width - 4) {
            this.panelX = Math.max(4, this.width - this.panelWidth - 4);
            this.packageLeft = 4;
        }

        this.senderBox = new EditBox(
                this.font,
                this.panelX,
                this.panelY + 32,
                this.panelWidth,
                18,
                Component.translatable("questlog_envelope.editor.package.sender")
        );
        this.senderBox.setMaxLength(128);
        this.senderBox.setValue(getString("sender", ""));
        this.senderBox.setHint(Component.translatable("questlog_envelope.editor.package.sender_hint"));
        this.addRenderableWidget(this.senderBox);

        this.packageTitleBox = new EditBox(
                this.font,
                this.panelX,
                this.panelY + 73,
                this.panelWidth,
                18,
                Component.translatable("questlog_envelope.editor.package.package_title")
        );
        this.packageTitleBox.setMaxLength(128);
        this.packageTitleBox.setValue(getString("title", "Package"));
        this.addRenderableWidget(this.packageTitleBox);

        this.addRenderableWidget(Button.builder(autoClaimLabel(), button -> {
            this.autoClaim = !this.autoClaim;
            button.setMessage(autoClaimLabel());
        }).bounds(this.panelX, this.panelY + 103, this.panelWidth, 18).build());

        int smallButtonWidth = 36;
        this.previousPageButton = this.addRenderableWidget(Button.builder(
                Component.literal("<"),
                button -> changePage(-1)
        ).bounds(this.packageLeft + 7, this.packageTop + 7, smallButtonWidth, 18).build());

        this.nextPageButton = this.addRenderableWidget(Button.builder(
                Component.literal(">"),
                button -> changePage(1)
        ).bounds(this.packageLeft + 133, this.packageTop + 7, smallButtonWidth, 18).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("questlog_envelope.editor.package.add_package"),
                button -> addPackagePage()
        ).bounds(this.panelX, this.panelY + 129, this.panelWidth, 18).build());

        this.removePageButton = this.addRenderableWidget(Button.builder(
                Component.translatable("questlog_envelope.editor.package.remove_package"),
                button -> removeCurrentPackagePage()
        ).bounds(this.panelX, this.panelY + 151, this.panelWidth, 18).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                button -> this.onClose()
        ).bounds(this.panelX, this.panelY + 174, (this.panelWidth - 6) / 2, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> saveAndClose()
        ).bounds(
                this.panelX + (this.panelWidth + 6) / 2,
                this.panelY + 174,
                (this.panelWidth - 6) / 2,
                20
        ).build());

        updatePageButtons();
    }

    private void loadContents() {
        this.configuredItems.clear();
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }

        // New visual format: every nested array is one real six-slot package.
        if (this.rewardEntry.has("packages") && this.rewardEntry.get("packages").isJsonArray()) {
            JsonArray packages = this.rewardEntry.getAsJsonArray("packages");
            for (JsonElement pageElement : packages) {
                if (!pageElement.isJsonArray()) {
                    continue;
                }

                JsonArray page = pageElement.getAsJsonArray();
                for (int slot = 0; slot < SLOTS_PER_PACKAGE; slot++) {
                    JsonElement definition = slot < page.size() ? page.get(slot) : JsonNull.INSTANCE;
                    this.configuredItems.add(parseStack(definition));
                }
            }
            if (!this.configuredItems.isEmpty()) {
                return;
            }
        }

        // Backward compatibility: older quests stored one flat item list. Chunk it
        // into visual pages of six slots when first opened in the new editor.
        if (!this.rewardEntry.has("items") || !this.rewardEntry.get("items").isJsonArray()) {
            return;
        }

        for (JsonElement definition : this.rewardEntry.getAsJsonArray("items")) {
            ItemStack stack = parseStack(definition);
            if (!stack.isEmpty()) {
                this.configuredItems.add(stack);
            }
        }
    }

    private ItemStack parseStack(JsonElement definition) {
        if (definition == null || definition.isJsonNull()
                || this.minecraft == null || this.minecraft.level == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = ItemReward.parseItemStack(definition, this.minecraft.level.registryAccess());
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (definition.isJsonObject()) {
            JsonObject object = definition.getAsJsonObject();
            if (object.has("count") && object.get("count").isJsonPrimitive()) {
                try {
                    stack.setCount(Math.max(1, object.get("count").getAsInt()));
                } catch (RuntimeException ignored) {
                }
            }
        }
        return stack.copy();
    }

    private void ensureWholePages() {
        if (this.configuredItems.isEmpty()) {
            for (int i = 0; i < SLOTS_PER_PACKAGE; i++) {
                this.configuredItems.add(ItemStack.EMPTY);
            }
            return;
        }

        while (this.configuredItems.size() % SLOTS_PER_PACKAGE != 0) {
            this.configuredItems.add(ItemStack.EMPTY);
        }
        this.currentPage = Math.max(0, Math.min(this.currentPage, getPageCount() - 1));
    }

    private int getPageCount() {
        return Math.max(1, this.configuredItems.size() / SLOTS_PER_PACKAGE);
    }

    private int pageStart() {
        return this.currentPage * SLOTS_PER_PACKAGE;
    }

    private Component autoClaimLabel() {
        return Component.translatable(
                "questlog_envelope.editor.package.auto_claim",
                Component.translatable(this.autoClaim ? "options.on" : "options.off")
        );
    }

    private void changePage(int delta) {
        this.currentPage = Math.max(0, Math.min(this.currentPage + delta, getPageCount() - 1));
        this.selectedSlot = -1;
        updatePageButtons();
    }

    private void addPackagePage() {
        for (int i = 0; i < SLOTS_PER_PACKAGE; i++) {
            this.configuredItems.add(ItemStack.EMPTY);
        }
        this.currentPage = getPageCount() - 1;
        this.selectedSlot = -1;
        updatePageButtons();
    }

    private void removeCurrentPackagePage() {
        if (getPageCount() <= 1) {
            for (int i = 0; i < SLOTS_PER_PACKAGE; i++) {
                this.configuredItems.set(i, ItemStack.EMPTY);
            }
            this.selectedSlot = -1;
            updatePageButtons();
            return;
        }

        int start = pageStart();
        for (int i = 0; i < SLOTS_PER_PACKAGE; i++) {
            this.configuredItems.remove(start);
        }
        this.currentPage = Math.min(this.currentPage, getPageCount() - 1);
        this.selectedSlot = -1;
        updatePageButtons();
    }

    private void updatePageButtons() {
        if (this.previousPageButton == null) {
            return;
        }
        this.previousPageButton.active = this.currentPage > 0;
        this.nextPageButton.active = this.currentPage < getPageCount() - 1;
        this.removePageButton.active = getPageCount() > 1 || !isCurrentPageEmpty();
    }

    private boolean isCurrentPageEmpty() {
        int start = pageStart();
        for (int i = 0; i < SLOTS_PER_PACKAGE; i++) {
            if (!this.configuredItems.get(start + i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void saveAndClose() {
        String sender = this.senderBox.getValue().trim();
        if (!sender.isEmpty() && ResourceLocation.tryParse(sender) == null) {
            this.validationError = Component.translatable("questlog_envelope.editor.package.invalid_sender");
            return;
        }

        JsonArray packages = serializePackages();
        if (packages == null) {
            this.validationError = Component.translatable("questlog_envelope.editor.package.invalid_items");
            return;
        }

        if (sender.isEmpty()) {
            this.rewardEntry.remove("sender");
        } else {
            this.rewardEntry.addProperty("sender", sender);
        }

        String title = this.packageTitleBox.getValue().trim();
        if (title.isEmpty()) {
            this.rewardEntry.remove("title");
        } else {
            this.rewardEntry.addProperty("title", title);
        }

        // Saving with the visual editor migrates old flat `items` definitions to
        // explicit six-slot package pages while remaining readable by the backend.
        this.rewardEntry.remove("items");
        if (packages.size() == 0) {
            this.rewardEntry.remove("packages");
        } else {
            this.rewardEntry.add("packages", packages);
        }

        this.rewardEntry.addProperty("auto_claim", this.autoClaim);
        this.validationError = null;

        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Nullable
    private JsonArray serializePackages() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }

        boolean hasAnyItem = this.configuredItems.stream().anyMatch(stack -> !stack.isEmpty());
        if (!hasAnyItem) {
            return new JsonArray();
        }

        JsonArray packages = new JsonArray();
        for (int page = 0; page < getPageCount(); page++) {
            JsonArray pageJson = new JsonArray();
            int start = page * SLOTS_PER_PACKAGE;

            for (int slot = 0; slot < SLOTS_PER_PACKAGE; slot++) {
                ItemStack stack = this.configuredItems.get(start + slot);
                if (stack.isEmpty()) {
                    pageJson.add(JsonNull.INSTANCE);
                    continue;
                }

                JsonElement encoded = ItemStack.CODEC.encodeStart(
                        RegistryOps.create(JsonOps.INSTANCE, this.minecraft.level.registryAccess()),
                        stack
                ).result().orElse(null);

                if (encoded == null) {
                    return null;
                }
                pageJson.add(encoded);
            }

            // Do not emit entirely empty extra pages. Empty positions inside a real
            // page remain as JSON null so slot placement is preserved exactly.
            boolean pageHasItem = false;
            for (JsonElement element : pageJson) {
                if (!element.isJsonNull()) {
                    pageHasItem = true;
                    break;
                }
            }
            if (pageHasItem) {
                packages.add(pageJson);
            }
        }
        return packages;
    }

    private String getString(String key, String fallback) {
        return this.rewardEntry.has(key) ? this.rewardEntry.get(key).getAsString() : fallback;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int packageSlot = getPackageSlotAt(mouseX, mouseY);
        if (packageSlot >= 0) {
            int absoluteSlot = pageStart() + packageSlot;
            if (button == 1) {
                this.configuredItems.set(absoluteSlot, ItemStack.EMPTY);
                if (this.selectedSlot == absoluteSlot) {
                    this.selectedSlot = -1;
                }
                updatePageButtons();
            } else if (button == 0) {
                this.selectedSlot = absoluteSlot;
            }
            return true;
        }

        int inventorySlot = getInventorySlotAt(mouseX, mouseY);
        if (inventorySlot >= 0 && (button == 0 || button == 1)) {
            copyInventoryStack(inventorySlot, button == 1);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void copyInventoryStack(int inventorySlot, boolean singleItem) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        ItemStack source = this.minecraft.player.getInventory().getItem(inventorySlot);
        if (source.isEmpty()) {
            return;
        }
        if (!PackageContents.canHold(source)) {
            this.validationError = Component.translatable("questlog_envelope.editor.package.cannot_package");
            return;
        }

        int target = this.selectedSlot;
        if (target < pageStart() || target >= pageStart() + SLOTS_PER_PACKAGE) {
            target = firstEmptySlotOnCurrentPage();
        }
        if (target < 0) {
            addPackagePage();
            target = pageStart();
        }

        ItemStack copy = source.copy();
        if (singleItem) {
            copy.setCount(1);
        }
        this.configuredItems.set(target, copy);
        this.selectedSlot = target;
        this.validationError = null;
        updatePageButtons();
    }

    private int firstEmptySlotOnCurrentPage() {
        int start = pageStart();
        for (int i = 0; i < SLOTS_PER_PACKAGE; i++) {
            if (this.configuredItems.get(start + i).isEmpty()) {
                return start + i;
            }
        }
        return -1;
    }

    private int getPackageSlotAt(double mouseX, double mouseY) {
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                int x = this.packageLeft + 62 + column * 18;
                int y = this.packageTop + 33 + row * 18;
                if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                    return column + row * 3;
                }
            }
        }
        return -1;
    }

    private int getInventorySlotAt(double mouseX, double mouseY) {
        // Main inventory: indices 9..35.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int x = this.packageLeft + 8 + column * 18;
                int y = this.packageTop + 96 + row * 18;
                if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                    return 9 + column + row * 9;
                }
            }
        }

        // Hotbar: indices 0..8.
        for (int column = 0; column < 9; column++) {
            int x = this.packageLeft + 8 + column * 18;
            int y = this.packageTop + 154;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                return column;
            }
        }
        return -1;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(graphics);
        graphics.blit(PackageScreen.TEXTURE, this.packageLeft, this.packageTop, 0, 0, 176, 178);
        graphics.fill(
                this.panelX - 6,
                this.panelY - 6,
                this.panelX + this.panelWidth + 6,
                this.panelY + 202,
                0xB0101010
        );
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderPackageContents(graphics);
        renderPlayerInventory(graphics);

        graphics.drawCenteredString(
                this.font,
                Component.translatable(
                        "questlog_envelope.editor.package.page",
                        this.currentPage + 1,
                        getPageCount()
                ),
                this.packageLeft + 88,
                this.packageTop + 12,
                0xFFFFFF
        );

        graphics.drawCenteredString(
                this.font,
                Component.translatable("questlog_envelope.editor.package.settings"),
                this.panelX + this.panelWidth / 2,
                this.panelY + 6,
                0xFFFFFF
        );
        graphics.drawString(
                this.font,
                Component.translatable("questlog_envelope.editor.package.sender"),
                this.panelX,
                this.panelY + 21,
                0xFFFFFF,
                false
        );
        graphics.drawString(
                this.font,
                Component.translatable("questlog_envelope.editor.package.package_title"),
                this.panelX,
                this.panelY + 62,
                0xFFFFFF,
                false
        );

        graphics.drawCenteredString(
                this.font,
                Component.translatable("questlog_envelope.editor.package.copy_hint"),
                this.packageLeft + 88,
                this.packageTop + 79,
                0xFFB0B0B0
        );

        ItemStack hovered = getHoveredStack(mouseX, mouseY);
        if (!hovered.isEmpty()) {
            graphics.renderTooltip(this.font, hovered, mouseX, mouseY);
        }

        if (this.validationError != null) {
            graphics.drawCenteredString(
                    this.font,
                    this.validationError,
                    this.panelX + this.panelWidth / 2,
                    this.panelY + 198,
                    0xFF5555
            );
        }
    }

    private void renderPackageContents(GuiGraphics graphics) {
        int start = pageStart();
        for (int localSlot = 0; localSlot < SLOTS_PER_PACKAGE; localSlot++) {
            int row = localSlot / 3;
            int column = localSlot % 3;
            int x = this.packageLeft + 62 + column * 18;
            int y = this.packageTop + 33 + row * 18;
            int absoluteSlot = start + localSlot;

            if (absoluteSlot == this.selectedSlot) {
                graphics.fill(x - 1, y - 1, x + 17, y, 0xFFFFFFFF);
                graphics.fill(x - 1, y + 16, x + 17, y + 17, 0xFFFFFFFF);
                graphics.fill(x - 1, y, x, y + 16, 0xFFFFFFFF);
                graphics.fill(x + 16, y, x + 17, y + 16, 0xFFFFFFFF);
            }

            renderStack(graphics, this.configuredItems.get(absoluteSlot), x, y);
        }
    }

    private void renderPlayerInventory(GuiGraphics graphics) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        Inventory inventory = this.minecraft.player.getInventory();

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slot = 9 + column + row * 9;
                int x = this.packageLeft + 8 + column * 18;
                int y = this.packageTop + 96 + row * 18;
                renderStack(graphics, inventory.getItem(slot), x, y);
            }
        }

        for (int column = 0; column < 9; column++) {
            int x = this.packageLeft + 8 + column * 18;
            int y = this.packageTop + 154;
            renderStack(graphics, inventory.getItem(column), x, y);
        }
    }

    private void renderStack(GuiGraphics graphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return;
        }
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(this.font, stack, x, y);
    }

    private ItemStack getHoveredStack(int mouseX, int mouseY) {
        int packageSlot = getPackageSlotAt(mouseX, mouseY);
        if (packageSlot >= 0) {
            return this.configuredItems.get(pageStart() + packageSlot);
        }

        int inventorySlot = getInventorySlotAt(mouseX, mouseY);
        if (inventorySlot >= 0 && this.minecraft != null && this.minecraft.player != null) {
            return this.minecraft.player.getInventory().getItem(inventorySlot);
        }
        return ItemStack.EMPTY;
    }
}
