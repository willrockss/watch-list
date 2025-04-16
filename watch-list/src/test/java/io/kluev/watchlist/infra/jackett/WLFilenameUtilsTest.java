package io.kluev.watchlist.infra.jackett;

import lombok.val;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class WLFilenameUtilsTest {

    public static Stream<Arguments> escapeFilenameArgs() {
        return Stream.of(
                Arguments.of("normalFilename.txt", "normalFilename.txt"),
                Arguments.of("filename_with_underscore.txt", "filename_with_underscore.txt"),
                Arguments.of("filename_with_underscore_and_numbers_123.txt", "filename_with_underscore_and_numbers_123.txt"),
                Arguments.of("filename with spaces.txt", "filename_with_spaces.txt"),
                Arguments.of("filename[].txt", "filename[].txt"),
                Arguments.of("filename().txt", "filename().txt"),
                Arguments.of("filename[()].txt", "filename[()].txt"),
                Arguments.of("на русском.txt", "на_русском.txt"),
                Arguments.of("with/slash.txt", "with_slash.txt"),
                Arguments.of("with\\backslash.txt", "with_backslash.txt"),
                Arguments.of("with:colon.txt", "with:colon.txt"),
                Arguments.of("with,comma.txt", "with_comma.txt"),
                Arguments.of("with multi   spaces.txt", "with_multi_spaces.txt"),
                Arguments.of(
                        "Терминатор 2: Судный день / Terminator 2: Judgment Day (Джеймс Кэмерон / James Cameron) [1991, фантастика, боевик, триллер, AC3, NTSC] [EXTENDED / REMASTERED] VO (Данис Нурмухаметов / версия 2016).txt",
                        "Терминатор_2:_Судный_день_Terminator_2:_Judgment_Day_(Джеймс_Кэмерон_James_Cameron)_[1991_фантас.txt"
                ),
                Arguments.of(
                        "Терминатор 2: Судный день / Terminator 2: Judgment Day (Джеймс Кэмерон / James Cameron) [1991, фантастика, боевик, триллер, AC3, NTSC] [EXTENDED / REMASTERED] VO (Данис Нурмухаметов / версия 2016)",
                        "Терминатор_2:_Судный_день_Terminator_2:_Judgment_Day_(Джеймс_Кэмерон_James_Cameron)_[1991_фантастика"
                )
        );
    }

    @MethodSource("escapeFilenameArgs")
    @ParameterizedTest
    void escapeFilename(String initialFilename, String expectedFilename) {
        val actual = WLFilenameUtils.escapeFilename(initialFilename);
        assertEquals(expectedFilename, actual);
    }
}