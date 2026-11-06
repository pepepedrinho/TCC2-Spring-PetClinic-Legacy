SELECT id, first_name, last_name, address, city, telephone FROM owners WHERE 1 = 0;
SELECT 1 / CASE WHEN COUNT(*) = 1 THEN 1 ELSE 0 END FROM seed_history WHERE id = 1;
