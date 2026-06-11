package com.moodmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.moodmatch.dto.tag.CreateTagRequest;
import com.moodmatch.dto.tag.TagResponse;
import com.moodmatch.entity.TagCategory;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class TagServiceTest {

    @Inject
    TagService tagService;

    @Test
    @TestTransaction
    void shouldCreateTagOnlyOnceAndListTagsInDeterministicOrder() {
        int initialTagCount = tagService.listTags().size();
        TagResponse first = tagService.createTagIfNeeded(new CreateTagRequest("  Mystery  ", TagCategory.GENRE));
        TagResponse duplicate = tagService.createTagIfNeeded(new CreateTagRequest("mystery", TagCategory.GENRE));
        TagResponse tone = tagService.createTagIfNeeded(new CreateTagRequest("Tone-" + System.nanoTime(), TagCategory.TONE));

        assertNotNull(first.id());
        assertEquals(first.id(), duplicate.id());

        List<TagResponse> tags = tagService.listTags();
        assertEquals(initialTagCount + 2, tags.size());
        assertEquals("Mystery", tags.stream()
                .filter(tag -> tag.id().equals(first.id()))
                .findFirst()
                .orElseThrow()
                .name());
        assertEquals(tone.id(), tags.stream()
                .filter(tag -> tag.id().equals(tone.id()))
                .findFirst()
                .orElseThrow()
                .id());
    }
}
