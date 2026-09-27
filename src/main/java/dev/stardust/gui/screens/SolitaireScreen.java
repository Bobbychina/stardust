package dev.stardust.gui.screens;

import dev.stardust.modules.Solitaire;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import dev.stardust.gui.widgets.solitaire.WSolitaire;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class SolitaireScreen extends WindowScreen {
    public SolitaireScreen(Solitaire module, GuiTheme theme, String title) {
        super(theme, title);
        this.module = module;
    }

    private final Solitaire module;
    private Cell<? extends WWidget> widget = null;

    public Cell<? extends WWidget> getWidget() {
        return widget;
    }

    @Override
    public void initWidgets() {
        this.widget = add(new WSolitaire(module, GuiThemes.get()));
    }

    @Override
    public void onClosed() {
        if (module.isActive()) module.toggle();
        if (widget != null && widget.widget() instanceof WSolitaire solitaire) {
            if (solitaire.dragging) solitaire.cancelDragReturn();
            if (solitaire.shouldSaveGame()) module.saveGame(solitaire.saveGame());
        }
    }
}
