SELECT id, first_name, last_name FROM vets WHERE 1 = 0;
SELECT id, name FROM specialties WHERE 1 = 0;
SELECT vet_id, specialty_id FROM vet_specialties WHERE 1 = 0;
SELECT id, name FROM types WHERE 1 = 0;
SELECT id, name, birth_date, type_id, owner_id FROM pets WHERE 1 = 0;
SELECT id, pet_id, visit_date, description FROM visits WHERE 1 = 0;
SELECT 1 / CASE WHEN COUNT(*) = 0 THEN 1 ELSE 0 END
FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'OWNERS';
