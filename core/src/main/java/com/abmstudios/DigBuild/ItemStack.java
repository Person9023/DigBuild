
package com.abmstudios.DigBuild;

public class ItemStack {

    private Item item;
    private int amount;


    public ItemStack(Item item, int amount) {
        this.item = item;
        this.amount = amount;
    }


    public Item getItem() {
        return item;
    }


    public int getAmount() {
        return amount;
    }


    public void setAmount(int amount) {
        this.amount = amount;
    }


    public void add(int amount) {
        this.amount += amount;
    }


    public void remove(int amount) {
        this.amount -= amount;

        if (this.amount < 0) {
            this.amount = 0;
        }
    }


    public boolean isEmpty() {
        return amount <= 0;
    }
}

