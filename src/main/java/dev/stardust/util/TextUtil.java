package dev.stardust.util;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import net.minecraft.network.chat.*;
import java.util.function.UnaryOperator;

public class TextUtil {
    // See ChatHudMixin.java && EntityRendererMixin.java
    public static Component modifyWithStyle(Component original, UnaryOperator<String> modifier) {
        List<StyledChar> chars = new ArrayList<>();
        collectStyledChars(original, original.getStyle(), chars);

        String modifiedContent = modifier.apply(original.getString());

        List<StyledChar> modifiedChars = new ArrayList<>();
        for (int n = 0; n < chars.size(); n++) {
            modifiedChars.add(new StyledChar(modifiedContent.charAt(n), chars.get(n).style));
        }

        return rebuildFromStyledChars(modifiedChars);
    }

    private static void collectStyledChars(Component original, Style inherited, List<StyledChar> out) {
        Style style = original.getStyle().withParent(inherited);
        String content = original.getContent().visit(Optional::of).orElse("");

        for (char c : content.toCharArray()) {
            out.add(new StyledChar(c, style));
        }

        for (Component sibling : original.getSiblings()) {
            collectStyledChars(sibling, style, out);
        }
    }

    private static Component rebuildFromStyledChars(List<StyledChar> chars) {
        if (chars.isEmpty()) return Component.literal("");

        MutableComponent root = null;
        StringBuilder sb = new StringBuilder();
        Style currentStyle = chars.getFirst().style();

        for (StyledChar sc : chars) {
            if (!sc.style.equals(currentStyle)) {
                if (!sb.isEmpty()) {
                    MutableComponent chunk = Component.literal(sb.toString()).setStyle(currentStyle);
                    if (root == null) root = chunk;
                    else root.append(chunk);
                    sb.setLength(0);
                }
                currentStyle = sc.style();
            }
            sb.append(sc.c());
        }

        if (!sb.isEmpty()) {
            MutableComponent chunk = Component.literal(sb.toString()).setStyle(currentStyle);
            if (root == null) root = chunk;
            else root.append(chunk);
        }

        return root;
    }

    record StyledChar(char c, Style style) {}
}
