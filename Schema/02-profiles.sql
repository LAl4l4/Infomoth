CREATE TABLE IF NOT EXISTS profiles (
    user_id INT NOT NULL PRIMARY KEY,
    bio VARCHAR(255),
    avatar_url VARCHAR(255),
    birthday DATE,
    gender VARCHAR(30)
);
