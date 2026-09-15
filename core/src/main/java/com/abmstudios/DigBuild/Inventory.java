package com.abmstudios.DigBuild;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Inventory {

    public static final int SIZE = 36;

    private static final int SAVE_VERSION = 1;

    private final ItemStack[] slots;

    public Inventory() {
        slots = new ItemStack[SIZE];
    }


    // ---------------------------------------------------------
    // Get / set slots
    // ---------------------------------------------------------

    public ItemStack getSlot(int slot) {

        if (slot < 0 || slot >= SIZE) {
            return null;
        }

        return slots[slot];
    }


    public void setSlot(int slot, ItemStack stack) {

        if (slot < 0 || slot >= SIZE) {
            return;
        }

        slots[slot] = stack;
    }


    // ---------------------------------------------------------
    // Add items to the inventory
    // ---------------------------------------------------------

    public boolean addItem(Item item, int amount) {

        // First try to add to an existing stack
        for (int i = 0; i < SIZE; i++) {

            ItemStack stack = slots[i];

            if (stack != null &&
                stack.getItem() == item &&
                stack.getAmount() < 64) {

                int space = 64 - stack.getAmount();

                int amountToAdd =
                    Math.min(amount, space);

                stack.add(amountToAdd);

                amount -= amountToAdd;

                if (amount <= 0) {
                    return true;
                }
            }
        }


        // Then look for an empty slot
        for (int i = 0; i < SIZE; i++) {

            if (slots[i] == null) {

                int amountToAdd =
                    Math.min(amount, 64);

                slots[i] =
                    new ItemStack(
                        item,
                        amountToAdd
                    );

                amount -= amountToAdd;

                if (amount <= 0) {
                    return true;
                }
            }
        }


        // Inventory was full
        return false;
    }


    // ---------------------------------------------------------
    // Remove items from a slot
    // ---------------------------------------------------------

    public boolean removeItem(int slot, int amount) {

        if (slot < 0 || slot >= SIZE) {
            return false;
        }

        ItemStack stack = slots[slot];

        if (stack == null) {
            return false;
        }

        if (stack.getAmount() < amount) {
            return false;
        }

        stack.remove(amount);

        if (stack.isEmpty()) {
            slots[slot] = null;
        }

        return true;
    }


    // ---------------------------------------------------------
    // Check if inventory is full
    // ---------------------------------------------------------

    public boolean isFull() {

        for (int i = 0; i < SIZE; i++) {

            ItemStack stack = slots[i];

            if (stack == null) {
                return false;
            }

            if (stack.getAmount() < 64) {
                return false;
            }
        }

        return true;
    }


    // =========================================================
    // SAVE / LOAD
    // =========================================================

    private FileHandle getSaveFile() {

        FileHandle saveDirectory =
            Gdx.files.local(
                "DigBuild/saves/world"
            );

        saveDirectory.mkdirs();

        return saveDirectory.child(
            "inventory.dat"
        );
    }


    // ---------------------------------------------------------
    // Save inventory
    // ---------------------------------------------------------

    public void save() {

        FileHandle file = getSaveFile();

        try (
            DataOutputStream out =
                new DataOutputStream(
                    new BufferedOutputStream(
                        file.write(false)
                    )
                )
        ) {

            // Save version
            out.writeInt(SAVE_VERSION);

            // Number of slots
            out.writeInt(SIZE);

            // Save every slot
            for (int i = 0; i < SIZE; i++) {

                ItemStack stack = slots[i];

                // Empty slot
                if (stack == null) {

                    out.writeBoolean(false);

                    continue;
                }

                // Occupied slot
                out.writeBoolean(true);

                // Item ID
                out.writeUTF(
                    stack.getItem().getId()
                );

                // Amount
                out.writeInt(
                    stack.getAmount()
                );
            }

        } catch (IOException e) {

            Gdx.app.error(
                "Inventory",
                "Failed to save inventory",
                e
            );
        }
    }


    // ---------------------------------------------------------
    // Load inventory
    // ---------------------------------------------------------

    public void load() {

        FileHandle file = getSaveFile();

        if (!file.exists()) {
            return;
        }

        try (
            DataInputStream in =
                new DataInputStream(
                    new BufferedInputStream(
                        file.read()
                    )
                )
        ) {

            int version =
                in.readInt();

            if (version != SAVE_VERSION) {

                throw new IOException(
                    "Unsupported inventory save version: "
                        + version
                );
            }

            int savedSize =
                in.readInt();

            /*
             * Clear the current inventory first.
             */
            for (int i = 0; i < SIZE; i++) {
                slots[i] = null;
            }


            /*
             * Read the saved slots.
             *
             * We only load up to SIZE so a future version
             * with more slots will not break this version.
             */
            for (int i = 0; i < savedSize; i++) {

                boolean occupied =
                    in.readBoolean();

                if (!occupied) {
                    continue;
                }

                String itemId =
                    in.readUTF();

                int amount =
                    in.readInt();

                Item item =
                    Item.fromId(itemId);

                /*
                 * Unknown items are skipped instead of
                 * crashing the entire save.
                 */
                if (item == null) {
                    continue;
                }

                /*
                 * Don't allow invalid amounts into the
                 * inventory.
                 */
                if (amount <= 0) {
                    continue;
                }

                amount =
                    Math.min(amount, 64);

                if (i < SIZE) {

                    slots[i] =
                        new ItemStack(
                            item,
                            amount
                        );
                }
            }

        } catch (Exception e) {

            Gdx.app.error(
                "Inventory",
                "Failed to load inventory",
                e
            );
        }
    }
}
