package com.abmstudios.DigBuild;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;


public class InventoryUI {

    private final Inventory inventory;

    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;

    private final Texture textureAtlas;

    private final Texture slotTexture;
    private final Texture selectedSlotTexture;

    private boolean craftingTableOpen = false;


    // =============================================================
    // INVENTORY SETTINGS
    // =============================================================

    private static final int COLUMNS = 9;

    private static final float SLOT_SIZE = 64f;
    private static final float SLOT_GAP = 4f;

    private static final float TOTAL_SLOT_SIZE =
        SLOT_SIZE + SLOT_GAP;

    private static final float UI_PADDING = 12f;

    private static final float HOTBAR_BOTTOM = 20f;

    private static final float ITEM_PADDING = 14f;


    // =============================================================
    // INVENTORY STATE
    // =============================================================

    private boolean inventoryOpen = false;

    private int selectedHotbarSlot = 0;


    // =============================================================
    // DRAGGING STATE
    // =============================================================

    private boolean dragging = false;

    private int draggedSlot = -1;

    private ItemStack draggedStack = null;

    private float dragX;
    private float dragY;

    private boolean leftMouseWasDown = false;

    // True when the currently dragged stack was created by splitting
    // a stack with right-click. The original stack remains in its slot.
    private boolean draggedStackIsSplit = false;

    // =============================================================
    // CRAFTING
    // =============================================================

    private static final int CRAFTING_SIZE = 2;

    private static final int TABLE_CRAFTING_SIZE = 3;

    private final ItemStack[][] craftingGrid =
        new ItemStack[TABLE_CRAFTING_SIZE][TABLE_CRAFTING_SIZE];


    private Recipe currentRecipe = null;

    private static final int CRAFTING_SLOT_BASE = -100;
    private static final int CRAFTING_OUTPUT_SLOT = -200;

    private boolean draggingFromCrafting = false;




    // =============================================================
    // CONSTRUCTOR
    // =============================================================

    private int getCurrentCraftingSize() {
        return craftingTableOpen
            ? TABLE_CRAFTING_SIZE
            : CRAFTING_SIZE;
    }

    public InventoryUI(Inventory inventory) {

        this.inventory = inventory;

        batch = new SpriteBatch();

        shapeRenderer = new ShapeRenderer();

        font = new BitmapFont();
        font.setColor(Color.WHITE);

        // ---------------------------------------------------------
        // Block texture atlas
        // ---------------------------------------------------------

        textureAtlas = new Texture(
            Gdx.files.internal("textureatlas.png")
        );

        textureAtlas.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );


        // ---------------------------------------------------------
        // Inventory slot texture
        // ---------------------------------------------------------

        slotTexture = new Texture(
            Gdx.files.internal("slot.png")
        );

        slotTexture.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );


        // ---------------------------------------------------------
        // Selected hotbar slot texture
        // ---------------------------------------------------------

        selectedSlotTexture = new Texture(
            Gdx.files.internal("selectedSlot.png")
        );

        selectedSlotTexture.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );
    }


    // =============================================================
    // UPDATE
    // =============================================================

    public void update() {

        // ---------------------------------------------------------
        // Open / close inventory
        // ---------------------------------------------------------

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {

            if (inventoryOpen) {

                inventoryOpen = false;
                craftingTableOpen = false;

                cancelDrag();

                Gdx.input.setCursorCatched(true);

            } else {

                inventoryOpen = true;
                craftingTableOpen = false;

                Gdx.input.setCursorCatched(false);
            }
        }


        // ---------------------------------------------------------
        // Hotbar selection
        // ---------------------------------------------------------

        if (!inventoryOpen) {

            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
                selectedHotbarSlot = 0;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
                selectedHotbarSlot = 1;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
                selectedHotbarSlot = 2;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
                selectedHotbarSlot = 3;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) {
                selectedHotbarSlot = 4;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6)) {
                selectedHotbarSlot = 5;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_7)) {
                selectedHotbarSlot = 6;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_8)) {
                selectedHotbarSlot = 7;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_9)) {
                selectedHotbarSlot = 8;
            }

            return;
        }


        // ---------------------------------------------------------
        // Inventory dragging
        // ---------------------------------------------------------

        updateDragging();
    }


    // =============================================================
    // DRAGGING UPDATE
    // =============================================================

    private void updateDragging() {

        dragX = Gdx.input.getX();

        dragY =
            Gdx.graphics.getHeight() -
                Gdx.input.getY();

        boolean leftMouseDown =
            Gdx.input.isButtonPressed(
                Input.Buttons.LEFT
            );

        // ---------------------------------------------------------
        // Right-click: split a stack in half
        // ---------------------------------------------------------

        if (!dragging &&
            Gdx.input.isButtonJustPressed(
                Input.Buttons.RIGHT
            )) {

            int slot =
                getSlotAt(
                    dragX,
                    dragY
                );

            if (slot >= CRAFTING_SLOT_BASE &&
                slot < CRAFTING_SLOT_BASE +
                    getCurrentCraftingSize() * getCurrentCraftingSize()) {

                int craftingIndex =
                    slot - CRAFTING_SLOT_BASE;

                int craftingSize =
                    getCurrentCraftingSize();

                int x =
                    craftingIndex % craftingSize;

                int y =
                    craftingIndex / craftingSize;

                ItemStack stack =
                    craftingGrid[y][x];

                startSplitDrag(
                    stack,
                    true,
                    craftingIndex
                );

            } else if (slot >= 0) {

                ItemStack stack =
                    inventory.getSlot(slot);

                startSplitDrag(
                    stack,
                    false,
                    slot
                );
            }
        }

        // ---------------------------------------------------------
        // Left-click: pick up the entire stack
        // ---------------------------------------------------------

        if (!dragging &&
            Gdx.input.isButtonJustPressed(
                Input.Buttons.LEFT
            )) {

            int slot =
                getSlotAt(
                    dragX,
                    dragY
                );

            if (slot == CRAFTING_OUTPUT_SLOT) {

                craftItem();

            } else if (slot >= CRAFTING_SLOT_BASE &&
                       slot < CRAFTING_SLOT_BASE +
                    getCurrentCraftingSize() * getCurrentCraftingSize()) {

                int craftingIndex =
                    slot - CRAFTING_SLOT_BASE;

                int craftingSize =
                    getCurrentCraftingSize();

                int x =
                    craftingIndex % craftingSize;

                int y =
                    craftingIndex / craftingSize;

                ItemStack stack =
                    craftingGrid[y][x];

                if (stack != null) {

                    dragging = true;
                    draggingFromCrafting = true;
                    draggedSlot = craftingIndex;
                    draggedStack = stack;
                    draggedStackIsSplit = false;

                    craftingGrid[y][x] = null;
                }

            } else if (slot >= 0) {

                ItemStack stack =
                    inventory.getSlot(slot);

                if (stack != null) {

                    dragging = true;
                    draggingFromCrafting = false;
                    draggedSlot = slot;
                    draggedStack = stack;
                    draggedStackIsSplit = false;

                    inventory.setSlot(
                        draggedSlot,
                        null
                    );
                }
            }
        }

        if (dragging) {

            dragX = Gdx.input.getX();

            dragY =
                Gdx.graphics.getHeight() -
                    Gdx.input.getY();

            boolean mouseReleased =
                leftMouseWasDown &&
                    !leftMouseDown;

            if (mouseReleased) {

                int targetSlot =
                    getSlotAt(
                        dragX,
                        dragY
                    );

                boolean placed = false;

                int craftingSize =
                    getCurrentCraftingSize();

                if (targetSlot >= CRAFTING_SLOT_BASE &&
                    targetSlot < CRAFTING_SLOT_BASE +
                        craftingSize * craftingSize) {

                    int craftingIndex =
                        targetSlot - CRAFTING_SLOT_BASE;


                    int x =
                        craftingIndex % craftingSize;

                    int y =
                        craftingIndex / craftingSize;

                    ItemStack targetStack =
                        craftingGrid[y][x];

                    if (targetStack == null) {

                        craftingGrid[y][x] =
                            draggedStack;

                        draggedStack = null;
                        placed = true;

                    } else if (targetStack.getItem() == draggedStack.getItem()) {

                        int space =
                            64 - targetStack.getAmount();

                        int moved =
                            Math.min(
                                space,
                                draggedStack.getAmount()
                            );

                        targetStack.add(moved);
                        draggedStack.remove(moved);

                        if (draggedStack.isEmpty()) {
                            draggedStack = null;
                            placed = true;
                        }

                    } else if (!draggedStackIsSplit) {

                        craftingGrid[y][x] =
                            draggedStack;

                        draggedStack = targetStack;
                        draggedStackIsSplit = false;
                        placed = true;

                    } else {
                        // A split stack cannot swap because the original
                        // stack is still in its source slot. Leave it held.
                    }

                } else if (targetSlot >= 0) {

                    ItemStack targetStack =
                        inventory.getSlot(
                            targetSlot
                        );

                    if (targetStack == null) {

                        inventory.setSlot(
                            targetSlot,
                            draggedStack
                        );

                        draggedStack = null;
                        placed = true;

                    } else if (targetStack.getItem() == draggedStack.getItem()) {

                        int space =
                            64 - targetStack.getAmount();

                        int moved =
                            Math.min(
                                space,
                                draggedStack.getAmount()
                            );

                        targetStack.add(moved);
                        draggedStack.remove(moved);

                        if (draggedStack.isEmpty()) {
                            draggedStack = null;
                            placed = true;
                        }

                    } else if (!draggedStackIsSplit) {

                        inventory.setSlot(
                            targetSlot,
                            draggedStack
                        );

                        draggedStack = targetStack;
                        draggedStackIsSplit = false;
                        placed = true;

                    } else {
                        // A split stack cannot swap because the original
                        // stack is still in its source slot. Leave it held.
                    }
                }

                if (!placed) {
                    if (draggedStack != null) {
                        restoreDraggedStack(
                            draggedStack
                        );
                    }

                    dragging = false;
                    draggingFromCrafting = false;
                    draggedSlot = -1;
                    draggedStack = null;
                    draggedStackIsSplit = false;

                } else if (draggedStack == null) {

                    dragging = false;
                    draggingFromCrafting = false;
                    draggedSlot = -1;
                    draggedStackIsSplit = false;
                }

                // If a same-item stack was full, keep the remaining
                // carried items attached to the mouse so they can be
                // placed into another slot.

            }
        }

        leftMouseWasDown = leftMouseDown;
    }


    private void startSplitDrag(
        ItemStack stack,
        boolean fromCrafting,
        int slot
    ) {

        // A single item cannot be split.
        if (stack == null || stack.getAmount() <= 1) {
            return;
        }

        int carriedAmount =
            stack.getAmount() / 2;

        int remainingAmount =
            stack.getAmount() - carriedAmount;

        // Keep the larger half in the original slot.
        stack.setAmount(remainingAmount);

        draggedStack =
            new ItemStack(
                stack.getItem(),
                carriedAmount
            );

        dragging = true;
        draggingFromCrafting = fromCrafting;
        draggedSlot = slot;
        draggedStackIsSplit = true;
    }


    private void restoreDraggedStack(ItemStack stack) {

        if (stack == null) {
            return;
        }

        if (draggedStackIsSplit) {

            // The original stack was never removed, so return the
            // carried half to that original slot.
            ItemStack originalStack;

            if (draggingFromCrafting) {
                int craftingSize =
                    getCurrentCraftingSize();

                int x =
                    draggedSlot % craftingSize;

                int y =
                    draggedSlot / craftingSize;
                originalStack = craftingGrid[y][x];

                if (originalStack == null) {
                    craftingGrid[y][x] = stack;
                } else if (originalStack.getItem() == stack.getItem()) {
                    originalStack.add(stack.getAmount());
                }
            } else {
                originalStack = inventory.getSlot(draggedSlot);

                if (originalStack == null) {
                    inventory.setSlot(draggedSlot, stack);
                } else if (originalStack.getItem() == stack.getItem()) {
                    originalStack.add(stack.getAmount());
                }
            }

            return;
        }

        if (draggingFromCrafting) {

            int craftingSize =
                getCurrentCraftingSize();

            int x =
                draggedSlot % craftingSize;

            int y =
                draggedSlot / craftingSize;

            craftingGrid[y][x] = stack;

        } else {

            inventory.setSlot(
                draggedSlot,
                stack
            );
        }
    }


    private void craftItem() {

        int craftingSize =
            getCurrentCraftingSize();

        Recipe recipe =
            RecipeManager.findRecipe(
                craftingGrid,
                craftingSize,
                craftingSize
            );

        if (recipe == null) {
            return;
        }

        if (!canAddItem(
            recipe.getOutput(),
            recipe.getOutputAmount()
        )) {
            return;
        }

        for (int y = 0; y < craftingSize; y++) {
            for (int x = 0; x < craftingSize; x++) {

                ItemStack stack =
                    craftingGrid[y][x];

                if (stack == null) {
                    continue;
                }

                stack.remove(1);

                if (stack.isEmpty()) {
                    craftingGrid[y][x] = null;
                }
            }
        }

        inventory.addItem(
            recipe.getOutput(),
            recipe.getOutputAmount()
        );
    }


    private boolean canAddItem(
        Item item,
        int amount
    ) {

        int remaining = amount;

        for (int i = 0; i < Inventory.SIZE; i++) {

            ItemStack stack =
                inventory.getSlot(i);

            if (stack != null &&
                stack.getItem() == item &&
                stack.getAmount() < 64) {

                remaining -=
                    64 - stack.getAmount();

                if (remaining <= 0) {
                    return true;
                }
            }
        }

        for (int i = 0; i < Inventory.SIZE; i++) {

            if (inventory.getSlot(i) == null) {

                remaining -= 64;

                if (remaining <= 0) {
                    return true;
                }
            }
        }

        return false;
    }


    // =============================================================
    // GET SLOT UNDER MOUSE
    // =============================================================

    private int getSlotAt(
        float mouseX,
        float mouseY
    ) {

        float scale =
            getUIScale();

        float slotSize =
            SLOT_SIZE * scale;

        float slotGap =
            SLOT_GAP * scale;

        float totalSlotSize =
            TOTAL_SLOT_SIZE * scale;

        int screenWidth =
            Gdx.graphics.getWidth();

        int screenHeight =
            Gdx.graphics.getHeight();

        int inventoryRows = 3;

        float inventoryWidth =
            COLUMNS * slotSize +
                (COLUMNS - 1) * slotGap;

        float inventoryHeight =
            inventoryRows * slotSize +
                (inventoryRows - 1) * slotGap;

        float startX =
            (screenWidth - inventoryWidth) / 2f;

        float hotbarHeight =
            slotSize +
                UI_PADDING * 2f;

        float bottomArea =
            HOTBAR_BOTTOM * scale +
                hotbarHeight +
                20f * scale;

        float availableHeight =
            screenHeight -
                bottomArea;

        float startY =
            (availableHeight -
                inventoryHeight) / 2f;

        startY += 50f * scale;

        if (startY < 10f * scale) {
            startY = 10f * scale;
        }

        // =========================================================
        // CRAFTING GRID
        // =========================================================

        float craftingSlotSize =
            slotSize * 0.75f;

        int craftingSize =
            getCurrentCraftingSize();

        float craftingWidth =
            craftingSize * craftingSlotSize +
                (craftingSize - 1) * slotGap;

        // Put the crafting grid above the inventory, centered
        // with the output slot to its right.
        float craftingTotalWidth =
            craftingWidth +
                65f * scale +
                craftingSlotSize;

        float craftingStartX =
            startX +
                (inventoryWidth - craftingTotalWidth) / 2f;

        float craftingStartY =
            startY +
                inventoryHeight +
                45f * scale;

        for (int y = 0; y < craftingSize; y++) {
            for (int x = 0; x < craftingSize; x++) {

                float slotX =
                    craftingStartX +
                        x *
                            (craftingSlotSize +
                                slotGap);

                float slotY =
                    craftingStartY +
                        (craftingSize - 1 - y) *
                            (craftingSlotSize +
                                slotGap);

                if (mouseX >= slotX &&
                    mouseX <= slotX + craftingSlotSize &&
                    mouseY >= slotY &&
                    mouseY <= slotY + craftingSlotSize) {

                    return CRAFTING_SLOT_BASE +
                        y * craftingSize +
                        x;
                }
            }
        }

        // Crafting output
        float outputSize =
            craftingSlotSize;

        float outputX =
            craftingStartX +
                craftingWidth +
                65f * scale;

        float outputY =
            craftingStartY +
                craftingSlotSize / 2f +
                slotGap / 2f;

        if (mouseX >= outputX &&
            mouseX <= outputX + outputSize &&
            mouseY >= outputY &&
            mouseY <= outputY + outputSize) {

            return CRAFTING_OUTPUT_SLOT;
        }

        // =========================================================
        // MAIN INVENTORY
        // =========================================================

        if (mouseX >= startX &&
            mouseX <= startX + inventoryWidth &&
            mouseY >= startY &&
            mouseY <= startY + inventoryHeight) {

            int column =
                (int)((mouseX - startX) /
                    totalSlotSize);

            int rowFromBottom =
                (int)((mouseY - startY) /
                    totalSlotSize);

            if (column >= 0 &&
                column < COLUMNS &&
                rowFromBottom >= 0 &&
                rowFromBottom < inventoryRows) {

                int visualRow =
                    inventoryRows -
                        1 -
                        rowFromBottom;

                int inventorySlot =
                    visualRow * COLUMNS +
                        column;

                return 9 + inventorySlot;
            }
        }

        // =========================================================
        // HOTBAR
        // =========================================================

        float hotbarWidth =
            COLUMNS * slotSize +
                (COLUMNS - 1) * slotGap;

        float hotbarStartX =
            (screenWidth - hotbarWidth) / 2f;

        float hotbarStartY =
            HOTBAR_BOTTOM * scale;

        if (mouseX >= hotbarStartX &&
            mouseX <= hotbarStartX + hotbarWidth &&
            mouseY >= hotbarStartY &&
            mouseY <= hotbarStartY + slotSize) {

            int column =
                (int)((mouseX - hotbarStartX) /
                    totalSlotSize);

            if (column >= 0 &&
                column < COLUMNS) {

                return column;
            }
        }

        return -1;
    }


    // =============================================================
    // CANCEL DRAG
    // =============================================================

    private void cancelDrag() {

        if (!dragging) {
            return;
        }

        restoreDraggedStack(
            draggedStack
        );

        dragging = false;
        draggingFromCrafting = false;
        draggedSlot = -1;
        draggedStack = null;
        draggedStackIsSplit = false;
    }


    // =============================================================
    // UI SCALE
    // =============================================================

    private float getUIScale() {

        float screenWidth =
            Gdx.graphics.getWidth();

        float screenHeight =
            Gdx.graphics.getHeight();


        float hotbarWidth =
            COLUMNS * SLOT_SIZE +
                (COLUMNS - 1) * SLOT_GAP;


        float requiredWidth =
            hotbarWidth +
                UI_PADDING * 2f;


        float inventoryHeight =
            3f * SLOT_SIZE +
                2f * SLOT_GAP;


        int craftingSize =
            getCurrentCraftingSize();

        float craftingHeight =
            craftingSize * (SLOT_SIZE * 0.75f) +
                (craftingSize - 1) * SLOT_GAP;

        float requiredHeight =
            inventoryHeight +
                craftingHeight +
                140f;


        float widthScale =
            screenWidth / requiredWidth;


        float heightScale =
            screenHeight / requiredHeight;


        float scale =
            Math.min(
                widthScale,
                heightScale
            );


        // Don't make the UI larger than its normal size
        scale = Math.min(
            scale,
            1f
        );


        return scale;
    }


    // =============================================================
    // RENDER
    // =============================================================

    public void render() {

        int screenWidth =
            Gdx.graphics.getWidth();

        int screenHeight =
            Gdx.graphics.getHeight();


        float scale =
            getUIScale();


        // =========================================================
        // RESET 2D RENDERING STATE
        // =========================================================

        batch.getTransformMatrix().idt();

        shapeRenderer.getTransformMatrix().idt();


        batch.getProjectionMatrix().setToOrtho2D(
            0f,
            0f,
            screenWidth,
            screenHeight
        );


        shapeRenderer.setProjectionMatrix(
            batch.getProjectionMatrix()
        );


        // =========================================================
        // HOTBAR
        // =========================================================

        renderHotbar(
            screenWidth,
            screenHeight,
            scale
        );


        // =========================================================
        // INVENTORY
        // =========================================================

        if (!inventoryOpen) {
            return;
        }


        renderInventory(
            screenWidth,
            screenHeight,
            scale
        );


        // =========================================================
        // DRAGGED ITEM
        // =========================================================

        if (dragging &&
            draggedStack != null) {

            batch.begin();

            drawDraggedItem(
                dragX,
                dragY,
                scale
            );

            batch.end();
        }
    }


    // =============================================================
    // RENDER INVENTORY
    // =============================================================

    private void renderInventory(
        int screenWidth,
        int screenHeight,
        float scale
    ) {

        int craftingSize =
            getCurrentCraftingSize();

        currentRecipe =
            RecipeManager.findRecipe(
                craftingGrid,
                craftingSize,
                craftingSize
            );

        float slotSize =
            SLOT_SIZE * scale;

        float slotGap =
            SLOT_GAP * scale;

        float totalSlotSize =
            TOTAL_SLOT_SIZE * scale;

        int inventoryRows = 3;

        float inventoryWidth =
            COLUMNS * slotSize +
                (COLUMNS - 1) * slotGap;

        float inventoryHeight =
            inventoryRows * slotSize +
                (inventoryRows - 1) * slotGap;

        float startX =
            (screenWidth - inventoryWidth) / 2f;

        float hotbarHeight =
            slotSize +
                UI_PADDING * 2f;

        float bottomArea =
            HOTBAR_BOTTOM * scale +
                hotbarHeight +
                20f * scale;

        float availableHeight =
            screenHeight -
                bottomArea;

        float startY =
            (availableHeight -
                inventoryHeight) / 2f;

        startY += 50f * scale;

        if (startY < 10f * scale) {
            startY = 10f * scale;
        }

        // =========================================================
        // CRAFTING POSITION
        // =========================================================

        float craftingSlotSize =
            slotSize * 0.75f;

        float craftingWidth =
            craftingSize * craftingSlotSize +
                (craftingSize - 1) * slotGap;

        // Put the crafting grid above the inventory, centered
        // with the output slot to its right.
        float craftingTotalWidth =
            craftingWidth +
                65f * scale +
                craftingSlotSize;

        float craftingStartX =
            startX +
                (inventoryWidth - craftingTotalWidth) / 2f;

        float craftingStartY =
            startY +
                inventoryHeight +
                45f * scale;

        float outputSize =
            craftingSlotSize;

        float outputX =
            craftingStartX +
                craftingWidth +
                65f * scale;

        float outputY =
            craftingStartY +
                craftingSlotSize / 2f +
                slotGap / 2f;

        // =========================================================
        // BACKGROUND
        // =========================================================

        Gdx.gl.glEnable(
            GL20.GL_BLEND
        );

        shapeRenderer.begin(
            ShapeRenderer.ShapeType.Filled
        );

        shapeRenderer.setColor(
            0f,
            0f,
            0f,
            0.75f
        );

        // The background must surround BOTH the inventory and the
        // crafting area, including the output slot.
        float craftingRight =
            outputX +
                outputSize;

        float craftingTop =
            craftingStartY +
                craftingSize * craftingSlotSize +
                (craftingSize - 1) * slotGap;

        float left =
            Math.min(startX, craftingStartX) -
                UI_PADDING * scale;

        float right =
            Math.max(
                startX + inventoryWidth,
                craftingRight
            ) +
                UI_PADDING * scale;

        float bottom =
            startY -
                UI_PADDING * scale;

        float top =
            Math.max(
                craftingTop,
                outputY + outputSize
            ) +
                UI_PADDING * scale;

        shapeRenderer.rect(
            left,
            bottom,
            right - left,
            top - bottom
        );

        shapeRenderer.end();

        // =========================================================
        // CRAFTING GRID + OUTPUT
        // =========================================================

        batch.begin();

        for (int y = 0; y < craftingSize; y++) {
            for (int x = 0; x < craftingSize; x++) {

                float slotX =
                    craftingStartX +
                        x *
                            (craftingSlotSize +
                                slotGap);

                float slotY =
                    craftingStartY +
                        (craftingSize - 1 - y) *
                            (craftingSlotSize +
                                slotGap);

                batch.draw(
                    slotTexture,
                    slotX,
                    slotY,
                    craftingSlotSize,
                    craftingSlotSize
                );

                ItemStack stack =
                    craftingGrid[y][x];

                if (stack != null) {
                    drawItemStackAtPosition(
                        stack,
                        slotX,
                        slotY,
                        craftingSlotSize,
                        scale
                    );
                }
            }
        }

        batch.draw(
            slotTexture,
            outputX,
            outputY,
            outputSize,
            outputSize
        );

        if (currentRecipe != null) {

            ItemStack outputStack =
                new ItemStack(
                    currentRecipe.getOutput(),
                    currentRecipe.getOutputAmount()
                );

            drawItemStackAtPosition(
                outputStack,
                outputX,
                outputY,
                outputSize,
                scale
            );
        }

        batch.end();

        // =========================================================
        // MAIN INVENTORY
        // =========================================================

        batch.begin();

        for (int slot = 9;
             slot < Inventory.SIZE;
             slot++) {

            int inventorySlot =
                slot - 9;

            int column =
                inventorySlot % COLUMNS;

            int row =
                inventorySlot / COLUMNS;

            float x =
                startX +
                    column * totalSlotSize;

            float y =
                startY +
                    (inventoryRows - 1 - row) *
                        totalSlotSize;

            batch.draw(
                slotTexture,
                x,
                y,
                slotSize,
                slotSize
            );
        }

        for (int slot = 9;
             slot < Inventory.SIZE;
             slot++) {

            if (dragging &&
                !draggingFromCrafting &&
                !draggedStackIsSplit &&
                slot == draggedSlot) {
                continue;
            }

            int inventorySlot =
                slot - 9;

            int column =
                inventorySlot % COLUMNS;

            int row =
                inventorySlot / COLUMNS;

            float x =
                startX +
                    column * totalSlotSize;

            float y =
                startY +
                    (inventoryRows - 1 - row) *
                        totalSlotSize;

            drawItemStackAt(
                slot,
                x,
                y,
                scale
            );
        }

        batch.end();
    }


    private void drawItemStackAtPosition(
        ItemStack stack,
        float x,
        float y,
        float slotSize,
        float scale
    ) {

        if (stack == null) {
            return;
        }

        int textureIndex =
            getItemTexture(
                stack.getItem()
            );

        if (textureIndex < 0) {
            return;
        }

        TextureRegion region =
            getTextureRegion(
                textureIndex
            );

        float itemSize =
            slotSize -
                ITEM_PADDING * 2f * scale;

        if (itemSize < 1f) {
            itemSize = slotSize;
        }

        float itemOffset =
            (slotSize - itemSize) / 2f;

        batch.draw(
            region,
            x + itemOffset,
            y + itemOffset,
            itemSize,
            itemSize
        );

        drawItemAmountAtSize(
            stack,
            x,
            y,
            slotSize,
            scale
        );
    }


    private void drawItemAmountAtSize(
        ItemStack stack,
        float x,
        float y,
        float slotSize,
        float scale
    ) {

        if (stack.getAmount() <= 1) {
            return;
        }

        String amount =
            String.valueOf(
                stack.getAmount()
            );

        float oldScale =
            font.getData().scaleX;

        font.getData().setScale(
            scale
        );

        GlyphLayout layout =
            new GlyphLayout(
                font,
                amount
            );

        font.draw(
            batch,
            amount,
            x +
                slotSize -
                layout.width -
                5f * scale,
            y +
                5f * scale +
                layout.height
        );

        font.getData().setScale(
            oldScale
        );
    }


    // =============================================================
    // DRAW INVENTORY ITEM
    // =============================================================

    private void drawItemStackAt(
        int slot,
        float x,
        float y,
        float scale
    ) {

        ItemStack stack =
            inventory.getSlot(slot);


        if (stack == null) {
            return;
        }


        Item item =
            stack.getItem();


        int textureIndex =
            getItemTexture(item);


        if (textureIndex < 0) {
            return;
        }


        TextureRegion region =
            getTextureRegion(
                textureIndex
            );


        float slotSize =
            SLOT_SIZE * scale;


        float itemSize =
            slotSize -
                ITEM_PADDING * 2f;

        float itemOffset =
            (slotSize - itemSize) / 2f;

        batch.draw(
            region,

            x + itemOffset,
            y + itemOffset,

            itemSize,
            itemSize
        );


        drawItemAmount(
            stack,
            x,
            y,
            scale
        );
    }


    // =============================================================
    // DRAW DRAGGED ITEM
    // =============================================================

    private void drawDraggedItem(
        float mouseX,
        float mouseY,
        float scale
    ) {

        Item item =
            draggedStack.getItem();


        int textureIndex =
            getItemTexture(item);


        if (textureIndex < 0) {
            return;
        }


        TextureRegion region =
            getTextureRegion(
                textureIndex
            );


        float slotSize =
            SLOT_SIZE * scale;


        float itemSize =
            slotSize -
                ITEM_PADDING * 2f;


        float itemOffset =
            (slotSize - itemSize) / 2f;


        // Center the item on the cursor
        float x =
            mouseX -
                slotSize / 2f +
                itemOffset;


        float y =
            mouseY -
                slotSize / 2f +
                itemOffset;


        batch.draw(
            region,
            x,
            y,
            itemSize,
            itemSize
        );


        drawItemAmount(
            draggedStack,

            mouseX -
                slotSize / 2f,

            mouseY -
                slotSize / 2f,

            scale
        );
    }


    // =============================================================
    // HOTBAR
    // =============================================================

    private void renderHotbar(
        int screenWidth,
        int screenHeight,
        float scale
    ) {

        float slotSize =
            SLOT_SIZE * scale;

        float slotGap =
            SLOT_GAP * scale;

        float totalSlotSize =
            TOTAL_SLOT_SIZE * scale;


        float hotbarWidth =
            COLUMNS * slotSize +
                (COLUMNS - 1) * slotGap;


        float startX =
            (screenWidth - hotbarWidth) / 2f;


        float startY =
            HOTBAR_BOTTOM * scale;


        // =========================================================
        // HOTBAR BACKGROUND
        // =========================================================

        Gdx.gl.glEnable(
            GL20.GL_BLEND
        );


        shapeRenderer.begin(
            ShapeRenderer.ShapeType.Filled
        );


        shapeRenderer.setColor(
            0f,
            0f,
            0f,
            0.75f
        );


        shapeRenderer.rect(
            startX -
                UI_PADDING * scale,

            startY -
                UI_PADDING * scale,

            hotbarWidth +
                UI_PADDING * 2f * scale,

            slotSize +
                UI_PADDING * 2f * scale
        );


        shapeRenderer.end();


        // =========================================================
        // HOTBAR SLOTS + ITEMS
        // =========================================================

        batch.begin();


        // ---------------------------------------------------------
        // Slot textures
        // ---------------------------------------------------------

        for (int slot = 0;
             slot < COLUMNS;
             slot++) {

            float x =
                startX +
                    slot * totalSlotSize;


            batch.draw(
                slotTexture,
                x,
                startY,
                slotSize,
                slotSize
            );


            // -----------------------------------------------------
            // Selected slot overlay
            // -----------------------------------------------------

            if (slot == selectedHotbarSlot) {

                batch.draw(
                    selectedSlotTexture,
                    x,
                    startY,
                    slotSize,
                    slotSize
                );
            }
        }


        // ---------------------------------------------------------
        // Item textures
        // ---------------------------------------------------------

        for (int slot = 0;
             slot < COLUMNS;
             slot++) {

            // Don't draw the stack while dragging it
            if (dragging &&
                !draggedStackIsSplit &&
                slot == draggedSlot) {

                continue;
            }


            float x =
                startX +
                    slot * totalSlotSize;


            drawHotbarItem(
                slot,
                x,
                startY,
                scale
            );
        }


        batch.end();
    }


    // =============================================================
    // DRAW HOTBAR ITEM
    // =============================================================

    private void drawHotbarItem(
        int slot,
        float x,
        float y,
        float scale
    ) {

        ItemStack stack =
            inventory.getSlot(slot);


        if (stack == null) {
            return;
        }


        Item item =
            stack.getItem();


        int textureIndex =
            getItemTexture(item);


        if (textureIndex < 0) {
            return;
        }


        TextureRegion region =
            getTextureRegion(
                textureIndex
            );


        float slotSize =
            SLOT_SIZE * scale;


        float itemSize =
            slotSize -
                ITEM_PADDING * 2f;

        float itemOffset =
            (slotSize - itemSize) / 2f;


        batch.draw(
            region,

            x + itemOffset,
            y + itemOffset,

            itemSize,
            itemSize
        );


        drawItemAmount(
            stack,
            x,
            y,
            scale
        );
    }


    // =============================================================
    // ITEM COUNT
    // =============================================================

    private void drawItemAmount(
        ItemStack stack,
        float x,
        float y,
        float scale
    ) {

        if (stack.getAmount() <= 1) {
            return;
        }


        String amount =
            String.valueOf(
                stack.getAmount()
            );


        float oldScale =
            font.getData().scaleX;


        font.getData().setScale(
            scale
        );


        GlyphLayout layout =
            new GlyphLayout(
                font,
                amount
            );


        float slotSize =
            SLOT_SIZE * scale;


        font.draw(
            batch,
            amount,

            x +
                slotSize -
                layout.width -
                5f * scale,

            y +
                5f * scale +
                layout.height
        );


        font.getData().setScale(
            oldScale
        );
    }


    // =============================================================
    // ITEM TEXTURE
    // =============================================================

    private int getItemTexture(
        Item item
    ) {

        if (item == Item.GRASS) {

            return Block.getFrontTexture(
                Block.GRASS
            );
        }


        if (item == Item.DIRT) {

            return Block.getFrontTexture(
                Block.DIRT
            );
        }


        if (item == Item.STONE) {

            return Block.getFrontTexture(
                Block.STONE
            );
        }


        if (item == Item.WOOD) {

            return Block.getFrontTexture(
                Block.WOOD
            );
        }


        if (item == Item.LEAVES) {

            return Block.getFrontTexture(
                Block.LEAVES
            );
        }

        if (item == Item.PLANKS) {
            return item.getTextureIndex();
        }

        if (item == Item.STICK) {
            return item.getTextureIndex();
        }
        if (item == Item.CRAFTING_TABLE) {
            return Block.getTopTexture(Block.CRAFTING_TABLE);
        }
        if (item == Item.WOODEN_PICKAXE) {
            return item.getTextureIndex();
        }
        if (item == Item.STONE_PICKAXE) {
            return item.getTextureIndex();
        }


        return -1;
    }


    // =============================================================
    // TEXTURE REGION
    // =============================================================

    private TextureRegion getTextureRegion(
        int textureIndex
    ) {

        int atlasSize = 4;


        int tileX =
            textureIndex % atlasSize;


        int tileY =
            textureIndex / atlasSize;


        int textureSize =
            textureAtlas.getWidth() /
                atlasSize;


        int pixelY =
            textureAtlas.getHeight() -
                (tileY + 1) *
                    textureSize;


        TextureRegion region =
            new TextureRegion(
                textureAtlas,

                tileX *
                    textureSize,

                pixelY,

                textureSize,
                textureSize
            );


        region.flip(
            false,
            true
        );


        return region;
    }


    // =============================================================
    // INVENTORY STATE
    // =============================================================

    public boolean isOpen() {

        return inventoryOpen;
    }

    public void openCraftingTable() {

        inventoryOpen = true;
        craftingTableOpen = true;

        Gdx.input.setCursorCatched(false);
    }

    public boolean isCraftingTableOpen() {
        return craftingTableOpen;
    }


    // =============================================================
    // SELECTED HOTBAR SLOT
    // =============================================================

    public int getSelectedHotbarSlot() {

        return selectedHotbarSlot;
    }


    // =============================================================
    // DISPOSE
    // =============================================================

    public void dispose() {

        batch.dispose();

        shapeRenderer.dispose();

        font.dispose();

        textureAtlas.dispose();

        slotTexture.dispose();

        selectedSlotTexture.dispose();
    }
}

