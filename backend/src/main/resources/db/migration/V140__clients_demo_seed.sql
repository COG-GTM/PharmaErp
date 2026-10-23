-- T05 Clients + feedback: demo customers and their feedback (no fixed IDs; staff links left NULL).
-- Feedback is linked via LAST_INSERT_ID() so it only ever attaches to the rows inserted here,
-- never to a pre-existing client that happens to share a name.
INSERT INTO `client` (`client_name`, `client_address`, `client_number`, `client_email`, `city`, `district`, `state`, `gstin`, `feedback_email`)
VALUES ('Shree Ganesh Medical Stores', '12 Station Road, Navrangpura', '+91 98250 11223', 'orders@shreeganeshmed.example', 'Ahmedabad', 'Ahmedabad', 'Gujarat', '24AAACS1234F1Z5', 'feedback@shreeganeshmed.example');
SET @t05_client = LAST_INSERT_ID();
INSERT INTO `feedback` (`client_id`, `employee_id`, `feedback_description`, `rating`)
VALUES (@t05_client, NULL, 'Orders always arrive on time with batch and expiry details.', 5),
       (@t05_client, NULL, 'Invoice GST breakdown was clear; one carton was dented.', 4);

INSERT INTO `client` (`client_name`, `client_address`, `client_number`, `client_email`, `city`, `district`, `state`, `gstin`, `feedback_email`)
VALUES ('Apollo Care Clinic', '45 MG Road, Camp', '+91 98220 44556', 'purchase@apollocare.example', 'Pune', 'Pune', 'Maharashtra', '27AABCA5678K1Z2', 'care@apollocare.example');
SET @t05_client = LAST_INSERT_ID();
INSERT INTO `feedback` (`client_id`, `employee_id`, `feedback_description`, `rating`)
VALUES (@t05_client, NULL, 'Cold-chain items reached at the correct temperature.', 5);

INSERT INTO `client` (`client_name`, `client_address`, `client_number`, `client_email`, `city`, `district`, `state`, `gstin`, `feedback_email`)
VALUES ('Lifeline Hospital Pharmacy', '8 Ring Road, Adajan', '0261-2765432', 'pharmacy@lifeline.example', 'Surat', 'Surat', 'Gujarat', '24AAECL9012M1Z8', NULL);
SET @t05_client = LAST_INSERT_ID();
INSERT INTO `feedback` (`client_id`, `employee_id`, `feedback_description`, `rating`)
VALUES (@t05_client, NULL, 'Delivery was two days late for the antibiotics order.', 2);

INSERT INTO `client` (`client_name`, `client_address`, `client_number`, `client_email`, `city`, `district`, `state`, `gstin`, `feedback_email`)
VALUES ('Sunrise Chemists', '221 Anna Salai', '+91 94440 77889', 'sunrise.chemists@example.com', 'Chennai', 'Chennai', 'Tamil Nadu', NULL, 'sunrise.chemists@example.com');
SET @t05_client = LAST_INSERT_ID();
INSERT INTO `feedback` (`client_id`, `employee_id`, `feedback_description`, `rating`)
VALUES (@t05_client, NULL, 'Good prices, but the payment receipt took a while to arrive.', 3);

INSERT INTO `client` (`client_name`, `client_address`, `client_number`, `client_email`, `city`, `district`, `state`, `gstin`, `feedback_email`)
VALUES ('Ravi Patel', 'B-302 Shanti Residency, Vastrapur', '+91 99090 12345', 'ravi.patel@example.com', 'Ahmedabad', 'Ahmedabad', 'Gujarat', NULL, NULL);
SET @t05_client = LAST_INSERT_ID();
INSERT INTO `feedback` (`client_id`, `employee_id`, `feedback_description`, `rating`)
VALUES (@t05_client, NULL, 'Staff explained the dosage clearly. Very helpful.', 5);

INSERT INTO `client` (`client_name`, `client_address`, `client_number`, `client_email`, `city`, `district`, `state`, `gstin`, `feedback_email`)
VALUES ('Wellness Plus Diagnostics', '17 Park Street', '033 2229 8765', 'accounts@wellnessplus.example', 'Kolkata', 'Kolkata', 'West Bengal', '19AAFCW3456P1ZQ', 'feedback@wellnessplus.example');
