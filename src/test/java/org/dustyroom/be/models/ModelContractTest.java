package org.dustyroom.be.models;

import org.dustyroom.be.models.book.PageKey;
import org.dustyroom.be.models.book.PageRef;
import org.dustyroom.be.models.book.PageShape;
import org.dustyroom.be.models.library.LibraryChapter;
import org.dustyroom.be.models.library.LibraryImport;
import org.dustyroom.be.models.library.LibrarySource;
import org.dustyroom.be.models.library.LibraryWork;
import org.dustyroom.be.models.state.FitMode;
import org.dustyroom.be.models.state.ReaderSettings;
import org.dustyroom.be.models.state.ViewerTheme;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModelContractTest {
    @Test
    void pageKeysKeepValueEqualityAcrossConstructorsAndBuilders() {
        PageKey key = new PageKey("book", "page");
        PageKey equal = PageKey.builder().sourceId("book").pageId("page").build();
        Map<PageKey, String> cache = new HashMap<>();
        cache.put(key, "cached page");
        assertEquals(key, equal);
        assertEquals("cached page", cache.get(equal));
        assertNotEquals(new PageKey("other", "page"), key);
        assertThrows(NullPointerException.class, () -> PageKey.builder().pageId("page").build());
    }

    @Test
    void pageValidationCannotBeBypassedWithBuilderOrSetters() {
        PageShape shape = PageShape.builder().width(100).height(200).build();
        assertSame(shape, shape.setWidth(300));
        assertTrue(shape.isLandscape());
        assertThrows(IllegalArgumentException.class, () -> shape.setHeight(0));
        assertEquals(200, shape.getHeight());
        assertThrows(IllegalArgumentException.class, () -> PageShape.builder().width(10).build());
        assertThrows(NullPointerException.class, () -> PageRef.builder().key(new PageKey("b", "p")).build());
    }

    @Test
    void settingsRetainValidationAndNormalizationAfterChainedUpdates() {
        ReaderSettings settings = ReaderSettings.defaults();
        assertSame(settings, settings.setTheme(ViewerTheme.LIGHT).setFitMode(FitMode.FIT_WIDTH).setZoom(2));
        assertThrows(IllegalArgumentException.class, () -> settings.setZoom(Double.NaN));
        assertEquals(2, settings.getZoom());
        assertThrows(IllegalArgumentException.class, () -> settings.setTheme(null));
        assertEquals(ViewerTheme.LIGHT, settings.getTheme());
        settings.setLastDirectory(null);
        assertEquals("", settings.getLastDirectory());
        assertThrows(IllegalArgumentException.class, () -> ReaderSettings.builder()
                .theme(ViewerTheme.DARK).fitMode(FitMode.FIT_SCREEN).zoom(0).build());
    }

    @Test
    void collectionBuildersAndSettersDoNotRetainCallerOwnedLists() {
        LibrarySource source = LibrarySource.builder().path("book.zip").title("Book").directory(false).build();
        List<LibrarySource> caller = new ArrayList<>(List.of(source));
        LibraryImport selection = LibraryImport.builder().root("root").title("Work")
                .sources(caller).build();
        caller.clear();
        assertEquals(1, selection.getSources().size());
        caller.add(source);
        assertSame(selection, selection.setSources(caller));
        caller.clear();
        assertEquals(1, selection.getSources().size());
        assertThrows(UnsupportedOperationException.class, selection.getSources()::clear);
    }

    @Test
    void workPreservesResumeAndCompletionSemanticsWithMutableDtos() {
        LibraryChapter first = LibraryChapter.builder().id("first").lastRead(10).finished(true).build();
        LibraryChapter second = LibraryChapter.builder().id("second").lastRead(20).build();
        LibraryWork work = LibraryWork.builder().id("work").title("Work").root("root")
                .chapters(List.of(first, second)).build();
        assertEquals(second, work.getResumeChapter());
        assertEquals(1, work.getCompleted());
        assertThrows(IllegalArgumentException.class, () -> work.setChapters(List.of()));
        assertEquals(2, work.getChapters().size());
    }
}
