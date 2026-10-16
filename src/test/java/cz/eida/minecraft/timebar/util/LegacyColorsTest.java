package cz.eida.minecraft.timebar.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyColorsTest {

    @Test
    void legacyCodesBecomeTags() {
        assertEquals("Léto", LegacyColors.toMiniMessage("Léto"));
        assertEquals("<yellow><b>Léto", LegacyColors.toMiniMessage("&e&lLéto"));
        assertEquals("<aqua>Zima<reset>", LegacyColors.toMiniMessage("§bZima§r"));
        assertEquals("<#ff8800>Podzim", LegacyColors.toMiniMessage("&#FF8800Podzim"));
        assertEquals("& more", LegacyColors.toMiniMessage("& more"));
    }
}
