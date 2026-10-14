package net.tfminecraft.codex.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class OtherUtilsTest {
    @Test
    void joinsNamesForAHoverLine() {
        assertEquals("Aldric", OtherUtils.joinNames(List.of("Aldric")));
        assertEquals("Aldric and Mira", OtherUtils.joinNames(List.of("Aldric", "Mira")));
        assertEquals("Aldric, Mira and Hazel", OtherUtils.joinNames(List.of("Aldric", "Mira", "Hazel")));
    }
}
