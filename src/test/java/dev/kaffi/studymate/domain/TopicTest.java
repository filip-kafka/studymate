package dev.kaffi.studymate.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TopicTest {

    private static final int MAX_LENGTH = 255;

    // ====================
    // validation
    // ====================

    @Test
    @DisplayName("A null topic is rejected")
    void constructor_rejectsNullValue() {
        assertThrows(NullPointerException.class, () -> new Topic(null));
    }

    @ParameterizedTest(name = "value = \"{0}\"")
    @ValueSource(strings = { "", " ", "   ", "\t", "\n", " \t\n " })
    @DisplayName("A blank topic is rejected")
    void constructor_rejectsBlankValue(String blank) {
        assertThrows(IllegalArgumentException.class, () -> new Topic(blank));
    }

    @ParameterizedTest(name = "value = \"{0}\"")
    @ValueSource(strings = { "Java\tGenerics", "Java\nGenerics", "Java\rGenerics" })
    @DisplayName("A topic containing an ISO control character is rejected")
    void constructor_rejectsIsoControlCharacters(String withControlCharacter) {
        assertThrows(IllegalArgumentException.class, () -> new Topic(withControlCharacter));
    }

    @Test
    @DisplayName("A topic of exactly the maximum length is accepted")
    void constructor_acceptsValueAtMaxLength() {
        String value = "a".repeat(MAX_LENGTH);

        assertEquals(value, new Topic(value).value());
    }

    @Test
    @DisplayName("A topic longer than the maximum length is rejected")
    void constructor_rejectsValueExceedingMaxLength() {
        String tooLong = "a".repeat(MAX_LENGTH + 1);

        assertThrows(IllegalArgumentException.class, () -> new Topic(tooLong));
    }

    @Test
    @DisplayName("Length is measured after trimming")
    void constructor_measuresLengthAfterTrimming() {
        String padded = "  " + "a".repeat(MAX_LENGTH) + "  ";

        assertEquals(MAX_LENGTH, new Topic(padded).value().length());
    }

    // ====================
    // normalisation
    // ====================

    @Test
    @DisplayName("Surrounding whitespace is trimmed from the topic")
    void constructor_trimsSurroundingWhitespace() {
        assertEquals("Java Generics", new Topic("  Java Generics  ").value());
    }

    @Test
    @DisplayName("Whitespace inside the topic is preserved")
    void constructor_preservesInnerWhitespace() {
        assertEquals("Java  Generics", new Topic("  Java  Generics  ").value());
    }

    // ====================
    // value semantics
    // ====================

    @Test
    @DisplayName("Topics with the same trimmed value are equal")
    void equals_ignoresSurroundingWhitespace() {
        Topic topic = new Topic("Java Generics");
        Topic padded = new Topic("  Java Generics  ");

        assertAll(
                () -> assertEquals(topic, padded),
                () -> assertEquals(topic.hashCode(), padded.hashCode()));
    }

    @Test
    @DisplayName("Topics with different values are not equal")
    void equals_distinguishesDifferentValues() {
        assertNotEquals(new Topic("Java Generics"), new Topic("Java Streams"));
    }

    @Test
    @DisplayName("Topics are case sensitive")
    void equals_isCaseSensitive() {
        assertNotEquals(new Topic("Java Generics"), new Topic("java generics"));
    }

    // ====================
    // toString override
    // ====================

    @Test
    @DisplayName("toString returns the bare topic value")
    void toString_returnsValue() {
        assertEquals("Java Generics", new Topic("  Java Generics  ").toString());
    }
}
