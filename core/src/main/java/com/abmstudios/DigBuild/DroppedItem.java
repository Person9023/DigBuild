package com.abmstudios.DigBuild;

public class DroppedItem {

    // ---------------------------------------------------------
    // Render type
    // ---------------------------------------------------------

    public enum RenderType {
        BLOCK,
        FLAT
    }

    private ItemStack stack;

    private float x;
    private float y;
    private float z;

    private float pickupCooldown = 0.5f;

    private static final float PICKUP_DELAY = 0.5f;

    private float animationTime = 0f;

    private static final float FLOAT_HEIGHT = 0.25f;
    private static final float ROTATION_SPEED = 90f;

    private static final float GRAVITY = 15f;
    private static final float TERMINAL_VELOCITY = 20f;

    private float velocityY = 0f;

    private float velocityX = 0f;
    private float velocityZ = 0f;

    private float throwTime = 0f;

    private static final float THROW_DURATION = 0.5f;

    // ---------------------------------------------------------
    // Render type
    // ---------------------------------------------------------

    private RenderType renderType;

    // ---------------------------------------------------------
    // Pickup
    // ---------------------------------------------------------

    public boolean canBePickedUp() {
        return pickupCooldown <= 0f;
    }

    // ---------------------------------------------------------
    // Update
    // ---------------------------------------------------------

    public void update(float delta, World world) {

        animationTime += delta;

        if (pickupCooldown > 0f) {
            pickupCooldown -= delta;
        }

        // ---------------------------------------------------------
        // Throw movement
        // ---------------------------------------------------------

        if (throwTime < THROW_DURATION) {

            throwTime += delta;

            x += velocityX * delta;
            z += velocityZ * delta;
        }

        // ---------------------------------------------------------
        // Gravity
        // ---------------------------------------------------------

        velocityY -= GRAVITY * delta;

        if (velocityY < -TERMINAL_VELOCITY) {
            velocityY = -TERMINAL_VELOCITY;
        }

        float newY = y + velocityY * delta;

        int blockX = (int)Math.floor(x);
        int blockY = (int)Math.floor(newY - 0.1f);
        int blockZ = (int)Math.floor(z);

        byte block = world.getBlock(
            blockX,
            blockY,
            blockZ
        );

        if (block != Block.AIR) {

            y = blockY + 1f;

            velocityY = 0f;

        } else {

            y = newY;
        }
    }

    // ---------------------------------------------------------
    // Animation
    // ---------------------------------------------------------

    public float getFloatOffset() {

        return (float)Math.sin(animationTime * 2.0f)
            * FLOAT_HEIGHT;
    }

    public float getRotation() {

        return animationTime * ROTATION_SPEED;
    }

    // ---------------------------------------------------------
    // Normal constructor
    // ---------------------------------------------------------

    public DroppedItem(
        Item item,
        int amount,
        float x,
        float y,
        float z,
        float velocityX,
        float velocityZ
    ) {

        this.stack = new ItemStack(item, amount);

        this.x = x;
        this.y = y;
        this.z = z;

        this.velocityX = velocityX;
        this.velocityZ = velocityZ;

        this.pickupCooldown = PICKUP_DELAY;

        this.renderType = getDefaultRenderType(item);
    }

    // ---------------------------------------------------------
    // Constructor with explicit render type
    // ---------------------------------------------------------

    public DroppedItem(
        Item item,
        int amount,
        float x,
        float y,
        float z,
        float velocityX,
        float velocityZ,
        RenderType renderType
    ) {

        this.stack = new ItemStack(item, amount);

        this.x = x;
        this.y = y;
        this.z = z;

        this.velocityX = velocityX;
        this.velocityZ = velocityZ;

        this.pickupCooldown = PICKUP_DELAY;

        this.renderType = renderType;
    }

    // ---------------------------------------------------------
    // Constructor used when loading from a save file
    // ---------------------------------------------------------

    public DroppedItem(
        Item item,
        int amount,
        float x,
        float y,
        float z,
        float velocityX,
        float velocityZ,
        float velocityY,
        float pickupCooldown,
        float animationTime,
        float throwTime
    ) {

        this.stack = new ItemStack(item, amount);

        this.x = x;
        this.y = y;
        this.z = z;

        this.velocityX = velocityX;
        this.velocityZ = velocityZ;
        this.velocityY = velocityY;

        this.pickupCooldown = pickupCooldown;
        this.animationTime = animationTime;
        this.throwTime = throwTime;

        this.renderType = getDefaultRenderType(item);
    }

    // ---------------------------------------------------------
    // Determine default render type
    // ---------------------------------------------------------

    private static RenderType getDefaultRenderType(Item item) {

        if (item == Item.GRASS) {
            return RenderType.BLOCK;
        }

        if (item == Item.DIRT) {
            return RenderType.BLOCK;
        }

        if (item == Item.STONE) {
            return RenderType.BLOCK;
        }

        if (item == Item.WOOD) {
            return RenderType.BLOCK;
        }

        if (item == Item.LEAVES) {
            return RenderType.BLOCK;
        }

        if (item == Item.PLANKS) {
            return RenderType.BLOCK;
        }

        if (item == Item.CRAFTING_TABLE) {
            return RenderType.BLOCK;
        }
        if (item == Item.COAL_ORE) {
            return RenderType.BLOCK;
        }

        // Sticks and future tools/weapons
        // will be rendered as flat/extruded items.
        return RenderType.FLAT;
    }

    // ---------------------------------------------------------
    // Getters
    // ---------------------------------------------------------

    public ItemStack getStack() {
        return stack;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public RenderType getRenderType() {
        return renderType;
    }

    public void setRenderType(RenderType renderType) {
        this.renderType = renderType;
    }

    public void setPosition(
        float x,
        float y,
        float z
    ) {

        this.x = x;
        this.y = y;
        this.z = z;
    }

    // ---------------------------------------------------------
    // Save-state getters
    // ---------------------------------------------------------

    public float getVelocityX() {
        return velocityX;
    }

    public float getVelocityY() {
        return velocityY;
    }

    public float getVelocityZ() {
        return velocityZ;
    }

    public float getPickupCooldown() {
        return pickupCooldown;
    }

    public float getAnimationTime() {
        return animationTime;
    }

    public float getThrowTime() {
        return throwTime;
    }
}
