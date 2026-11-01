-- Sample catalog. The full cast is imported in milestone M6; keep this seed
-- idempotent by slug so repeated runs never duplicate rows.
INSERT INTO characters (slug, name, bio, status)
VALUES
    ('monica', 'Mônica',
     'Baixinha, dentuça e de temperamento forte, Mônica é a líder informal da turma. Carrega o coelho Sansão e resolve quase tudo com a própria força.',
     'ACTIVE'),
    ('cebolinha', 'Cebolinha',
     'O menino de cinco fios de cabelo que vive inventando planos infalíveis para derrotar a Mônica. Tem o costume de trocar o R pelo L ao falar.',
     'ACTIVE'),
    ('cascao', 'Cascão',
     'O amigo que foge de banho e de chuva, mas é leal e sempre entra nas aventuras da turma.',
     'ACTIVE'),
    ('magali', 'Magali',
     'Comilona inveterada, adora todos os tipos de comida e tem um apetite que assusta os amigos.',
     'ACTIVE'),
    ('chico-bento', 'Chico Bento',
     'Caipira do interior, vive na roça com a família e a namorada Rosinha, sempre com um jeito simples de encarar a vida.',
     'ACTIVE')
ON CONFLICT (slug) DO UPDATE
SET name = EXCLUDED.name,
    bio = EXCLUDED.bio,
    updated_at = now();

INSERT INTO character_aliases (character_id, alias)
SELECT c.id, alias
FROM characters c
JOIN (
    VALUES
        ('monica', 'Monica'),
        ('monica', 'Baixinha'),
        ('cebolinha', 'Cebola'),
        ('cebolinha', 'Cebolinha'),
        ('cascao', 'Cascao'),
        ('magali', 'Magali'),
        ('chico-bento', 'Chico Bento'),
        ('chico-bento', 'Chico')
) AS seed (slug, alias) ON seed.slug = c.slug
ON CONFLICT DO NOTHING;
