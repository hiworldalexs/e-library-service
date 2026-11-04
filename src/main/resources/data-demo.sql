-- Sample books for the `demo` Spring profile (stable UUID IDs matching the OpenAPI examples).
INSERT INTO books (id, title, author, category, total_copies, created_at, updated_at) VALUES
  ('11111111-1111-1111-1111-111111111101', 'Clean Code', 'Robert C. Martin', 'Tech', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('11111111-1111-1111-1111-111111111102', 'Effective Java', 'Joshua Bloch', 'Tech', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('11111111-1111-1111-1111-111111111103', 'The Pragmatic Programmer', 'Andrew Hunt', 'Tech', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('11111111-1111-1111-1111-111111111104', 'Domain-Driven Design', 'Eric Evans', 'Architecture', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('11111111-1111-1111-1111-111111111105', 'Pride and Prejudice', 'Jane Austen', 'Fiction', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('11111111-1111-1111-1111-111111111106', '1984', 'George Orwell', 'Fiction', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
