UPDATE expenses SET name = 'Permiso circulación', category = 'Otros' WHERE name LIKE 'Permiso circulaci%';
UPDATE expenses SET category = 'Créditos' WHERE category LIKE 'Cr%ditos';
UPDATE expenses SET category = 'Educación' WHERE category LIKE 'Educaci%n';
SELECT name, amount, due_date, category, status FROM expenses ORDER BY due_date, name;
