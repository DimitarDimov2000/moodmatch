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
        TagResponse first = tagService.createTagIfNeeded(new CreateTagRequest("  Mystery  ", TagCategory.GENRE));
        TagResponse duplicate = tagService.createTagIfNeeded(new CreateTagRequest("mystery", TagCategory.GENRE));
        TagResponse tone = tagService.createTagIfNeeded(new CreateTagRequest("spannend", TagCategory.TONE));

        assertNotNull(first.id());
        assertEquals(first.id(), duplicate.id());

        List<TagResponse> tags = tagService.listTags();
        assertEquals(2, tags.size());
        assertEquals(first.id(), tags.getFirst().id());
        assertEquals(tone.id(), tags.get(1).id());
        assertEquals("Mystery", tags.getFirst().name());
    }
}
