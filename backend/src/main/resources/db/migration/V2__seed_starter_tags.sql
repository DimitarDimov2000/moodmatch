INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('11111111-1111-1111-1111-111111111111' AS UUID), 'Sci-Fi', 'GENRE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Sci-Fi') AND category = 'GENRE'
);

INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('11111111-1111-1111-1111-111111111112' AS UUID), 'Drama', 'GENRE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Drama') AND category = 'GENRE'
);

INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('11111111-1111-1111-1111-111111111113' AS UUID), 'Fantasy', 'GENRE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Fantasy') AND category = 'GENRE'
);

INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('11111111-1111-1111-1111-111111111114' AS UUID), 'Mystery', 'GENRE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Mystery') AND category = 'GENRE'
);

INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('11111111-1111-1111-1111-111111111115' AS UUID), 'Romance', 'GENRE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Romance') AND category = 'GENRE'
);

INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('22222222-2222-2222-2222-222222222221' AS UUID), 'Space', 'THEME', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Space') AND category = 'THEME'
);

INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('22222222-2222-2222-2222-222222222222' AS UUID), 'Survival', 'THEME', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Survival') AND category = 'THEME'
);

INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('22222222-2222-2222-2222-222222222223' AS UUID), 'Family', 'THEME', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Family') AND category = 'THEME'
);

INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('22222222-2222-2222-2222-222222222224' AS UUID), 'Identity', 'THEME', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Identity') AND category = 'THEME'
);

INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('22222222-2222-2222-2222-222222222225' AS UUID), 'Politics', 'THEME', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Politics') AND category = 'THEME'
);

INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('33333333-3333-3333-3333-333333333331' AS UUID), 'Thoughtful', 'TONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Thoughtful') AND category = 'TONE'
);

INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('33333333-3333-3333-3333-333333333332' AS UUID), 'Emotional', 'TONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Emotional') AND category = 'TONE'
);

INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('33333333-3333-3333-3333-333333333333' AS UUID), 'Dark', 'TONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Dark') AND category = 'TONE'
);

INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('33333333-3333-3333-3333-333333333334' AS UUID), 'Light', 'TONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Light') AND category = 'TONE'
);

INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('33333333-3333-3333-3333-333333333335' AS UUID), 'Epic', 'TONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Epic') AND category = 'TONE'
);

INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('44444444-4444-4444-4444-444444444441' AS UUID), 'Future', 'SETTING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Future') AND category = 'SETTING'
);

INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('44444444-4444-4444-4444-444444444442' AS UUID), 'Historical', 'SETTING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Historical') AND category = 'SETTING'
);

INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('44444444-4444-4444-4444-444444444443' AS UUID), 'Urban', 'SETTING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Urban') AND category = 'SETTING'
);

INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('44444444-4444-4444-4444-444444444444' AS UUID), 'Nature', 'SETTING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Nature') AND category = 'SETTING'
);

INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('55555555-5555-5555-5555-555555555551' AS UUID), 'Relaxing', 'EXPERIENCE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Relaxing') AND category = 'EXPERIENCE'
);

INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('55555555-5555-5555-5555-555555555552' AS UUID), 'Intense', 'EXPERIENCE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Intense') AND category = 'EXPERIENCE'
);

INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('55555555-5555-5555-5555-555555555553' AS UUID), 'Challenging', 'EXPERIENCE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Challenging') AND category = 'EXPERIENCE'
);

INSERT INTO tags (id, name, category, created_at, updated_at)
SELECT CAST('55555555-5555-5555-5555-555555555554' AS UUID), 'Comfort', 'EXPERIENCE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tags WHERE lower(name) = lower('Comfort') AND category = 'EXPERIENCE'
);
