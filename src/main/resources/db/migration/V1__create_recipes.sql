CREATE TABLE recipes (
    id           BIGSERIAL    PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    description  VARCHAR(2000),
    servings     INTEGER      NOT NULL CHECK (servings > 0),
    vegetarian   BOOLEAN      NOT NULL,
    instructions TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL
);

CREATE TABLE recipe_ingredients (
    id            BIGSERIAL PRIMARY KEY,
    recipe_id     BIGINT       NOT NULL
                  REFERENCES recipes (id) ON DELETE CASCADE,
    name          VARCHAR(120) NOT NULL,

    -- Nullable for the unquantified ingredient -- "salt, to taste" has no number at all.
    quantity      NUMERIC(10, 3) CHECK (quantity IS NULL OR quantity > 0),
    unit          VARCHAR(50),
    display_order INTEGER      NOT NULL,

    -- This will be used in include/exclude ingredients filters to avoid sequential scan.
    CONSTRAINT uq_recipe_ingredient_name UNIQUE (recipe_id, name)
);

CREATE INDEX idx_recipe_ingredients_name ON recipe_ingredients (name);
