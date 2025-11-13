DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'admin') THEN
        CREATE ROLE admin WITH LOGIN PASSWORD 'admin';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'database') THEN
        CREATE DATABASE database OWNER admin;
    END IF;
END $$;

GRANT ALL PRIVILEGES ON DATABASE database TO admin;

\connect database

-- Таблица routes
CREATE TABLE IF NOT EXISTS routes (
    id SERIAL PRIMARY KEY CHECK (id > 0),
    name VARCHAR NOT NULL CHECK (name != '' AND LENGTH(TRIM(name)) > 0),
    x FLOAT NOT NULL,
    y DOUBLE PRECISION NOT NULL CHECK (y <= 807),
    from_x DOUBLE PRECISION NOT NULL,
    from_y DOUBLE PRECISION NOT NULL,
    from_name VARCHAR,
    to_x DOUBLE PRECISION NOT NULL,
    to_y DOUBLE PRECISION NOT NULL,
    to_name VARCHAR,
    distance BIGINT NOT NULL CHECK(distance > 1),
    rating BIGINT NOT NULL CHECK(rating > 0),
    creation_date TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Функции

-- 1. Вернуть один объект с максимальным значением name
CREATE OR REPLACE FUNCTION get_route_with_max_name()
RETURNS TABLE(
    id INTEGER,
    name VARCHAR,
    x FLOAT,
    y DOUBLE PRECISION,
    from_x DOUBLE PRECISION,
    from_y DOUBLE PRECISION,
    from_name VARCHAR,
    to_x DOUBLE PRECISION,
    to_y DOUBLE PRECISION,
    to_name VARCHAR,
    distance BIGINT,
    rating BIGINT,
    creation_date TIMESTAMPTZ
) AS $$
BEGIN
    RETURN QUERY
    SELECT r.id, r.name, r.x, r.y, r.from_x, r.from_y, r.from_name,
           r.to_x, r.to_y, r.to_name, r.distance, r.rating, r.creation_date::TIMESTAMPTZ
    FROM routes r
    WHERE r.name = (SELECT MAX(routes.name) FROM routes)
    ORDER BY r.id -- для детерминированности
    LIMIT 1;
END;
$$ LANGUAGE plpgsql;

-- 2. Вернуть количество объектов с rating меньше заданного
CREATE OR REPLACE FUNCTION count_routes_with_rating_less_than(rating_threshold BIGINT)
RETURNS BIGINT AS $$
BEGIN
    RETURN (SELECT COUNT(*) FROM routes WHERE rating < rating_threshold);
END;
$$ LANGUAGE plpgsql;

-- 3. Вернуть массив объектов с rating больше заданного
CREATE OR REPLACE FUNCTION get_routes_with_rating_greater_than(rating_threshold BIGINT)
RETURNS TABLE(
    id INTEGER,
    name VARCHAR,
    x FLOAT,
    y DOUBLE PRECISION,
    from_x DOUBLE PRECISION,
    from_y DOUBLE PRECISION,
    from_name VARCHAR,
    to_x DOUBLE PRECISION,
    to_y DOUBLE PRECISION,
    to_name VARCHAR,
    distance BIGINT,
    rating BIGINT,
    creation_date TIMESTAMPTZ
) AS $$
BEGIN
    RETURN QUERY
    SELECT r.id, r.name, r.x, r.y, r.from_x, r.from_y, r.from_name,
           r.to_x, r.to_y, r.to_name, r.distance, r.rating, r.creation_date::TIMESTAMPTZ
    FROM routes r
    WHERE r.rating > rating_threshold
    ORDER BY r.rating DESC;
END;
$$ LANGUAGE plpgsql;

-- 4. Найти маршруты между указанными локациями с сортировкой
CREATE OR REPLACE FUNCTION find_routes_between_locations(
    from_location_name VARCHAR DEFAULT NULL,
    to_location_name VARCHAR DEFAULT NULL,
    sort_by VARCHAR DEFAULT 'name' -- name, distance, rating, creation_date
)
RETURNS TABLE(
    id INTEGER,
    name VARCHAR,
    x FLOAT,
    y DOUBLE PRECISION,
    from_x DOUBLE PRECISION,
    from_y DOUBLE PRECISION,
    from_name VARCHAR,
    to_x DOUBLE PRECISION,
    to_y DOUBLE PRECISION,
    to_name VARCHAR,
    distance BIGINT,
    rating BIGINT,
    creation_date TIMESTAMPTZ
) AS $$
BEGIN
    IF sort_by = 'distance' THEN
        RETURN QUERY
        SELECT r.id, r.name, r.x, r.y, r.from_x, r.from_y, r.from_name,
               r.to_x, r.to_y, r.to_name, r.distance, r.rating, r.creation_date::TIMESTAMPTZ
        FROM routes r
        WHERE (from_location_name IS NULL OR r.from_name = from_location_name)
          AND (to_location_name IS NULL OR r.to_name = to_location_name)
        ORDER BY r.distance;
    ELSIF sort_by = 'rating' THEN
        RETURN QUERY
        SELECT r.id, r.name, r.x, r.y, r.from_x, r.from_y, r.from_name,
               r.to_x, r.to_y, r.to_name, r.distance, r.rating, r.creation_date::TIMESTAMPTZ
        FROM routes r
        WHERE (from_location_name IS NULL OR r.from_name = from_location_name)
          AND (to_location_name IS NULL OR r.to_name = to_location_name)
        ORDER BY r.rating DESC;
    ELSIF sort_by = 'creation_date' THEN
        RETURN QUERY
        SELECT r.id, r.name, r.x, r.y, r.from_x, r.from_y, r.from_name,
               r.to_x, r.to_y, r.to_name, r.distance, r.rating, r.creation_date::TIMESTAMPTZ
        FROM routes r
        WHERE (from_location_name IS NULL OR r.from_name = from_location_name)
          AND (to_location_name IS NULL OR r.to_name = to_location_name)
        ORDER BY r.creation_date DESC;
    ELSE -- default: sort by name
        RETURN QUERY
        SELECT r.id, r.name, r.x, r.y, r.from_x, r.from_y, r.from_name,
               r.to_x, r.to_y, r.to_name, r.distance, r.rating, r.creation_date::TIMESTAMPTZ
        FROM routes r
        WHERE (from_location_name IS NULL OR r.from_name = from_location_name)
          AND (to_location_name IS NULL OR r.to_name = to_location_name)
        ORDER BY r.name;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- 5. Добавить новый маршрут между указанными локациями
CREATE OR REPLACE FUNCTION add_route_between_locations(
    route_name VARCHAR,
    coord_x FLOAT,
    coord_y DOUBLE PRECISION,
    from_loc_x DOUBLE PRECISION,
    from_loc_y DOUBLE PRECISION,
    from_loc_name VARCHAR,
    to_loc_x DOUBLE PRECISION,
    to_loc_y DOUBLE PRECISION,
    to_loc_name VARCHAR,
    route_distance BIGINT,
    route_rating BIGINT
)
RETURNS INTEGER AS $$
DECLARE
    new_id INTEGER;
BEGIN
    INSERT INTO routes (name, x, y, from_x, from_y, from_name, to_x, to_y, to_name, distance, rating)
    VALUES (route_name, coord_x, coord_y, from_loc_x, from_loc_y, from_loc_name,
            to_loc_x, to_loc_y, to_loc_name, route_distance, route_rating)
    RETURNING id INTO new_id;
    
    RETURN new_id;
END;
$$ LANGUAGE plpgsql;
