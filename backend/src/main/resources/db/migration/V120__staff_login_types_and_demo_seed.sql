-- T03 staff management: reference login types (spec sheet "LogIn Type") and a small demo roster.
INSERT INTO `login_type` (`login_name`) VALUES ('jr.Employee'), ('sr.Employee'), ('Manager');

INSERT INTO `staff` (`login_id`, `staff_type`, `first_name`, `middle_name`, `last_name`, `date_of_birth`,
                     `join_date`, `experience`, `salary`, `home_town`, `home_town_address`, `job_address`,
                     `mobile_number`, `email`, `last_company`)
VALUES ((SELECT `login_id` FROM `login_type` WHERE `login_name` = 'Manager'), 'INTERNAL',
        'Asha', NULL, 'Mehta', '1984-03-12', '2015-06-01', '12 years retail pharmacy', 95000.00, 'Ahmedabad',
        '12 Shanti Block, Near Lal Darwaja, CG Road, Navrangpura, Ahmedabad, Ahmedabad, 380009',
        'Head Office, Ahmedabad', '+919825000001', 'asha.mehta@demo.pharmaerp.local', 'MedPlus'),
       ((SELECT `login_id` FROM `login_type` WHERE `login_name` = 'sr.Employee'), 'INTERNAL',
        'Rohan', 'K', 'Shah', '1990-11-02', '2018-01-15', '7 years inventory control', 62000.00, 'Surat',
        '4 Ganga Society, Opp. City Light, Ring Road, Surat, Surat, 395007',
        'Central Warehouse, Surat', '+919825000002', 'rohan.shah@demo.pharmaerp.local', 'Apollo Pharmacy'),
       ((SELECT `login_id` FROM `login_type` WHERE `login_name` = 'jr.Employee'), 'DISTRIBUTOR',
        'Priya', NULL, 'Patel', '1998-07-21', '2023-04-03', '2 years distribution', 32000.00, 'Vadodara',
        '9 Akota Garden, Near Akota Stadium, Productivity Road, Vadodara, Vadodara, 390020',
        NULL, '+919825000003', 'priya.patel@demo.pharmaerp.local', NULL);

INSERT INTO `salary` (`employee_id`, `salary_amount`, `pay_period`, `paid_on`)
SELECT s.`employee_id`, s.`salary`, p.`pay_period`, p.`paid_on`
FROM `staff` s
CROSS JOIN (SELECT '2026-08' AS `pay_period`, DATE '2026-08-31' AS `paid_on`
            UNION ALL SELECT '2026-09', DATE '2026-09-30') p
WHERE s.`email` LIKE '%@demo.pharmaerp.local';
