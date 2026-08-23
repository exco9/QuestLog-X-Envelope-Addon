package io.github.exco9.questlogenvelope.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Editor for questlog_envelope:package reward-specific fields. */
public final class PackageRewardEditorScreen extends Screen {
    private final Screen parent;
    private final JsonObject rewardEntry;

    private EditBox senderBox;
    private EditBox packageTitleBox;
    private MultiLineEditBox itemsBox;
    private boolean autoClaim;

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
        int panelWidth = Math.min(360, this.width - 30);
        int x = (this.width - panelWidth) / 2;
        int y = Math.max(28, (this.height - 238) / 2);

        this.senderBox = new EditBox(
                this.font,
                x,
                y + 34,
                panelWidth,
                18,
                Component.translatable("questlog_envelope.editor.package.sender")
        );
        this.senderBox.setMaxLength(128);
        this.senderBox.setValue(getString("sender", ""));
        this.senderBox.setHint(Component.translatable("questlog_envelope.editor.package.sender_hint"));
        this.addRenderableWidget(this.senderBox);

        this.packageTitleBox = new EditBox(
                this.font,
                x,
                y + 76,
                panelWidth,
                18,
                Component.translatable("questlog_envelope.editor.package.package_title")
        );
        this.packageTitleBox.setMaxLength(128);
        this.packageTitleBox.setValue(getString("title", "Package"));
        this.addRenderableWidget(this.packageTitleBox);

        this.itemsBox = new MultiLineEditBox(
                this.font,
                x,
                y + 118,
                panelWidth,
                58,
                Component.translatable("questlog_envelope.editor.package.items"),
                Component.empty()
        );
        this.itemsBox.setCharacterLimit(8192);
        this.itemsBox.setValue(itemsToText());
        this.addRenderableWidget(this.itemsBox);

        Button autoClaimButton = Button.builder(autoClaimLabel(), button -> {
            this.autoClaim = !this.autoClaim;
            button.setMessage(autoClaimLabel());
        }).bounds(x, y + 184, panelWidth, 18).build();
        this.addRenderableWidget(autoClaimButton);

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                button -> this.onClose()
        ).bounds(x, y + 210, (panelWidth - 6) / 2, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> saveAndClose()
        ).bounds(x + (panelWidth + 6) / 2, y + 210, (panelWidth - 6) / 2, 20).build());
    }

    private Component autoClaimLabel() {
        return Component.translatable(
                "questlog_envelope.editor.package.auto_claim",
                Component.translatable(this.autoClaim ? "options.on" : "options.off")
        );
    }

    private void saveAndClose() {
        String sender = this.senderBox.getValue().trim();
        if (!sender.isEmpty() && ResourceLocation.tryParse(sender) == null) {
            this.validationError = Component.translatable("questlog_envelope.editor.package.invalid_sender");
            return;
        }

        JsonArray items = parseItems(this.itemsBox.getValue());
        if (items == null) {
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

        if (items.size() == 0) {
            this.rewardEntry.remove("items");
        } else {
            this.rewardEntry.add("items", items);
        }

        this.rewardEntry.addProperty("auto_claim", this.autoClaim);
        this.validationError = null;

        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Nullable
    private static JsonArray parseItems(String text) {
        JsonArray result = new JsonArray();

        for (String rawLine : text.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            try {
                if (line.startsWith("{")) {
                    JsonElement parsed = JsonParser.parseString(line);
                    if (!parsed.isJsonObject()) {
                        return null;
                    }
                    result.add(parsed.getAsJsonObject());
                    continue;
                }

                String[] parts = line.split("\\s+");
                if (parts.length < 1 || parts.length > 2 || ResourceLocation.tryParse(parts[0]) == null) {
                    return null;
                }

                int count = 1;
                if (parts.length == 2) {
                    String countText = parts[1];
                    if (countText.startsWith("x") || countText.startsWith("X")) {
                        countText = countText.substring(1);
                    }
                    count = Integer.parseInt(countText);
                    if (count <= 0) {
                        return null;
                    }
                }

                JsonObject item = new JsonObject();
                item.addProperty("item", parts[0]);
                if (count != 1) {
                    item.addProperty("count", count);
                }
                result.add(item);
            } catch (RuntimeException exception) {
                return null;
            }
        }

        return result;
    }

    private String itemsToText() {
        if (!this.rewardEntry.has("items") || !this.rewardEntry.get("items").isJsonArray()) {
            return "";
        }

        StringBuilder text = new StringBuilder();
        for (JsonElement element : this.rewardEntry.getAsJsonArray("items")) {
            if (text.length() > 0) {
                text.append('\n');
            }

            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                text.append(element.getAsString());
                continue;
            }

            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                String id = null;
                if (object.has("item") && object.get("item").isJsonPrimitive()) {
                    id = object.get("item").getAsString();
                } else if (object.has("id") && object.get("id").isJsonPrimitive()) {
                    id = object.get("id").getAsString();
                }

                boolean simple = id != null
                        && object.entrySet().stream().allMatch(entry ->
                        entry.getKey().equals("item") || entry.getKey().equals("id") || entry.getKey().equals("count"));

                if (simple) {
                    text.append(id);
                    if (object.has("count") && object.get("count").isJsonPrimitive()) {
                        int count = object.get("count").getAsInt();
                        if (count != 1) {
                            text.append(' ').append(count);
                        }
                    }
                    continue;
                }
            }

            text.append(element);
        }
        return text.toString();
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
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int panelWidth = Math.min(360, this.width - 30);
        int x = (this.width - panelWidth) / 2;
        int y = Math.max(28, (this.height - 238) / 2);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, y + 4, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable("questlog_envelope.editor.package.sender"), x, y + 23, 0xFFFFFF, false);
        graphics.drawString(this.font, Component.translatable("questlog_envelope.editor.package.package_title"), x, y + 65, 0xFFFFFF, false);
        graphics.drawString(this.font, Component.translatable("questlog_envelope.editor.package.items"), x, y + 107, 0xFFFFFF, false);

        if (this.validationError != null) {
            graphics.drawCenteredString(this.font, this.validationError, this.width / 2, y + 232, 0xFF5555);
        }
    }
}
