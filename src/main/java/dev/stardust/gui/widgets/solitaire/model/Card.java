package dev.stardust.gui.widgets.solitaire.model;

public class Card {
    public Rank rank;
    public Suit suit;
    public boolean faceUp;

    public Card(Rank r, Suit s) {
        this.rank = r;
        this.suit = s;
    }

    @Override public String toString() {
        // 26.1: Yarn StringIdentifiable#asString -> 官方 StringRepresentable#getSerializedName
        return rank.getSerializedName() + suit.getSerializedName();
    }
}
